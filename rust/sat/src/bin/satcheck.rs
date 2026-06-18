//! Minimal DIMACS CLI: `satcheck <file.cnf>`. Prints `s SATISFIABLE` / `s UNSATISFIABLE`
//! and uses the SAT-competition exit codes (10 = SAT, 20 = UNSAT, 2 = error).

use std::process::ExitCode;

fn main() -> ExitCode {
    let path = match std::env::args().nth(1) {
        Some(p) => p,
        None => {
            eprintln!("usage: satcheck <file.cnf>");
            return ExitCode::from(2);
        }
    };
    let input = match std::fs::read_to_string(&path) {
        Ok(s) => s,
        Err(e) => {
            eprintln!("read error: {e}");
            return ExitCode::from(2);
        }
    };
    let cnf = match satcheck_sat::dimacs::parse(&input) {
        Ok(c) => c,
        Err(e) => {
            eprintln!("parse error: {e}");
            return ExitCode::from(2);
        }
    };
    match satcheck_sat::solve_cnf(&cnf) {
        satcheck_sat::SolveResult::Sat(_) => {
            println!("s SATISFIABLE");
            ExitCode::from(10)
        }
        satcheck_sat::SolveResult::Unsat => {
            println!("s UNSATISFIABLE");
            ExitCode::from(20)
        }
    }
}
