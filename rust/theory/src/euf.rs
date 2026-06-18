//! EUF — equality with uninterpreted functions, via proof-producing incremental
//! congruence closure.
//!
//! Public API contract:
//! - `mk_const`/`mk_app` build terms *and* register them in the e-graph;
//! - `register_eq_atom(var, a, b)` ties a SAT variable to the equality atom `a = b`;
//! - the [`Theory`] impl drives assert/check/propagate/explain/push/pop.
//!
//! # Algorithm
//!
//! The core is a union-find over [`TermId`]s with use-lists and a signature table, the
//! standard incremental congruence-closure scheme (Downey–Sethi–Tarjan / Nelson–Oppen as
//! presented by Nieuwenhuis & Oliveras, *Proof-Producing Congruence Closure*, RTA 2005).
//!
//! Explanations use a separate **proof forest**: every `merge(x, y, reason)` adds an
//! undirected proof edge `x — y` (we re-root the smaller side via path reversal so the edge
//! can be appended). `explain(a, b)` walks the unique proof path between `a` and `b`; an
//! `Input` edge contributes its literal directly, a `Congruence` edge recurses on the
//! corresponding argument pairs.
//!
//! Backtracking is handled by a single mutation trail with level markers; `pop` replays the
//! trail in reverse, exactly undoing each recorded write.

use rustc_hash::FxHashMap;
use satcheck_core::{Lbool, Lit, Symbol, Theory, Var};
use satcheck_term::{TermArena, TermId};

/// Why two terms were merged — the justification recorded on a proof-forest edge.
#[derive(Clone, Copy)]
enum Reason {
    /// Asserted by the SAT solver: this literal is the (positive) equality atom.
    Input(Lit),
    /// Derived by congruence: `app1` and `app2` have the same function symbol and pairwise
    /// equal arguments, so they are equal. Explanation recurses on the argument pairs.
    Congruence(TermId, TermId),
}

/// A signature key: a function symbol together with the representatives of its arguments.
/// Two applications are congruent iff they share a signature.
type Sig = (Symbol, Vec<TermId>);

/// One reversible mutation recorded on the trail.
enum TrailOp {
    /// `repr[i]` was changed; restore the saved old value.
    Repr(usize, TermId),
    /// `size[i]` was changed; restore the saved old value.
    Size(usize, u32),
    /// A value was pushed onto `uses[i]`; pop it.
    UsePush(usize),
    /// `uses[i]` was emptied (its contents migrated to another class); restore the saved vec.
    UseTake(usize, Vec<TermId>),
    /// A signature was inserted into the table; remove it on undo.
    SigInsert(Sig),
    /// A signature mapping was overwritten; restore `(key -> old_value)` on undo.
    SigOverwrite(Sig, TermId),
    /// A signature mapping was removed; restore `(key -> old_value)` on undo.
    SigRemove(Sig, TermId),
    /// `proof_parent[i]`/`proof_reason[i]` were set; restore the saved old values.
    Proof(usize, Option<TermId>, Option<Reason>),
    /// A disequality was pushed; pop it.
    DiseqPush,
    /// `assigned[vi]` was changed; restore the saved old value.
    Assigned(usize, Lbool),
}

pub struct Euf {
    arena: TermArena,

    /// Union-find parent (representative). `repr[i] == i` for a root.
    repr: Vec<TermId>,
    /// Size of the class rooted at each representative (used for union-by-size).
    size: Vec<u32>,
    /// Use-list: for representative `r`, the application terms one of whose arguments lies in
    /// `r`'s class (kept on the representative; migrated on union).
    uses: Vec<Vec<TermId>>,

    /// Proof-forest parent edge and its reason. The forest is rooted arbitrarily; an edge
    /// `i — proof_parent[i]` is justified by `proof_reason[i]`.
    proof_parent: Vec<Option<TermId>>,
    proof_reason: Vec<Option<Reason>>,

    /// Signature table: `(symbol, repr-of-args) -> canonical application term`.
    sig: FxHashMap<Sig, TermId>,

    /// Asserted disequalities `(a, b, lit)` where `lit` is the *negated* atom literal.
    diseq: Vec<(TermId, TermId, Lit)>,

    /// SAT variable -> equality atom `(a, b)`.
    atoms: Vec<Option<(TermId, TermId)>>,

