//! The SMT layer: wires the CDCL solver to a theory (DPLL(T)) and owns the atom↔variable map.
//!
//! Milestone 2: equality logic (EUF). [`SmtBuilder`] lets a front-end build terms, mint equality
//! atoms (each a SAT variable), add clauses over those atoms (the boolean abstraction), and solve.

use rustc_hash::FxHashMap;
use satcheck_core::{Lit, Var};
use satcheck_sat::{SolveResult, Solver};
use satcheck_term::TermId;
use satcheck_theory::Euf;

pub struct SmtBuilder {
    solver: Solver<Euf>,
    /// Equality atom `(a,b)` (order-normalized) -> its SAT variable.
    eq_vars: FxHashMap<(TermId, TermId), Var>,
    next_var: usize,
    clauses: Vec<Vec<Lit>>,
}

impl Default for SmtBuilder {
    fn default() -> Self {
        SmtBuilder::new()
    }
}

impl SmtBuilder {
    pub fn new() -> SmtBuilder {
        SmtBuilder {
            solver: Solver::with_theory(Euf::new()),
            eq_vars: FxHashMap::default(),
            next_var: 0,
            clauses: Vec::new(),
        }
    }

    /// Access the EUF theory to build terms (`mk_const` / `mk_app`).
    pub fn euf(&mut self) -> &mut Euf {
        self.solver.theory_mut()
    }

    /// A fresh propositional variable (for Tseitin auxiliaries and `Bool`-sorted symbols).
    pub fn fresh_var(&mut self) -> Var {
        let v = Var::from_index(self.next_var);
        self.next_var += 1;
        v
    }

    /// The positive literal of the equality atom `a = b` (deduplicated, order-insensitive).
    pub fn eq_atom(&mut self, a: TermId, b: TermId) -> Lit {
        let key = if a <= b { (a, b) } else { (b, a) };
        if let Some(&v) = self.eq_vars.get(&key) {
            return v.pos();
        }
        let v = self.fresh_var();
        self.eq_vars.insert(key, v);
        self.solver.theory_mut().register_eq_atom(v, key.0, key.1);
        v.pos()
    }

    /// Add a clause to the boolean abstraction.
    pub fn add_clause(&mut self, lits: Vec<Lit>) {
        self.clauses.push(lits);
    }

    /// Solve. Returns true for SAT, false for UNSAT.
    pub fn solve(&mut self) -> bool {
        self.solver.ensure_vars(self.next_var);
        for c in &self.clauses {
            self.solver.add_clause(c);
        }
        matches!(self.solver.solve(), SolveResult::Sat(_))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn direct_congruence_unsat() {
        // a = b  &  f(a) != f(b)  -> UNSAT
        let mut s = SmtBuilder::new();
        let a = s.euf().mk_const("a");
        let b = s.euf().mk_const("b");
        let fa = s.euf().mk_app("f", vec![a]);
        let fb = s.euf().mk_app("f", vec![b]);
        let e_ab = s.eq_atom(a, b);
        let e_ff = s.eq_atom(fa, fb);
        s.add_clause(vec![e_ab]); // a = b
        s.add_clause(vec![!e_ff]); // f(a) != f(b)
        assert!(!s.solve(), "a=b & f(a)!=f(b) must be UNSAT");
    }

    #[test]
    fn transitive_congruence_unsat() {
        // a=b, c=d, b=d, f(a)!=f(c) -> UNSAT (the case the Java solver originally got wrong)
        let mut s = SmtBuilder::new();
        let a = s.euf().mk_const("a");
        let b = s.euf().mk_const("b");
        let c = s.euf().mk_const("c");
        let d = s.euf().mk_const("d");
        let fa = s.euf().mk_app("f", vec![a]);
        let fc = s.euf().mk_app("f", vec![c]);
        let e_ab = s.eq_atom(a, b);
        let e_cd = s.eq_atom(c, d);
        let e_bd = s.eq_atom(b, d);
        let e_ff = s.eq_atom(fa, fc);
        s.add_clause(vec![e_ab]);
        s.add_clause(vec![e_cd]);
        s.add_clause(vec![e_bd]);
        s.add_clause(vec![!e_ff]);
        assert!(!s.solve(), "transitive congruence must be UNSAT");
    }

    #[test]
    fn congruence_sat_without_link() {
        // a=b, c=d, f(a)!=f(c)  (no b=d) -> SAT
        let mut s = SmtBuilder::new();
        let a = s.euf().mk_const("a");
        let b = s.euf().mk_const("b");
        let c = s.euf().mk_const("c");
        let d = s.euf().mk_const("d");
        let fa = s.euf().mk_app("f", vec![a]);
        let fc = s.euf().mk_app("f", vec![c]);
        let e_ab = s.eq_atom(a, b);
        let e_cd = s.eq_atom(c, d);
        let e_ff = s.eq_atom(fa, fc);
        s.add_clause(vec![e_ab]);
        s.add_clause(vec![e_cd]);
        s.add_clause(vec![!e_ff]);
        assert!(s.solve(), "a=b & c=d & f(a)!=f(c) must be SAT");
    }

    #[test]
    fn boolean_structure_unsat() {
        // (a=b | a=c) & a!=b & a!=c  -> UNSAT (exercises the SAT/theory loop on a disjunction)
        let mut s = SmtBuilder::new();
        let a = s.euf().mk_const("a");
        let b = s.euf().mk_const("b");
        let c = s.euf().mk_const("c");
        let e_ab = s.eq_atom(a, b);
        let e_ac = s.eq_atom(a, c);
        s.add_clause(vec![e_ab, e_ac]); // a=b OR a=c
        s.add_clause(vec![!e_ab]); // a!=b
        s.add_clause(vec![!e_ac]); // a!=c
        assert!(!s.solve());
    }

    #[test]
    fn boolean_structure_sat() {
        // (a=b | a=c) & a!=b  -> SAT (pick a=c)
        let mut s = SmtBuilder::new();
        let a = s.euf().mk_const("a");
        let b = s.euf().mk_const("b");
        let c = s.euf().mk_const("c");
        let e_ab = s.eq_atom(a, b);
        let e_ac = s.eq_atom(a, c);
        s.add_clause(vec![e_ab, e_ac]);
        s.add_clause(vec![!e_ab]);
        assert!(s.solve());
    }
}
