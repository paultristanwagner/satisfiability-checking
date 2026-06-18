//! Headless SMT-LIB front-end for QF_UF/QF_EQ: reads a script from a file (or stdin) and prints
//! one `sat`/`unsat` line per `(check-sat)`.
//!
//! Usage: `satcheck-smt <file.smt2>` or `satcheck-smt < file.smt2`.

use std::io::Read;
use std::process::ExitCode;

use satcheck_smt::script::Script;

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().collect();
    let input = match args.get(1) {
        Some(path) if path != "-" => match std::fs::read_to_string(path) {
            Ok(s) => s,
            Err(e) => {
                eprintln!("error: cannot read {path}: {e}");
                return ExitCode::FAILURE;
            }
        },
        _ => {
            let mut s = String::new();
            if let Err(e) = std::io::stdin().read_to_string(&mut s) {
                eprintln!("error: cannot read stdin: {e}");
                return ExitCode::FAILURE;
            }
            s
        }
    };

    match Script::run(&input) {
        Ok(script) => {
            for r in &script.results {
                println!("{}", r.answer.as_str());
            }
            ExitCode::SUCCESS
        }
        Err(e) => {
            eprintln!("error: {e}");
            ExitCode::FAILURE
        }
    }
}