    /// SAT variable -> current theory assignment of its atom (`Undef` if not yet asserted/implied).
    /// Used by `propagate` to skip atoms whose truth is already fixed.
    assigned: Vec<Lbool>,

    /// For an atom propagated FALSE (`a != b`), the disequality `(p, q, neg_lit)` that witnessed
    /// the separation at propagation time, with `a ≡ p` and `b ≡ q`. Pinned here because the
    /// `explain` call happens later, after further merges; the witness's `a≡p`/`b≡q` proof paths
    /// stay valid but *which* disequality applies must be fixed at propagation time.
    neg_witness: Vec<Option<(TermId, TermId, Lit)>>,

    /// Pending congruence-closure work: pairs of terms to be merged.
    pending: Vec<(TermId, TermId, Reason)>,

    /// Mutation trail and the stack of level start offsets (one per `push`).
    trail: Vec<TrailOp>,
    levels: Vec<usize>,

    /// How many arena terms have been registered into the e-graph so far.
    registered: usize,
}

impl Default for Euf {
    fn default() -> Self {
        Euf::new()
    }
}

impl Euf {
    pub fn new() -> Euf {
        Euf {
            arena: TermArena::new(),
            repr: Vec::new(),
            size: Vec::new(),
            uses: Vec::new(),
            proof_parent: Vec::new(),
            proof_reason: Vec::new(),
            sig: FxHashMap::default(),
            diseq: Vec::new(),
            atoms: Vec::new(),
            assigned: Vec::new(),
            neg_witness: Vec::new(),
            pending: Vec::new(),
            trail: Vec::new(),
            levels: Vec::new(),
            registered: 0,
        }
    }

    /// Build (and register) a 0-ary constant.
    pub fn mk_const(&mut self, name: &str) -> TermId {
        let id = self.arena.constant(name);
        self.register_terms(id);
        id
    }

    /// Build (and register) the application `name(args)`.
    pub fn mk_app(&mut self, name: &str, args: Vec<TermId>) -> TermId {
        let id = self.arena.func(name, args);
        self.register_terms(id);
        id
    }

    /// Tie SAT variable `var` to the equality atom `a = b`.
    pub fn register_eq_atom(&mut self, var: Var, a: TermId, b: TermId) {
        let vi = var.index();
        if vi >= self.atoms.len() {
            self.atoms.resize(vi + 1, None);
            self.assigned.resize(vi + 1, Lbool::Undef);
            self.neg_witness.resize(vi + 1, None);
        }
        self.atoms[vi] = Some((a, b));
    }

    pub fn num_terms(&self) -> usize {
        self.arena.num_terms()
    }

    /// Register the e-graph state for term `id` (the term just built in the arena). Each
    /// `mk_*` call creates at most one new node, whose index is `num_terms() - 1`; if `id`
    /// was already registered (hash-consing returned an existing term) this is a no-op.
    ///
    /// Because args are always built before their parent, a term's arguments are already
    /// registered by the time we see it. Registration happens *before* solving (no `push` has
    /// occurred yet) so it need not be trailed; we assert that.
    fn register_terms(&mut self, id: TermId) {
        if id.index() < self.registered {
            return; // already registered (existing hash-consed term)
        }
        debug_assert_eq!(id.index(), self.registered, "terms registered in order");
        debug_assert!(
            self.levels.is_empty(),
            "terms must be registered before solving (no push active)"
        );

        self.repr.push(id);
        self.size.push(1);
        self.uses.push(Vec::new());
        self.proof_parent.push(None);
        self.proof_reason.push(None);
        self.registered += 1;

        // Add this term to the use-lists of its arguments' classes and to the signature
        // table. At registration time everything is a singleton class (repr == self), so these
        // are non-conflicting; an argument that later merges carries the use along.
        let args: Vec<TermId> = self.arena.args_of(id).to_vec();
        if !args.is_empty() {
            for &arg in &args {
                let r = self.find(arg);
                self.uses[r.index()].push(id);
            }
            // Insert signature (no trail: pre-solving). Distinct constructed terms cannot
            // collide here because hash-consing already deduplicated identical terms and all
            // classes are singletons.
            let key = self.signature(id);
            self.sig.insert(key, id);
        }
    }

    // ----- union-find -----

    #[inline]
    fn find(&self, mut t: TermId) -> TermId {
        // No path compression: representatives change under backtracking, so a plain walk
        // (kept shallow by union-by-size) keeps undo trivial and correct.
        while self.repr[t.index()] != t {
            t = self.repr[t.index()];
        }
        t
    }

