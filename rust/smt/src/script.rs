//! SMT-LIB QF_UF/QF_EQ script processing: command handling + boolean-structure (Tseitin)
//! encoding of assertions into clauses over equality atoms, solved by [`SmtBuilder`] (DPLL(T)+EUF).
//!
//! Encoding scheme:
//! - An equality `(= s t)` between sort-`U` terms is an equality atom (one SAT variable in the EUF).
//! - A `U`-sorted term is built in the EUF term DAG (`mk_const`/`mk_app`); `(ite c a b)` over `U`
//!   is lifted to a fresh constant `t` with clauses `c -> t=a`, `!c -> t=b`.
//! - A `Bool`-sorted uninterpreted term `p` (a `Bool` constant or predicate application) is encoded
//!   as the atom `p = !true`, where `!true` is a reserved constant — so congruence over predicates
//!   works (if `a=b` then `(P a)` and `(P b)` are forced equal).
//! - Boolean connectives are Tseitin-encoded into auxiliary variables and clauses.

use crate::sexp::Sexp;
use crate::SmtBuilder;
use rustc_hash::FxHashMap;
use satcheck_core::Lit;
use satcheck_term::TermId;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Answer {
    Sat,
    Unsat,
}

impl Answer {
    pub fn as_str(self) -> &'static str {
        match self {
            Answer::Sat => "sat",
            Answer::Unsat => "unsat",
        }
    }
}

#[derive(Debug, Clone)]
pub struct CheckResult {
    pub answer: Answer,
    /// The `:status` annotation in scope at this `check-sat`, if any.
    pub expected: Option<Answer>,
}

struct Frame {
    assert_len: usize,
    decls: Vec<String>,
}

/// A processed SMT-LIB script. Each `(check-sat)` produces one [`CheckResult`].
pub struct Script {
    /// symbol -> (argument sorts, return sort)
    sigs: FxHashMap<String, (Vec<String>, String)>,
    asserts: Vec<Sexp>,
    frames: Vec<Frame>,
    expected: Option<Answer>,
    logic: Option<String>,
    pub results: Vec<CheckResult>,
}

impl Default for Script {
    fn default() -> Self {
        Script::new()
    }
}

impl Script {
    pub fn new() -> Script {
        Script {
            sigs: FxHashMap::default(),
            asserts: Vec::new(),
            frames: vec![Frame {
                assert_len: 0,
                decls: Vec::new(),
            }],
            expected: None,
            logic: None,
            results: Vec::new(),
        }
    }

    /// Parse and run a whole script.
    pub fn run(input: &str) -> Result<Script, String> {
        let cmds = crate::sexp::parse_script(input)?;
        let mut script = Script::new();
        for cmd in &cmds {
            script.command(cmd)?;
        }
        Ok(script)
    }

    /// Run a single command, returning the new [`CheckResult`] if it was a `(check-sat)`. Used by
    /// the interactive REPL to surface verdicts as soon as each command is entered.
    pub fn exec(&mut self, cmd: &Sexp) -> Result<Option<CheckResult>, String> {
        let before = self.results.len();
        self.command(cmd)?;
        Ok((self.results.len() > before).then(|| self.results.last().unwrap().clone()))
    }

    /// The logic set via `(set-logic …)`, if any (for the REPL prompt).
    pub fn logic(&self) -> Option<&str> {
        self.logic.as_deref()
    }

    fn command(&mut self, cmd: &Sexp) -> Result<(), String> {
        let list = cmd.as_list().ok_or("expected a command list")?;
        let head = list
            .first()
            .and_then(Sexp::as_atom)
            .ok_or("empty command")?;
        match head {
            "declare-fun" => self.declare_fun(&list[1..]),
            "declare-const" => self.declare_const(&list[1..]),
            "declare-sort" | "define-sort" => Ok(()),
            "assert" => {
                let a = list.get(1).ok_or("assert expects a formula")?;
                self.asserts.push(a.clone());
                Ok(())
            }
            "check-sat" | "check-sat-assuming" => {
                let r = self.solve()?;
                self.results.push(r);
                Ok(())
            }
            "push" => self.push(&list[1..]),
            "pop" => self.pop(&list[1..]),
            "set-info" => {
                self.set_info(&list[1..]);
                Ok(())
            }
            "set-logic" => {
                self.logic = list.get(1).and_then(Sexp::as_atom).map(str::to_string);
                Ok(())
            }
            "set-option" | "get-model" | "get-info" | "get-value" | "echo" | "reset" | "exit" => {
                Ok(())
            }
            other => Err(format!("unsupported command '{other}'")),
        }
    }

