//! Brute-force SAT oracle: try every assignment. Only for *small* instances (`num_vars` ≤ 24);
//! used purely as a trusted reference in differential tests, mirroring the Java
//! `EnumerationSolver`.

use crate::dimacs::Cnf;

/// Returns `Some(model)` (indexed by variable) if satisfiable, else `None`. Panics if the
/// instance is too large to enumerate.
pub fn solve(cnf: &Cnf) -> Option<Vec<bool>> {
    let n = cnf.num_vars;
    assert!(
        n <= 24,
        "enumerate::solve is only for small instances (n={n})"
    );
    for bits in 0u64..(1u64 << n) {
        let sat = cnf.clauses.iter().all(|clause| {
            clause.iter().any(|l| {
                let v = (bits >> l.var().index()) & 1 == 1;
                if l.is_negated() {
                    !v
                } else {
                    v
                }
            })
        });
        if sat {
            return Some((0..n).map(|v| (bits >> v) & 1 == 1).collect());
        }
    }
    None
}

/// Check whether `model` (indexed by variable) satisfies every clause.
pub fn model_satisfies(cnf: &Cnf, model: &[bool]) -> bool {
    cnf.clauses.iter().all(|clause| {
        clause.iter().any(|l| {
            let v = model[l.var().index()];
            if l.is_negated() {
                !v
            } else {
                v
            }
        })
    })
}