    #[inline]
    fn signature(&self, app: TermId) -> Sig {
        let sym = self.arena.symbol_of(app);
        let args = self
            .arena
            .args_of(app)
            .iter()
            .map(|&a| self.find(a))
            .collect();
        (sym, args)
    }

    // ----- trailed mutations -----

    #[inline]
    fn set_repr(&mut self, i: usize, v: TermId) {
        self.trail.push(TrailOp::Repr(i, self.repr[i]));
        self.repr[i] = v;
    }
    #[inline]
    fn set_size(&mut self, i: usize, v: u32) {
        self.trail.push(TrailOp::Size(i, self.size[i]));
        self.size[i] = v;
    }
    #[inline]
    fn push_use(&mut self, i: usize, app: TermId) {
        self.uses[i].push(app);
        self.trail.push(TrailOp::UsePush(i));
    }
    #[inline]
    fn set_proof(&mut self, i: usize, parent: Option<TermId>, reason: Option<Reason>) {
        self.trail.push(TrailOp::Proof(
            i,
            self.proof_parent[i],
            self.proof_reason[i],
        ));
        self.proof_parent[i] = parent;
        self.proof_reason[i] = reason;
    }
    fn sig_set(&mut self, key: Sig, app: TermId) {
        match self.sig.insert(key.clone(), app) {
            Some(old) => self.trail.push(TrailOp::SigOverwrite(key, old)),
            None => self.trail.push(TrailOp::SigInsert(key)),
        }
    }
    fn sig_remove(&mut self, key: &Sig) {
        if let Some(old) = self.sig.remove(key) {
            self.trail.push(TrailOp::SigRemove(key.clone(), old));
        }
    }

    // ----- congruence closure -----

    /// Assert the equality `a = b` with the given reason and run closure to a fixpoint.
    fn merge(&mut self, a: TermId, b: TermId, reason: Reason) {
        self.pending.push((a, b, reason));
        self.propagate_pending();
    }

    fn propagate_pending(&mut self) {
        while let Some((a, b, reason)) = self.pending.pop() {
            let ra = self.find(a);
            let rb = self.find(b);
            if ra == rb {
                continue;
            }

            // Add the proof edge a — b before changing union-find. Re-root the *original*
            // endpoints' proof trees so we can hang one under the other.
            self.add_proof_edge(a, b, reason);

            // Union by size: attach the smaller class under the larger root.
            let (small, large) = if self.size[ra.index()] <= self.size[rb.index()] {
                (ra, rb)
            } else {
                (rb, ra)
            };

            // Detach `small`'s use-list and record it so `pop` can put it back verbatim.
            let small_uses = std::mem::take(&mut self.uses[small.index()]);
            self.trail
                .push(TrailOp::UseTake(small.index(), small_uses.clone()));

            // Remove signatures of `small`'s use terms (their arg-reprs are about to change).
            for &app in &small_uses {
                let key = self.signature(app);
                self.sig_remove(&key);
            }

            // Re-root the union-find.
            self.set_repr(small.index(), large);
            let new_size = self.size[large.index()] + self.size[small.index()];
            self.set_size(large.index(), new_size);

            // Re-insert / reconcile signatures, moving use terms onto `large`.
            for &app in &small_uses {
                let key = self.signature(app);
                if let Some(&other) = self.sig.get(&key) {
                    // Congruent to an existing application: schedule a merge.
                    self.pending
                        .push((app, other, Reason::Congruence(app, other)));
                } else {
                    self.sig_set(key, app);
                }
                self.push_use(large.index(), app);
            }
        }
    }

    /// Add an undirected proof edge `a — b` justified by `reason`, re-rooting one side.
    fn add_proof_edge(&mut self, a: TermId, b: TermId, reason: Reason) {
        // Re-root the proof tree containing `a` at `a` (reverse the path a -> root), then make
        // `a`'s parent `b`. Re-rooting the smaller side keeps it cheap; we re-root `a`'s side.
        self.reroot_proof(a);
        self.set_proof(a.index(), Some(b), Some(reason));
    }