    fn declare_fun(&mut self, args: &[Sexp]) -> Result<(), String> {
        // (declare-fun name (argSorts...) retSort)
        if args.len() != 3 {
            return Err("declare-fun expects name, arg-sorts, ret-sort".to_string());
        }
        let name = args[0].as_atom().ok_or("declare-fun name")?.to_string();
        let arg_sorts: Vec<String> = args[1]
            .as_list()
            .ok_or("declare-fun arg sorts must be a list")?
            .iter()
            .map(|s| sort_name(s))
            .collect::<Result<_, _>>()?;
        let ret = sort_name(&args[2])?;
        self.frames.last_mut().unwrap().decls.push(name.clone());
        self.sigs.insert(name, (arg_sorts, ret));
        Ok(())
    }

    fn declare_const(&mut self, args: &[Sexp]) -> Result<(), String> {
        // (declare-const name retSort)
        if args.len() != 2 {
            return Err("declare-const expects name, ret-sort".to_string());
        }
        let name = args[0].as_atom().ok_or("declare-const name")?.to_string();
        let ret = sort_name(&args[1])?;
        self.frames.last_mut().unwrap().decls.push(name.clone());
        self.sigs.insert(name, (Vec::new(), ret));
        Ok(())
    }

    fn push(&mut self, args: &[Sexp]) -> Result<(), String> {
        let n = count_arg(args)?;
        for _ in 0..n {
            self.frames.push(Frame {
                assert_len: self.asserts.len(),
                decls: Vec::new(),
            });
        }
        Ok(())
    }

    fn pop(&mut self, args: &[Sexp]) -> Result<(), String> {
        let n = count_arg(args)?;
        for _ in 0..n {
            if self.frames.len() <= 1 {
                return Err("pop without matching push".to_string());
            }
            let f = self.frames.pop().unwrap();
            for d in &f.decls {
                self.sigs.remove(d);
            }
            self.asserts.truncate(f.assert_len);
        }
        Ok(())
    }

    fn set_info(&mut self, args: &[Sexp]) {
        // (set-info :status sat|unsat|unknown)
        if let [Sexp::Atom(key), Sexp::Atom(val)] = args {
            if key == ":status" {
                self.expected = match val.as_str() {
                    "sat" => Some(Answer::Sat),
                    "unsat" => Some(Answer::Unsat),
                    _ => None,
                };
            }
        }
    }

    /// Build a fresh solver instance, encode all in-scope assertions, and solve.
    fn solve(&self) -> Result<CheckResult, String> {
        let mut enc = Encoder::new(&self.sigs);
        for a in &self.asserts {
            let lit = enc.bool(a)?;
            enc.builder.add_clause(vec![lit]);
        }
        let sat = enc.builder.solve();
        Ok(CheckResult {
            answer: if sat { Answer::Sat } else { Answer::Unsat },
            expected: self.expected,
        })
    }
}

fn is_connective(head: &str) -> bool {
    matches!(
        head,
        "and" | "or" | "not" | "=>" | "xor" | "=" | "distinct" | "ite"
    )
}

fn count_arg(args: &[Sexp]) -> Result<usize, String> {
    match args.first() {
        None => Ok(1),
        Some(Sexp::Atom(s)) => s
            .parse::<usize>()
            .map_err(|_| "bad push/pop count".to_string()),
        _ => Err("bad push/pop count".to_string()),
    }
}

/// A sort is either a bare symbol `Bool`/`U` or a parametric `(_ ...)`/`(Array ...)` form;
/// for QF_UF we only need the head name.
fn sort_name(s: &Sexp) -> Result<String, String> {
    match s {
        Sexp::Atom(a) => Ok(a.clone()),
        Sexp::List(l) => l
            .first()
            .and_then(Sexp::as_atom)
            .map(|h| h.to_string())
            .ok_or_else(|| "malformed sort".to_string()),
    }
}

