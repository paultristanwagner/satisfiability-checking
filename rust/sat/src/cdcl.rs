//! A CDCL SAT solver: two-watched literals, VSIDS, 1-UIP analysis with non-chronological
//! backjumping, Luby restarts, and LBD-based clause-database reduction.
//!
//! Follows the MiniSat/Glucose lineage. Data structures are index-based (no `Rc`/identity
//! maps): clauses live in a `Vec` addressed by [`ClauseRef`]; per-variable data is
//! struct-of-arrays; watch lists are indexed by `Lit::index()`.

use crate::heap::VarHeap;
use satcheck_core::{Lbool, Lit, Var};

/// Index of a clause in the solver's clause vector.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct ClauseRef(u32);

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Reason {
    Decision,
    Clause(ClauseRef),
}

struct Clause {
    lits: Vec<Lit>,
    learnt: bool,
    /// Literal Block Distance (number of distinct decision levels), used for reduction.
    lbd: u32,
    activity: f64,
    deleted: bool,
}

#[derive(Clone, Copy)]
struct Watch {
    clause: ClauseRef,
    /// The clause's other watched literal — if already true, the clause is satisfied (shortcut).
    blocker: Lit,
}

/// Outcome of a solve. `Sat` carries a full model indexed by variable.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum SolveResult {
    Sat(Vec<bool>),
    Unsat,
}

pub struct Solver {
    num_vars: usize,
    clauses: Vec<Clause>,
    learnts: Vec<ClauseRef>,
    watches: Vec<Vec<Watch>>, // length 2*num_vars

    // assignment / trail (struct-of-arrays)
    assigns: Vec<Lbool>,
    polarity: Vec<bool>, // saved phase
    level: Vec<u32>,
    reason: Vec<Reason>,
    trail: Vec<Lit>,
    trail_lim: Vec<usize>,
    qhead: usize,

    // VSIDS
    activity: Vec<f64>,
    var_inc: f64,
    var_decay: f64,
    order: VarHeap,

    // clause activity (for learnt reduction)
    cla_inc: f64,
    cla_decay: f64,

    // bookkeeping
    conflicts: u64,
    ok: bool, // false once unsatisfiability is detected at level 0

    // reusable scratch
    seen: Vec<bool>,
    lbd_stamp: Vec<u64>,
    lbd_counter: u64,
}

impl Default for Solver {
    fn default() -> Self {
        Solver::new()
    }
}

impl Solver {
    pub fn new() -> Solver {
        Solver {
            num_vars: 0,
            clauses: Vec::new(),
            learnts: Vec::new(),
            watches: Vec::new(),
            assigns: Vec::new(),
            polarity: Vec::new(),
            level: Vec::new(),
            reason: Vec::new(),
            trail: Vec::new(),
            trail_lim: Vec::new(),
            qhead: 0,
            activity: Vec::new(),
            var_inc: 1.0,
            var_decay: 0.95,
            order: VarHeap::default(),
            cla_inc: 1.0,
            cla_decay: 0.999,
            conflicts: 0,
            ok: true,
            seen: Vec::new(),
            lbd_stamp: Vec::new(),
            lbd_counter: 0,
        }
    }

    /// Ensure variables `0..n` exist.
    pub fn ensure_vars(&mut self, n: usize) {
        if n <= self.num_vars {
            return;
        }
        self.assigns.resize(n, Lbool::Undef);
        self.polarity.resize(n, false);
        self.level.resize(n, 0);
        self.reason.resize(n, Reason::Decision);
        self.activity.resize(n, 0.0);
        self.seen.resize(n, false);
        self.lbd_stamp.resize(n, 0);
        self.watches.resize(2 * n, Vec::new());
        for v in self.num_vars..n {
            let var = Var::from_index(v);
            self.order.insert(&self.activity, var);
        }
        self.num_vars = n;
    }

    #[inline]
    fn value(&self, l: Lit) -> Lbool {
        match self.assigns[l.var().index()] {
            Lbool::Undef => Lbool::Undef,
            v => {
                // `v` is the variable's value; the literal's value flips if negated.
                if l.is_negated() {
                    if v == Lbool::True {
                        Lbool::False
                    } else {
                        Lbool::True
                    }
                } else {
                    v
                }
            }
        }
    }

    #[inline]
    fn decision_level(&self) -> u32 {
        self.trail_lim.len() as u32
    }