    /// Reverse the proof-forest path from `t` up to its root so that `t` becomes a root.
    fn reroot_proof(&mut self, t: TermId) {
        let mut prev: Option<(TermId, Reason)> = None;
        let mut cur = t;
        loop {
            let next = self.proof_parent[cur.index()];
            let next_reason = self.proof_reason[cur.index()];
            // Set cur's parent to `prev` (reversing the edge we just came along).
            match prev {
                Some((p, r)) => self.set_proof(cur.index(), Some(p), Some(r)),
                None => self.set_proof(cur.index(), None, None),
            }
            match (next, next_reason) {
                (Some(n), Some(r)) => {
                    prev = Some((cur, r));
                    cur = n;
                }
                _ => break,
            }
        }
    }

    // ----- explanations -----

    /// The set of `Input` equality literals justifying `a ≡ b`, collected over the proof path.
    fn explain_eq(&self, a: TermId, b: TermId) -> Vec<Lit> {
        let mut out = Vec::new();
        let mut seen_pairs: Vec<(TermId, TermId)> = Vec::new();
        self.explain_into(a, b, &mut out, &mut seen_pairs);
        out.sort_by_key(|l| l.code());
        out.dedup();
        out
    }

    fn explain_into(
        &self,
        a: TermId,
        b: TermId,
        out: &mut Vec<Lit>,
        seen: &mut Vec<(TermId, TermId)>,
    ) {
        if a == b {
            return;
        }
        // Walk the proof path between a and b. Find their nearest common ancestor in the proof
        // forest by collecting ancestor sets.
        let path = self.proof_path(a, b);
        for &(x, y) in &path {
            // Edge x — y: the recorded reason lives on whichever endpoint stored the parent
            // pointer toward the other.
            let reason = if self.proof_parent[x.index()] == Some(y) {
                self.proof_reason[x.index()]
            } else {
                self.proof_reason[y.index()]
            };
            match reason {
                Some(Reason::Input(lit)) => out.push(lit),
                Some(Reason::Congruence(app1, app2)) => {
                    // Recurse on argument pairs.
                    let args1: Vec<TermId> = self.arena.args_of(app1).to_vec();
                    let args2: Vec<TermId> = self.arena.args_of(app2).to_vec();
                    for (a1, a2) in args1.into_iter().zip(args2) {
                        let key = (a1.min(a2), a1.max(a2));
                        if !seen.contains(&key) {
                            seen.push(key);
                            self.explain_into(a1, a2, out, seen);
                        }
                    }
                }
                None => {
                    // Should not happen for an edge on the path between two equal terms.
                    debug_assert!(false, "proof edge without reason");
                }
            }
        }
    }

    /// The list of edges `(x, y)` on the unique proof-forest path from `a` to `b`. Both must be
    /// in the same proof tree (guaranteed when `find(a) == find(b)`).
    fn proof_path(&self, a: TermId, b: TermId) -> Vec<(TermId, TermId)> {
        // Collect ancestors of `a` (with depth), then climb from `b` until we hit one.
        let mut anc_a: Vec<TermId> = Vec::new();
        let mut x = a;
        anc_a.push(x);
        while let Some(p) = self.proof_parent[x.index()] {
            anc_a.push(p);
            x = p;
        }
        let mut anc_b: Vec<TermId> = Vec::new();
        let mut y = b;
        anc_b.push(y);
        while let Some(p) = self.proof_parent[y.index()] {
            anc_b.push(p);
            y = p;
        }
        // Find the lowest common ancestor: the first node of anc_a that appears in anc_b.
        let mut lca = None;
        'outer: for &na in &anc_a {
            for &nb in &anc_b {
                if na == nb {
                    lca = Some(na);
                    break 'outer;
                }
            }
        }
        let lca = lca.expect("terms not in same proof tree");

        let mut edges = Vec::new();
        // a -> lca
        let mut cur = a;
        while cur != lca {
            let p = self.proof_parent[cur.index()].unwrap();
            edges.push((cur, p));
            cur = p;
        }
        // b -> lca
        let mut cur = b;
        while cur != lca {
            let p = self.proof_parent[cur.index()].unwrap();
            edges.push((cur, p));
            cur = p;
        }
        edges
    }
}

impl Theory for Euf {
    fn assert(&mut self, lit: Lit) {
        let vi = lit.var().index();
        let (a, b) = match self.atoms.get(vi).copied().flatten() {
            Some(p) => p,
            None => return, // not a theory atom we know about
        };
        self.trail.push(TrailOp::Assigned(vi, self.assigned[vi]));
        self.assigned[vi] = if lit.is_negated() {
            Lbool::False
        } else {
            Lbool::True
        };
        if lit.is_negated() {
            // Disequality a != b.
            self.diseq.push((a, b, lit));
            self.trail.push(TrailOp::DiseqPush);
        } else {
            // Equality a = b.
            self.merge(a, b, Reason::Input(lit));
        }
    }