struct Encoder<'a> {
    builder: SmtBuilder,
    sigs: &'a FxHashMap<String, (Vec<String>, String)>,
    /// `true` literal, forced true by a unit clause.
    true_lit: Lit,
    /// Reserved EUF constants used to encode `Bool`-sorted terms: a term's truth is `term=bool_true`
    /// and its falsity `term=bool_false`, with `bool_true != bool_false` asserted globally. The
    /// two-valued encoding is needed so that `Bool`-sorted *function arguments* get correct
    /// congruence (two `false` arguments must be provably equal).
    bool_true: TermId,
    bool_false: TermId,
    /// Bool-sorted term nodes already given the `(=true) | (=false)` linkage clause.
    linked: rustc_hash::FxHashSet<TermId>,
    bt_bf_distinct_added: bool,
    ite_counter: usize,
}

impl<'a> Encoder<'a> {
    fn new(sigs: &'a FxHashMap<String, (Vec<String>, String)>) -> Encoder<'a> {
        let mut builder = SmtBuilder::new();
        let true_var = builder.fresh_var();
        let true_lit = true_var.pos();
        builder.add_clause(vec![true_lit]);
        let bool_true = builder.euf().mk_const("!true");
        let bool_false = builder.euf().mk_const("!false");
        Encoder {
            builder,
            sigs,
            true_lit,
            bool_true,
            bool_false,
            linked: rustc_hash::FxHashSet::default(),
            bt_bf_distinct_added: false,
            ite_counter: 0,
        }
    }

    /// Give a `Bool`-sorted term node `n` the two-valued linkage `(n=true) | (n=false)` (once),
    /// and ensure the global `bool_true != bool_false` constraint exists.
    fn link_bool(&mut self, n: TermId) {
        if n == self.bool_true || n == self.bool_false || !self.linked.insert(n) {
            return;
        }
        let et = self.builder.eq_atom(n, self.bool_true);
        let ef = self.builder.eq_atom(n, self.bool_false);
        self.builder.add_clause(vec![et, ef]);
        if !self.bt_bf_distinct_added {
            self.bt_bf_distinct_added = true;
            let e = self.builder.eq_atom(self.bool_true, self.bool_false);
            self.builder.add_clause(vec![!e]);
        }
    }

    /// Build the EUF node for a `Bool`-sorted term used *as a term* (e.g. a function argument),
    /// wiring its propositional truth literal to `node = bool_true`.
    fn bool_arg(&mut self, t: &Sexp) -> Result<TermId, String> {
        let node = match t {
            Sexp::Atom(a) => match a.as_str() {
                "true" => self.bool_true,
                "false" => self.bool_false,
                sym => self.builder.euf().mk_const(sym),
            },
            Sexp::List(l) => {
                let head = l
                    .first()
                    .and_then(Sexp::as_atom)
                    .ok_or("empty application")?;
                if is_connective(head) {
                    // A compound formula used as a term: a fresh node bound to its truth value.
                    let val = self.bool(t)?;
                    self.ite_counter += 1;
                    let n = self
                        .builder
                        .euf()
                        .mk_const(&format!("!b{}", self.ite_counter));
                    let et = self.builder.eq_atom(n, self.bool_true);
                    self.builder.add_clause(vec![!val, et]);
                    self.builder.add_clause(vec![val, !et]);
                    n
                } else {
                    // A predicate application: build the application node directly.
                    let mut args = Vec::with_capacity(l.len() - 1);
                    for a in &l[1..] {
                        args.push(self.term(a)?);
                    }
                    self.builder.euf().mk_app(head, args)
                }
            }
        };
        self.link_bool(node);
        Ok(node)
    }

    /// The sort of a term (only `"Bool"` vs everything-else matters for dispatch).
    fn sort_of(&self, t: &Sexp) -> Result<String, String> {
        match t {
            Sexp::Atom(a) => match a.as_str() {
                "true" | "false" => Ok("Bool".to_string()),
                sym => self
                    .sigs
                    .get(sym)
                    .map(|(_, ret)| ret.clone())
                    .ok_or_else(|| format!("undeclared symbol '{sym}'")),
            },
            Sexp::List(l) => {
                let head = l
                    .first()
                    .and_then(Sexp::as_atom)
                    .ok_or("empty application")?;
                match head {
                    "and" | "or" | "not" | "=>" | "xor" | "=" | "distinct" => {
                        Ok("Bool".to_string())
                    }
                    "ite" => self.sort_of(&l[2]), // sort of the then-branch
                    f => self
                        .sigs
                        .get(f)
                        .map(|(_, ret)| ret.clone())
                        .ok_or_else(|| format!("undeclared function '{f}'")),
                }
            }
        }
    }

    /// Build a term in the EUF term DAG. `Bool`-sorted terms are routed through [`Self::bool_arg`]
    /// so their truth is wired to `bool_true`/`bool_false`.
    fn term(&mut self, t: &Sexp) -> Result<TermId, String> {
        if self.sort_of(t)? == "Bool" {
            return self.bool_arg(t);
        }
        match t {
            Sexp::Atom(name) => Ok(self.builder.euf().mk_const(name)),
            Sexp::List(l) => {
                let head = l
                    .first()
                    .and_then(Sexp::as_atom)
                    .ok_or("empty application")?;
                if head == "ite" {
                    // term-level ITE: fresh constant lifted with c -> t=a, !c -> t=b
                    let cond = self.bool(&l[1])?;
                    let then_t = self.term(&l[2])?;
                    let else_t = self.term(&l[3])?;
                    self.ite_counter += 1;
                    let fresh = self
                        .builder
                        .euf()
                        .mk_const(&format!("!ite{}", self.ite_counter));
                    let eq_then = self.builder.eq_atom(fresh, then_t);
                    let eq_else = self.builder.eq_atom(fresh, else_t);
                    self.builder.add_clause(vec![!cond, eq_then]);
                    self.builder.add_clause(vec![cond, eq_else]);
                    Ok(fresh)
                } else {
                    let mut args = Vec::with_capacity(l.len() - 1);
                    for a in &l[1..] {
                        args.push(self.term(a)?);
                    }
                    Ok(self.builder.euf().mk_app(head, args))
                }
            }
        }
    }

    /// Encode a `Bool`-sorted formula into a literal, emitting Tseitin clauses as needed.
    fn bool(&mut self, t: &Sexp) -> Result<Lit, String> {
        match t {
            Sexp::Atom(a) => match a.as_str() {
                "true" => Ok(self.true_lit),
                "false" => Ok(!self.true_lit),
                sym => {
                    // a Bool-sorted constant: encode as (sym = !true)
                    let term = self.builder.euf().mk_const(sym);
                    Ok(self.builder.eq_atom(term, self.bool_true))
                }
            },
            Sexp::List(l) => {
                let head = l
                    .first()
                    .and_then(Sexp::as_atom)
                    .ok_or("empty application")?;
                let args = &l[1..];
                match head {
                    "not" => Ok(!self.bool(&args[0])?),
                    "and" => {
                        let lits = self.bool_all(args)?;
                        Ok(self.tseitin_and(lits))
                    }
                    "or" => {
                        let lits = self.bool_all(args)?;
                        Ok(self.tseitin_or(lits))
                    }
                    "=>" => {
                        // (=> a b ... z) == (or !a !b ... z_last)
                        let mut lits = self.bool_all(args)?;
                        let last = lits.pop().ok_or("=> needs args")?;
                        let mut clause: Vec<Lit> = lits.into_iter().map(|l| !l).collect();
                        clause.push(last);
                        Ok(self.tseitin_or(clause))
                    }
                    "xor" => {
                        let lits = self.bool_all(args)?;
                        let mut acc = lits[0];
                        for &l in &lits[1..] {
                            acc = self.mk_xor(acc, l);
                        }
                        Ok(acc)
                    }
                    "ite" => {
                        let c = self.bool(&args[0])?;
                        let th = self.bool(&args[1])?;
                        let el = self.bool(&args[2])?;
                        Ok(self.tseitin_ite(c, th, el))
                    }
                    "=" => self.eq(args),
                    "distinct" => self.distinct(args),
                    _ => {
                        // predicate application: encode as ((P args) = !true)
                        let term = self.term(t)?;
                        Ok(self.builder.eq_atom(term, self.bool_true))
                    }
                }
            }
        }
    }

    fn bool_all(&mut self, ts: &[Sexp]) -> Result<Vec<Lit>, String> {
        ts.iter().map(|t| self.bool(t)).collect()
    }

    /// `(= a b ...)`: iff-chain over Bool args, equality-atom chain over `U` args.
    fn eq(&mut self, args: &[Sexp]) -> Result<Lit, String> {
        if args.len() < 2 {
            return Ok(self.true_lit);
        }
        let is_bool = self.sort_of(&args[0])? == "Bool";
        if is_bool {
            let lits = self.bool_all(args)?;
            let mut conj = Vec::new();
            for w in lits.windows(2) {
                conj.push(self.mk_iff(w[0], w[1]));
            }
            Ok(self.tseitin_and(conj))
        } else {
            let mut terms = Vec::with_capacity(args.len());
            for a in args {
                terms.push(self.term(a)?);
            }
            let mut conj = Vec::new();
            for w in terms.windows(2) {
                conj.push(self.builder.eq_atom(w[0], w[1]));
            }
            Ok(self.tseitin_and(conj))
        }
    }

    /// `(distinct a b ...)`: pairwise `!=`.
    fn distinct(&mut self, args: &[Sexp]) -> Result<Lit, String> {
        let is_bool = self.sort_of(&args[0])? == "Bool";
        let mut conj = Vec::new();
        if is_bool {
            let lits = self.bool_all(args)?;
            for i in 0..lits.len() {
                for j in (i + 1)..lits.len() {
                    // distinct bools: lits[i] xor lits[j]
                    let x = self.mk_xor(lits[i], lits[j]);
                    conj.push(x);
                }
            }
        } else {
            let mut terms = Vec::with_capacity(args.len());
            for a in args {
                terms.push(self.term(a)?);
            }
            for i in 0..terms.len() {
                for j in (i + 1)..terms.len() {
                    let e = self.builder.eq_atom(terms[i], terms[j]);
                    conj.push(!e);
                }
            }
        }
        Ok(self.tseitin_and(conj))
    }

    fn tseitin_and(&mut self, lits: Vec<Lit>) -> Lit {
        if lits.is_empty() {
            return self.true_lit;
        }
        if lits.len() == 1 {
            return lits[0];
        }
        let r = self.builder.fresh_var().pos();
        for &l in &lits {
            self.builder.add_clause(vec![!r, l]); // r -> l
        }
        let mut big = Vec::with_capacity(lits.len() + 1);
        big.push(r);
        for &l in &lits {
            big.push(!l); // (all l) -> r
        }
        self.builder.add_clause(big);
        r
    }

    fn tseitin_or(&mut self, lits: Vec<Lit>) -> Lit {
        if lits.is_empty() {
            return !self.true_lit;
        }
        if lits.len() == 1 {
            return lits[0];
        }
        let r = self.builder.fresh_var().pos();
        for &l in &lits {
            self.builder.add_clause(vec![!l, r]); // l -> r
        }
        let mut big = Vec::with_capacity(lits.len() + 1);
        big.push(!r);
        for &l in &lits {
            big.push(l); // r -> (some l)
        }
        self.builder.add_clause(big);
        r
    }

    fn tseitin_ite(&mut self, c: Lit, t: Lit, e: Lit) -> Lit {
        let r = self.builder.fresh_var().pos();
        self.builder.add_clause(vec![!c, !r, t]);
        self.builder.add_clause(vec![!c, r, !t]);
        self.builder.add_clause(vec![c, !r, e]);
        self.builder.add_clause(vec![c, r, !e]);
        r
    }

    fn mk_xor(&mut self, a: Lit, b: Lit) -> Lit {
        let r = self.builder.fresh_var().pos();
        self.builder.add_clause(vec![!a, !b, !r]);
        self.builder.add_clause(vec![a, b, !r]);
        self.builder.add_clause(vec![a, !b, r]);
        self.builder.add_clause(vec![!a, b, r]);
        r
    }

    fn mk_iff(&mut self, a: Lit, b: Lit) -> Lit {
        !self.mk_xor(a, b)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn answers(input: &str) -> Vec<Answer> {
        Script::run(input)
            .unwrap()
            .results
            .into_iter()
            .map(|r| r.answer)
            .collect()
    }

    #[test]
    fn eq_diamond_like_unsat() {
        let s = "
            (set-logic QF_UF)
            (declare-sort U 0)
            (declare-fun x0 () U) (declare-fun y0 () U) (declare-fun z0 () U)
            (declare-fun x1 () U)
            (assert (and (or (and (= x0 y0) (= y0 x1)) (and (= x0 z0) (= z0 x1)))
                         (not (= x0 x1))))
            (check-sat)";
        assert_eq!(answers(s), vec![Answer::Unsat]);
    }

    #[test]
    fn congruence_and_predicates() {
        // a=b => f(a)=f(b); P over equal args agrees
        let unsat = "
            (set-logic QF_UF)
            (declare-sort U 0)
            (declare-fun a () U) (declare-fun b () U)
            (declare-fun f (U) U)
            (assert (= a b))
            (assert (not (= (f a) (f b))))
            (check-sat)";
        assert_eq!(answers(unsat), vec![Answer::Unsat]);

        let pred = "
            (set-logic QF_UF)
            (declare-sort U 0)
            (declare-fun a () U) (declare-fun b () U)
            (declare-fun P (U) Bool)
            (assert (= a b))
            (assert (P a))
            (assert (not (P b)))
            (check-sat)";
        assert_eq!(answers(pred), vec![Answer::Unsat]);
    }

    #[test]
    fn ite_term_level() {
        // (ite c a b): if c then result=a. With c true and a!=result forced -> interplay
        let s = "
            (set-logic QF_UF)
            (declare-sort U 0)
            (declare-fun a () U) (declare-fun b () U)
            (declare-fun c () Bool)
            (assert c)
            (assert (not (= (ite c a b) a)))
            (check-sat)";
        assert_eq!(answers(s), vec![Answer::Unsat]);

        let sat = "
            (set-logic QF_UF)
            (declare-sort U 0)
            (declare-fun a () U) (declare-fun b () U)
            (declare-fun c () Bool)
            (assert (not (= (ite c a b) a)))
            (check-sat)";
        assert_eq!(answers(sat), vec![Answer::Sat]);
    }

    #[test]
    fn bool_equality_and_distinct() {
        let s = "
            (set-logic QF_UF)
            (declare-fun p () Bool) (declare-fun q () Bool)
            (assert (= p q))
            (assert (distinct p q))
            (check-sat)";
        assert_eq!(answers(s), vec![Answer::Unsat]);
    }

    #[test]
    fn incremental_exec_surfaces_results() {
        // Mirrors how the REPL drives the solver: feed commands one at a time and observe the
        // verdict returned by the (check-sat) command itself.
        let mut s = Script::new();
        for src in [
            "(set-logic QF_UF)",
            "(declare-sort U 0)",
            "(declare-fun a () U)",
            "(declare-fun b () U)",
            "(assert (= a b))",
        ] {
            let cmd = &crate::sexp::parse_script(src).unwrap()[0];
            assert!(s.exec(cmd).unwrap().is_none());
        }
        assert_eq!(s.logic(), Some("QF_UF"));
        let check = &crate::sexp::parse_script("(check-sat)").unwrap()[0];
        assert_eq!(s.exec(check).unwrap().unwrap().answer, Answer::Sat);
        let neg = &crate::sexp::parse_script("(assert (not (= a b)))").unwrap()[0];
        assert!(s.exec(neg).unwrap().is_none());
        assert_eq!(s.exec(check).unwrap().unwrap().answer, Answer::Unsat);
    }

    #[test]
    fn push_pop_scoping() {
        let s = "
            (set-logic QF_UF)
            (declare-sort U 0)
            (declare-fun a () U) (declare-fun b () U)
            (assert (= a b))
            (push 1)
            (assert (not (= a b)))
            (check-sat)
            (pop 1)
            (check-sat)";
        assert_eq!(answers(s), vec![Answer::Unsat, Answer::Sat]);
    }
}