    /// Add a clause (DIMACS-style literal list). Returns `false` if this makes the formula
    /// trivially unsatisfiable. Must be called at decision level 0.
    pub fn add_clause(&mut self, lits: &[Lit]) -> bool {
        debug_assert_eq!(self.decision_level(), 0);
        if !self.ok {
            return false;
        }
        // Normalize: ensure vars exist, dedup, detect tautology, drop level-0-false lits.
        let maxvar = lits.iter().map(|l| l.var().index()).max();
        if let Some(mv) = maxvar {
            self.ensure_vars(mv + 1);
        }
        let mut c: Vec<Lit> = Vec::with_capacity(lits.len());
        for &l in lits {
            match self.value(l) {
                Lbool::True => return true, // satisfied at level 0; drop the whole clause
                Lbool::False => continue,   // false at level 0; drop the literal
                Lbool::Undef => {
                    if c.contains(&l) {
                        continue; // duplicate
                    }
                    if c.contains(&!l) {
                        return true; // tautology
                    }
                    c.push(l);
                }
            }
        }
        match c.len() {
            0 => {
                self.ok = false;
                false
            }
            1 => {
                self.enqueue(c[0], Reason::Decision);
                // a top-level unit; conflict (if any) surfaces on the next propagate()
                self.ok
            }
            _ => {
                self.attach_clause(c, false);
                true
            }
        }
    }

    fn attach_clause(&mut self, lits: Vec<Lit>, learnt: bool) -> ClauseRef {
        let w0 = lits[0];
        let w1 = lits[1];
        let lbd = if learnt { self.compute_lbd(&lits) } else { 0 };
        let cref = ClauseRef(self.clauses.len() as u32);
        self.clauses.push(Clause {
            lits,
            learnt,
            lbd,
            activity: 0.0,
            deleted: false,
        });
        // Watch the negations of the two watched literals.
        self.watches[(!w0).index()].push(Watch {
            clause: cref,
            blocker: w1,
        });
        self.watches[(!w1).index()].push(Watch {
            clause: cref,
            blocker: w0,
        });
        if learnt {
            self.learnts.push(cref);
        }
        cref
    }

    #[inline]
    fn enqueue(&mut self, l: Lit, reason: Reason) {
        let v = l.var().index();
        self.assigns[v] = Lbool::from_bool(!l.is_negated());
        self.level[v] = self.decision_level();
        self.reason[v] = reason;
        self.trail.push(l);
    }