    fn check(&mut self, _complete: bool) -> Result<(), Vec<Lit>> {
        for i in 0..self.diseq.len() {
            let (a, b, neg_lit) = self.diseq[i];
            if self.find(a) == self.find(b) {
                // Conflict: a = b is derivable yet a != b was asserted.
                let mut clause = self.explain_eq(a, b);
                // explain returns the Input equality literals e (all currently TRUE), so we
                // negate them; together with the positive atom (= !neg_lit).
                for l in clause.iter_mut() {
                    *l = !*l;
                }
                clause.push(!neg_lit);
                clause.sort_by_key(|l| l.code());
                clause.dedup();
                return Err(clause);
            }
        }
        Ok(())
    }

    fn propagate(&mut self) -> Vec<Lit> {
        // Deduce equality atoms whose truth is now forced by the congruence closure:
        //  * `find(a) == find(b)`  =>  the atom is implied TRUE  (a and b proven equal);
        //  * a and b sit in classes separated by an asserted disequality  =>  implied FALSE.
        // Skip atoms already assigned. Explanations are reconstructed lazily in `explain`.
        let mut implied = Vec::new();

        // Snapshot the current disequal class-pairs once (representatives are stable until the
        // next merge / backtrack), mapping each to a witnessing disequality index.
        let mut diseq_pairs: FxHashMap<(u32, u32), usize> = FxHashMap::default();
        for (i, &(p, q, _)) in self.diseq.iter().enumerate() {
            let rp = self.find(p);
            let rq = self.find(q);
            diseq_pairs.entry(ordered(rp, rq)).or_insert(i);
        }

        for vi in 0..self.atoms.len() {
            if self.assigned[vi] != Lbool::Undef {
                continue;
            }
            let (a, b) = match self.atoms[vi] {
                Some(p) => p,
                None => continue,
            };
            let ra = self.find(a);
            let rb = self.find(b);
            let var = Var::from_index(vi);
            if ra == rb {
                implied.push(var.pos());
            } else if let Some(&di) = diseq_pairs.get(&ordered(ra, rb)) {
                let (p, q, neg_lit) = self.diseq[di];
                // Orient so a ≡ p and b ≡ q.
                let (p, q) = if self.find(p) == ra { (p, q) } else { (q, p) };
                self.neg_witness[vi] = Some((p, q, neg_lit));
                implied.push(var.neg());
            }
        }
        implied
    }

    fn explain(&mut self, lit: Lit) -> Vec<Lit> {
        // Reason clause for a theory-propagated literal `lit`: `[lit] ++ [!a for a in antecedents]`,
        // where the antecedents are the currently-true literals that forced `lit`. This serves
        // both as an asserting reason (lit true, rest false) and, when `lit` is already false, as
        // an all-false conflict clause.
        let vi = lit.var().index();
        let (a, b) = self.atoms[vi].expect("explain on a non-atom literal");
        let mut clause = vec![lit];
        if lit.is_negated() {
            // a != b was implied by the disequality pinned at propagation time, with a≡p, b≡q.
            let (p, q, neg_lit) =
                self.neg_witness[vi].expect("missing negative-propagation witness");
            for e in self.explain_eq(a, p) {
                clause.push(!e);
            }
            for e in self.explain_eq(b, q) {
                clause.push(!e);
            }
            clause.push(!neg_lit);
        } else {
            // a = b was implied because the closure proved them equal.
            for e in self.explain_eq(a, b) {
                clause.push(!e);
            }
        }
        clause.sort_by_key(|l| l.code());
        clause.dedup();
        clause
    }

    fn push(&mut self) {
        self.levels.push(self.trail.len());
    }

    fn pop(&mut self, levels: usize) {
        for _ in 0..levels {
            let mark = self.levels.pop().expect("pop below level 0");
            while self.trail.len() > mark {
                let op = self.trail.pop().unwrap();
                self.undo(op);
            }
        }
    }
}

