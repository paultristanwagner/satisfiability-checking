//! CDCL SAT solver (Milestone 1).
//!
//! - [`cdcl::Solver`] — the CDCL solver (watched literals, VSIDS, 1-UIP, restarts, reduction).
//! - [`dimacs`] — DIMACS CNF reader.
//! - [`enumerate`] — brute-force oracle for differential testing.

pub mod cdcl;
pub mod dimacs;
pub mod enumerate;
mod heap;

pub use cdcl::{SolveResult, Solver};
pub use dimacs::Cnf;

/// Convenience: build a solver from a parsed CNF and solve it.
pub fn solve_cnf(cnf: &Cnf) -> SolveResult {
    let mut s = Solver::new();
    s.ensure_vars(cnf.num_vars);
    for clause in &cnf.clauses {
        if !s.add_clause(clause) {
            // formula became trivially unsat while adding
        }
    }
    s.solve()
}

#[cfg(test)]
mod tests {
    use super::*;
    use satcheck_core::Lit;

    fn lits(ds: &[i32]) -> Vec<Lit> {
        ds.iter().map(|&d| Lit::from_dimacs(d)).collect()
    }

    #[test]
    fn trivial_sat() {
        let mut s = Solver::new();
        s.ensure_vars(2);
        assert!(s.add_clause(&lits(&[1, 2])));
        assert!(s.add_clause(&lits(&[-1, 2])));
        match s.solve() {
            SolveResult::Sat(m) => assert!(m[1]), // var 2 (index 1) must be true
            SolveResult::Unsat => panic!("should be SAT"),
        }
    }

    #[test]
    fn trivial_unsat() {
        let mut s = Solver::new();
        s.ensure_vars(1);
        s.add_clause(&lits(&[1]));
        s.add_clause(&lits(&[-1]));
        assert_eq!(s.solve(), SolveResult::Unsat);
    }

    #[test]
    fn implication_chain_sat() {
        // (a) & (~a|b) & (~b|c) & (~c|d) -> SAT with all true
        let cnf = dimacs::parse("p cnf 4 4\n1 0\n-1 2 0\n-2 3 0\n-3 4 0\n").unwrap();
        match solve_cnf(&cnf) {
            SolveResult::Sat(m) => assert_eq!(m, vec![true, true, true, true]),
            SolveResult::Unsat => panic!("should be SAT"),
        }
    }

    #[test]
    fn pigeonhole_2_into_1_unsat() {
        // 2 pigeons, 1 hole: p1,p2 must each be in hole, but hole holds one -> unsat
        // vars: x1 = pigeon1 in hole, x2 = pigeon2 in hole; (x1)&(x2)&(~x1|~x2)
        let cnf = dimacs::parse("p cnf 2 3\n1 0\n2 0\n-1 -2 0\n").unwrap();
        assert_eq!(solve_cnf(&cnf), SolveResult::Unsat);
    }
}