    /// Boolean constraint propagation. Returns the conflicting clause, if any.
    fn propagate(&mut self) -> Option<ClauseRef> {
        let mut confl = None;
        while self.qhead < self.trail.len() {
            let p = self.trail[self.qhead];
            self.qhead += 1;
            // Process clauses watching ¬p (i.e. those whose watched literal ¬p just became false).
            let mut ws = std::mem::take(&mut self.watches[p.index()]);
            let mut i = 0;
            let mut keep = 0;
            'next_watch: while i < ws.len() {
                let w = ws[i];
                i += 1;
                // Shortcut: if the blocker is already satisfied, the clause is fine.
                if self.value(w.blocker) == Lbool::True {
                    ws[keep] = w;
                    keep += 1;
                    continue;
                }
                let cref = w.clause;
                let ci = cref.0 as usize;
                let false_lit = !p;
                // Make sure the false literal is at position 1.
                if self.clauses[ci].lits[0] == false_lit {
                    self.clauses[ci].lits.swap(0, 1);
                }
                let first = self.clauses[ci].lits[0];
                // If the other watched literal is true, the clause is satisfied.
                if first != w.blocker && self.value(first) == Lbool::True {
                    ws[keep] = Watch {
                        clause: cref,
                        blocker: first,
                    };
                    keep += 1;
                    continue;
                }
                // Look for a new literal to watch among positions 2..len.
                let len = self.clauses[ci].lits.len();
                for k in 2..len {
                    let lk = self.clauses[ci].lits[k];
                    if self.value(lk) != Lbool::False {
                        // move lk to the watched position 1, relocate the watch
                        self.clauses[ci].lits.swap(1, k);
                        self.watches[(!lk).index()].push(Watch {
                            clause: cref,
                            blocker: first,
                        });
                        continue 'next_watch; // do not keep in p's list
                    }
                }
                // No new watch: clause is unit or conflicting under `first`.
                match self.value(first) {
                    Lbool::False => {
                        // conflict: keep remaining watches and stop.
                        ws[keep] = w;
                        keep += 1;
                        while i < ws.len() {
                            ws[keep] = ws[i];
                            keep += 1;
                            i += 1;
                        }
                        confl = Some(cref);
                        break;
                    }
                    _ => {
                        ws[keep] = w;
                        keep += 1;
                        self.enqueue(first, Reason::Clause(cref));
                    }
                }
            }
            ws.truncate(keep);
            self.watches[p.index()] = ws;
            if confl.is_some() {
                self.qhead = self.trail.len();
                break;
            }
        }
        confl
    }

    // ---- VSIDS ----
    #[inline]
    fn bump_var(&mut self, v: Var) {
        self.activity[v.index()] += self.var_inc;
        if self.activity[v.index()] > 1e100 {
            for a in self.activity.iter_mut() {
                *a *= 1e-100;
            }
            self.var_inc *= 1e-100;
        }
        self.order.increase(&self.activity, v);
    }
    #[inline]
    fn decay_var(&mut self) {
        self.var_inc /= self.var_decay;
    }

    #[inline]
    fn bump_clause(&mut self, ci: usize) {
        self.clauses[ci].activity += self.cla_inc;
        if self.clauses[ci].activity > 1e20 {
            for &cr in &self.learnts {
                self.clauses[cr.0 as usize].activity *= 1e-20;
            }
            self.cla_inc *= 1e-20;
        }
    }
    #[inline]
    fn decay_clause(&mut self) {
        self.cla_inc /= self.cla_decay;
    }

    fn compute_lbd(&mut self, lits: &[Lit]) -> u32 {
        self.lbd_counter += 1;
        let stamp = self.lbd_counter;
        let mut lbd = 0;
        for &l in lits {
            let lev = self.level[l.var().index()] as usize;
            // `lbd_stamp` is indexed by decision level; grow lazily.
            if lev >= self.lbd_stamp.len() {
                self.lbd_stamp.resize(lev + 1, 0);
            }
            if self.lbd_stamp[lev] != stamp {
                self.lbd_stamp[lev] = stamp;
                lbd += 1;
            }
        }
        lbd
    }

    /// 1-UIP conflict analysis. Returns the learnt clause (asserting literal first) and the
    /// level to backjump to.
    fn analyze(&mut self, confl: ClauseRef) -> (Vec<Lit>, u32) {
        let mut learnt: Vec<Lit> = vec![Lit::from_code(0)]; // placeholder for the UIP at [0]
        let mut path_count = 0i32;
        let mut p: Option<Lit> = None;
        let mut confl = confl;
        let mut index = self.trail.len();

        loop {
            // Bump clause activity; iterate its literals.
            let ci = confl.0 as usize;
            if self.clauses[ci].learnt {
                self.bump_clause(ci);
            }
            let clause_len = self.clauses[ci].lits.len();
            for j in 0..clause_len {
                let q = self.clauses[ci].lits[j];
                if Some(q) == p {
                    continue; // skip the resolved-on literal
                }
                let v = q.var();
                if !self.seen[v.index()] && self.level[v.index()] > 0 {
                    self.bump_var(v);
                    self.seen[v.index()] = true;
                    if self.level[v.index()] >= self.decision_level() {
                        path_count += 1;
                    } else {
                        learnt.push(q);
                    }
                }
            }
            // Select the next literal to resolve: the most recent seen literal on the trail.
            loop {
                index -= 1;
                if self.seen[self.trail[index].var().index()] {
                    break;
                }
            }
            let pl = self.trail[index];
            self.seen[pl.var().index()] = false;
            p = Some(pl);
            path_count -= 1;
            if path_count <= 0 {
                learnt[0] = !pl; // the asserting literal (1-UIP)
                break;
            }
            confl = match self.reason[pl.var().index()] {
                Reason::Clause(c) => c,
                Reason::Decision => unreachable!("UIP reached a decision with path_count > 0"),
            };
        }

        // Minimal cleanup: clear `seen` for the literals we recorded.
        // Determine backjump level = second-highest level in the learnt clause.
        let mut backtrack_level = 0u32;
        if learnt.len() > 1 {
            let mut max_i = 1;
            for i in 2..learnt.len() {
                if self.level[learnt[i].var().index()] > self.level[learnt[max_i].var().index()] {
                    max_i = i;
                }
            }
            learnt.swap(1, max_i);
            backtrack_level = self.level[learnt[1].var().index()];
        }
        for &l in &learnt {
            self.seen[l.var().index()] = false;
        }
        (learnt, backtrack_level)
    }

    fn backtrack(&mut self, level: u32) {
        if self.decision_level() <= level {
            return;
        }
        let lim = self.trail_lim[level as usize];
        for i in (lim..self.trail.len()).rev() {
            let l = self.trail[i];
            let v = l.var();
            self.polarity[v.index()] = !l.is_negated(); // save phase
            self.assigns[v.index()] = Lbool::Undef;
            self.order.insert(&self.activity, v);
        }
        self.trail.truncate(lim);
        self.trail_lim.truncate(level as usize);
        self.qhead = self.trail.len();
    }

    fn pick_branch(&mut self) -> Option<Lit> {
        while let Some(v) = self.order.pop_max(&self.activity) {
            if self.assigns[v.index()] == Lbool::Undef {
                let neg = !self.polarity[v.index()]; // phase saving (default false => positive)
                return Some(Lit::new(v, neg));
            }
        }
        None
    }

    fn new_decision_level(&mut self) {
        self.trail_lim.push(self.trail.len());
    }

    fn reduce_db(&mut self) {
        // Sort learnts by (keep low LBD, then high activity); drop the worst half, but never
        // drop a clause that is currently a reason.
        let mut ls: Vec<ClauseRef> = self
            .learnts
            .iter()
            .copied()
            .filter(|c| !self.clauses[c.0 as usize].deleted)
            .collect();
        ls.sort_by(|&a, &b| {
            let ca = &self.clauses[a.0 as usize];
            let cb = &self.clauses[b.0 as usize];
            cb.lbd
                .cmp(&ca.lbd)
                .then(ca.activity.partial_cmp(&cb.activity).unwrap())
        });
        let half = ls.len() / 2;
        for &cr in ls.iter().take(half) {
            let ci = cr.0 as usize;
            if self.clauses[ci].lbd <= 2 {
                continue; // always keep glue clauses
            }
            if self.is_reason(cr) {
                continue;
            }
            self.detach_clause(cr);
        }
        self.learnts.retain(|c| !self.clauses[c.0 as usize].deleted);
    }

    fn is_reason(&self, cref: ClauseRef) -> bool {
        // A clause is a reason if some literal on the trail points to it.
        let ci = cref.0 as usize;
        let l0 = self.clauses[ci].lits[0];
        self.value(l0) == Lbool::True && self.reason[l0.var().index()] == Reason::Clause(cref)
    }

    fn detach_clause(&mut self, cref: ClauseRef) {
        let ci = cref.0 as usize;
        let w0 = self.clauses[ci].lits[0];
        let w1 = self.clauses[ci].lits[1];
        self.watches[(!w0).index()].retain(|w| w.clause != cref);
        self.watches[(!w1).index()].retain(|w| w.clause != cref);
        self.clauses[ci].deleted = true;
        self.clauses[ci].lits.clear();
    }

    /// Solve the current formula. Returns a model or UNSAT.
    pub fn solve(&mut self) -> SolveResult {
        if !self.ok {
            return SolveResult::Unsat;
        }
        let mut restart_no: u64 = 0;
        let mut conflicts_since_restart: u64 = 0;
        let mut max_conflicts = luby(2.0, restart_no) as u64 * 100;
        let mut learnt_limit = (self.clauses.len() as f64 * 1.3).max(1000.0);

        loop {
            match self.propagate() {
                Some(confl) => {
                    self.conflicts += 1;
                    conflicts_since_restart += 1;
                    if self.decision_level() == 0 {
                        self.ok = false;
                        return SolveResult::Unsat;
                    }
                    let (learnt, bt_level) = self.analyze(confl);
                    self.backtrack(bt_level);
                    if learnt.len() == 1 {
                        self.enqueue(learnt[0], Reason::Decision);
                    } else {
                        let asserting = learnt[0];
                        let cref = self.attach_clause(learnt, true);
                        self.bump_clause(cref.0 as usize);
                        self.enqueue(asserting, Reason::Clause(cref));
                    }
                    self.decay_var();
                    self.decay_clause();
                }
                None => {
                    // Restart?
                    if conflicts_since_restart >= max_conflicts {
                        self.backtrack(0);
                        restart_no += 1;
                        conflicts_since_restart = 0;
                        max_conflicts = luby(2.0, restart_no) as u64 * 100;
                    }
                    // Reduce learnt DB?
                    if self.learnts.len() as f64 >= learnt_limit {
                        self.reduce_db();
                        learnt_limit *= 1.1;
                    }
                    match self.pick_branch() {
                        None => {
                            // all variables assigned -> SAT
                            let model = (0..self.num_vars)
                                .map(|v| self.assigns[v] == Lbool::True)
                                .collect();
                            return SolveResult::Sat(model);
                        }
                        Some(dec) => {
                            self.new_decision_level();
                            self.enqueue(dec, Reason::Decision);
                        }
                    }
                }
            }
        }
    }
}

/// Luby sequence (used for restart intervals): 1,1,2,1,1,2,4,...
fn luby(y: f64, mut x: u64) -> f64 {
    let mut size = 1u64;
    let mut seq = 0u32;
    while size < x + 1 {
        seq += 1;
        size = 2 * size + 1;
    }
    while size - 1 != x {
        size = (size - 1) >> 1;
        seq -= 1;
        x %= size;
    }
    y.powi(seq as i32)
}