impl Euf {
    fn undo(&mut self, op: TrailOp) {
        match op {
            TrailOp::Repr(i, old) => self.repr[i] = old,
            TrailOp::Size(i, old) => self.size[i] = old,
            TrailOp::UsePush(i) => {
                self.uses[i].pop();
            }
            TrailOp::SigInsert(key) => {
                self.sig.remove(&key);
            }
            TrailOp::SigOverwrite(key, old) => {
                self.sig.insert(key, old);
            }
            TrailOp::SigRemove(key, old) => {
                self.sig.insert(key, old);
            }
            TrailOp::Proof(i, p, r) => {
                self.proof_parent[i] = p;
                self.proof_reason[i] = r;
            }
            TrailOp::UseTake(i, v) => {
                self.uses[i] = v;
            }
            TrailOp::DiseqPush => {
                self.diseq.pop();
            }
            TrailOp::Assigned(vi, old) => {
                self.assigned[vi] = old;
            }
        }
    }
}

/// An unordered pair of term ids as a canonical `(min, max)` key.
#[inline]
fn ordered(a: TermId, b: TermId) -> (u32, u32) {
    let (x, y) = (a.index() as u32, b.index() as u32);
    if x <= y {
        (x, y)
    } else {
        (y, x)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashMap;

    /// Test harness: an `Euf` plus a record of the current (partial) assignment, so we can
    /// check that every literal in a returned conflict clause is currently FALSE.
    struct Harness {
        euf: Euf,
        next_var: usize,
        /// var index -> assigned bool (true/false). Absent = unassigned.
        assign: HashMap<usize, bool>,
    }

    impl Harness {
        fn new() -> Harness {
            Harness {
                euf: Euf::new(),
                next_var: 0,
                assign: HashMap::new(),
            }
        }
        fn c(&mut self, name: &str) -> TermId {
            self.euf.mk_const(name)
        }
        fn f(&mut self, name: &str, args: Vec<TermId>) -> TermId {
            self.euf.mk_app(name, args)
        }
        /// Allocate a fresh equality-atom variable for `a = b`.
        fn atom(&mut self, a: TermId, b: TermId) -> Var {
            let v = Var::from_index(self.next_var);
            self.next_var += 1;
            self.euf.register_eq_atom(v, a, b);
            v
        }
        /// Assert a literal and remember its truth value.
        fn assert(&mut self, lit: Lit) {
            self.assign.insert(lit.var().index(), !lit.is_negated());
            self.euf.assert(lit);
        }
        /// True iff `lit` is currently assigned FALSE.
        fn is_false(&self, lit: Lit) -> bool {
            match self.assign.get(&lit.var().index()) {
                Some(&val) => val == lit.is_negated(), // val == false when lit positive, etc.
                None => false,
            }
        }
        fn check(&mut self) -> Result<(), Vec<Lit>> {
            self.euf.check(true)
        }
        fn assert_clause_all_false(&self, clause: &[Lit]) {
            for &l in clause {
                assert!(
                    self.is_false(l),
                    "clause literal {:?} (var {}) is not currently false; assign={:?}",
                    l,
                    l.var().index(),
                    self.assign
                );
            }
        }
    }

    #[test]
    fn direct_congruence_conflict() {
        // a = b  =>  f(a) = f(b); assert f(a) != f(b) => conflict.
        let mut h = Harness::new();
        let a = h.c("a");
        let b = h.c("b");
        let fa = h.f("f", vec![a]);
        let fb = h.f("f", vec![b]);

        let eq_ab = h.atom(a, b);
        let eq_fafb = h.atom(fa, fb);

        h.assert(eq_ab.pos()); // a = b
        h.assert(eq_fafb.neg()); // f(a) != f(b)

        let conflict = h.check().expect_err("expected a conflict");
        h.assert_clause_all_false(&conflict);
        // Clause should contain ¬(a=b) and the positive atom f(a)=f(b).
        assert!(conflict.contains(&eq_ab.neg()));
        assert!(conflict.contains(&eq_fafb.pos()));
    }

    #[test]
    fn transitive_congruence_conflict() {
        // a=b, c=d, b=d, f(a) != f(c) => conflict (the case the Java solver got wrong).
        let mut h = Harness::new();
        let a = h.c("a");
        let b = h.c("b");
        let cc = h.c("c");
        let d = h.c("d");
        let fa = h.f("f", vec![a]);
        let fc = h.f("f", vec![cc]);

        let eq_ab = h.atom(a, b);
        let eq_cd = h.atom(cc, d);
        let eq_bd = h.atom(b, d);
        let eq_fafc = h.atom(fa, fc);

        h.assert(eq_ab.pos());
        h.assert(eq_cd.pos());
        h.assert(eq_bd.pos());
        h.assert(eq_fafc.neg());

        let conflict = h.check().expect_err("expected a conflict");
        h.assert_clause_all_false(&conflict);
        assert!(conflict.contains(&eq_fafc.pos()));
        // a..c equality chain needs a=b, b=d, c=d.
        assert!(conflict.contains(&eq_ab.neg()));
        assert!(conflict.contains(&eq_bd.neg()));
        assert!(conflict.contains(&eq_cd.neg()));
    }

    #[test]
    fn sat_case_no_conflict() {
        // a=b, f(a) != f(c) => no conflict.
        let mut h = Harness::new();
        let a = h.c("a");
        let b = h.c("b");
        let cc = h.c("c");
        let fa = h.f("f", vec![a]);
        let fc = h.f("f", vec![cc]);

        let eq_ab = h.atom(a, b);
        let eq_fafc = h.atom(fa, fc);

        h.assert(eq_ab.pos());
        h.assert(eq_fafc.neg());

        assert!(h.check().is_ok(), "should be satisfiable");
    }

    #[test]
    fn push_pop_basic() {
        // assert a=b at level 0; push; assert a!=b -> conflict; pop(1); assert a=c consistent.
        let mut h = Harness::new();
        let a = h.c("a");
        let b = h.c("b");
        let cc = h.c("c");

        let eq_ab = h.atom(a, b);
        let neq_ab = h.atom(a, b); // separate atom var also for a=b
        let eq_ac = h.atom(a, cc);

        h.assert(eq_ab.pos());
        assert!(h.check().is_ok());

        h.euf.push();
        h.assert(neq_ab.neg()); // a != b, contradicts a = b
        let conflict = h.check().expect_err("expected conflict under push");
        h.assert_clause_all_false(&conflict);

        h.euf.pop(1);
        // The level-1 disequality is undone. a=c is consistent with a=b.
        h.assert(eq_ac.pos());
        assert!(h.check().is_ok(), "after pop, a=c should be consistent");
    }

    #[test]
    fn push_pop_state_restored_across_cycles() {
        // Verify find/merge state is restored across several push/pop cycles.
        let mut h = Harness::new();
        let a = h.c("a");
        let b = h.c("b");
        let cc = h.c("c");
        let fa = h.f("f", vec![a]);
        let fc = h.f("f", vec![cc]);

        let eq_ab = h.atom(a, b);
        let eq_bc = h.atom(b, cc);
        let eq_fafc = h.atom(fa, fc);

        // Baseline: nothing equal.
        assert_ne!(h.euf.find(a), h.euf.find(b));

        for _ in 0..5 {
            h.euf.push();
            h.assert(eq_ab.pos());
            assert_eq!(h.euf.find(a), h.euf.find(b));
            // f(a) and f(c) not yet equal.
            assert_ne!(h.euf.find(fa), h.euf.find(fc));

            h.euf.push();
            h.assert(eq_bc.pos());
            // Now a=b=c, so by congruence f(a)=f(c).
            assert_eq!(h.euf.find(a), h.euf.find(cc));
            assert_eq!(h.euf.find(fa), h.euf.find(fc));

            // f(a) != f(c) must now conflict.
            h.assert(eq_fafc.neg());
            let conflict = h.check().expect_err("congruence conflict");
            h.assert_clause_all_false(&conflict);

            h.euf.pop(2);
            // Fully restored to baseline.
            assert_ne!(h.euf.find(a), h.euf.find(b));
            assert_ne!(h.euf.find(b), h.euf.find(cc));
            assert_ne!(h.euf.find(fa), h.euf.find(fc));
            // assignment record cleared for popped vars (so harness stays consistent)
            h.assign.remove(&eq_ab.pos().var().index());
            h.assign.remove(&eq_bc.pos().var().index());
            h.assign.remove(&eq_fafc.pos().var().index());
        }
    }

    // ---- fuzz / property test against a brute-force reference ----

    /// Brute-force reference: maintain explicit equalities, compute their congruence closure by
    /// fixpoint, and report whether any asserted disequality is violated.
    struct Reference {
        nconsts: usize,
        // terms: 0..nconsts are constants; nconsts..2*nconsts are f(const_i).
        eqs: Vec<(usize, usize)>,
        diseqs: Vec<(usize, usize)>,
    }
    impl Reference {
        fn nterms(&self) -> usize {
            2 * self.nconsts
        }
        fn fof(&self, i: usize) -> usize {
            // f(const i) lives at nconsts + i (only defined for constants here)
            self.nconsts + i
        }
        /// Returns true iff consistent (no asserted diseq is implied equal).
        fn consistent(&self) -> bool {
            let n = self.nterms();
            // union-find
            let mut parent: Vec<usize> = (0..n).collect();
            fn find(p: &mut Vec<usize>, mut x: usize) -> usize {
                while p[x] != x {
                    p[x] = p[p[x]];
                    x = p[x];
                }
                x
            }
            fn union(p: &mut Vec<usize>, a: usize, b: usize) {
                let ra = find(p, a);
                let rb = find(p, b);
                if ra != rb {
                    p[ra] = rb;
                }
            }
            for &(a, b) in &self.eqs {
                union(&mut parent, a, b);
            }
            // congruence fixpoint: if const i == const j then f(i) == f(j)
            loop {
                let mut changed = false;
                for i in 0..self.nconsts {
                    for j in (i + 1)..self.nconsts {
                        if find(&mut parent, i) == find(&mut parent, j) {
                            let fi = self.fof(i);
                            let fj = self.fof(j);
                            if find(&mut parent, fi) != find(&mut parent, fj) {
                                union(&mut parent, fi, fj);
                                changed = true;
                            }
                        }
                    }
                }
                if !changed {
                    break;
                }
            }
            for &(a, b) in &self.diseqs {
                if find(&mut parent, a) == find(&mut parent, b) {
                    return false;
                }
            }
            true
        }
    }

    // A tiny deterministic PRNG (xorshift) so the test is reproducible without deps.
    struct Rng(u64);
    impl Rng {
        fn next(&mut self) -> u64 {
            let mut x = self.0;
            x ^= x << 13;
            x ^= x >> 7;
            x ^= x << 17;
            self.0 = x;
            x
        }
        fn below(&mut self, n: usize) -> usize {
            (self.next() % n as u64) as usize
        }
    }

    #[test]
    fn fuzz_against_reference() {
        const NCONSTS: usize = 4;
        let mut rng = Rng(0x9E3779B97F4A7C15);

        for _trial in 0..400 {
            let mut h = Harness::new();
            // constants
            let consts: Vec<TermId> = (0..NCONSTS).map(|i| h.c(&format!("c{i}"))).collect();
            // f(c_i)
            let fconsts: Vec<TermId> = (0..NCONSTS).map(|i| h.f("f", vec![consts[i]])).collect();

            // Build a flat list of terms matching the reference numbering.
            let term_of = |idx: usize| -> TermId {
                if idx < NCONSTS {
                    consts[idx]
                } else {
                    fconsts[idx - NCONSTS]
                }
            };

            let nterms = 2 * NCONSTS;
            let mut reference = Reference {
                nconsts: NCONSTS,
                eqs: Vec::new(),
                diseqs: Vec::new(),
            };

            let nassert = 3 + rng.below(8);
            for _ in 0..nassert {
                let i = rng.below(nterms);
                let mut j = rng.below(nterms);
                while j == i {
                    j = rng.below(nterms);
                }
                let positive = rng.below(2) == 0;
                let ti = term_of(i);
                let tj = term_of(j);
                let v = h.atom(ti, tj);
                if positive {
                    reference.eqs.push((i, j));
                    h.assert(v.pos());
                } else {
                    reference.diseqs.push((i, j));
                    h.assert(v.neg());
                }
                // After each assertion, cross-check verdicts.
                let verdict = h.check();
                let euf_ok = verdict.is_ok();
                let ref_ok = reference.consistent();
                assert_eq!(
                    euf_ok, ref_ok,
                    "verdict mismatch: euf_ok={euf_ok} ref_ok={ref_ok}\n eqs={:?} diseqs={:?}",
                    reference.eqs, reference.diseqs
                );
                if let Err(clause) = verdict {
                    // Validate the conflict clause is all-false, then stop (in a real solver
                    // the SAT layer would backtrack past this inconsistent assignment).
                    h.assert_clause_all_false(&clause);
                    break;
                }
            }
        }
    }
}
