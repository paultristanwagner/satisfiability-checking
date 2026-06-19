//! SMT-LIB front-end for QF_UF/QF_EQ.
//!
//! Modes:
//! - `satcheck-smt <file.smt2>` — run a script file, print one `sat`/`unsat` per `(check-sat)`.
//! - `satcheck-smt -` or piped stdin — same, reading the script from stdin.
//! - `satcheck-smt` on a terminal — an interactive REPL (line editing + history) that keeps the
//!   declaration/assertion state across commands and prints each verdict as it is entered.

use std::io::{IsTerminal, Read};
use std::process::ExitCode;

use rustyline::error::ReadlineError;
use rustyline::DefaultEditor;
use satcheck_smt::script::Script;
use satcheck_smt::sexp::{parse_script, Sexp};

fn main() -> ExitCode {
    let arg = std::env::args().nth(1);
    match arg.as_deref() {
        Some("-") => batch(read_stdin()),
        Some(path) => match std::fs::read_to_string(path) {
            Ok(s) => batch(s),
            Err(e) => {
                eprintln!("error: cannot read {path}: {e}");
                ExitCode::FAILURE
            }
        },
        None => {
            if std::io::stdin().is_terminal() {
                repl()
            } else {
                batch(read_stdin())
            }
        }
    }
}

fn read_stdin() -> String {
    let mut s = String::new();
    let _ = std::io::stdin().read_to_string(&mut s);
    s
}

/// Non-interactive: parse and run the whole script, print a verdict per `(check-sat)`.
fn batch(input: String) -> ExitCode {
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

fn repl() -> ExitCode {
    let mut rl = match DefaultEditor::new() {
        Ok(rl) => rl,
        Err(e) => {
            eprintln!("error: cannot start REPL: {e}");
            return ExitCode::FAILURE;
        }
    };
    let history = history_path();
    if let Some(h) = &history {
        let _ = rl.load_history(h);
    }

    println!("satcheck-smt — interactive QF_UF/QF_EQ solver");
    println!("Enter SMT-LIB commands; `(check-sat)` prints a verdict. Ctrl-D or `(exit)` to quit.");

    let mut script = Script::new();
    let mut buf = String::new();
    loop {
        let prompt = if buf.trim().is_empty() {
            format!("{}> ", script.logic().unwrap_or("smt"))
        } else {
            "   ...> ".to_string()
        };
        match rl.readline(&prompt) {
            Ok(line) => {
                if !line.trim().is_empty() {
                    let _ = rl.add_history_entry(line.as_str());
                }
                buf.push_str(&line);
                buf.push('\n');
                // Execute every complete top-level form; keep any incomplete tail buffered.
                let n = complete_len(&buf);
                if n == 0 {
                    continue;
                }
                let ready = buf[..n].to_string();
                buf.drain(..n);
                if run_forms(&ready, &mut script) {
                    break;
                }
            }
            Err(ReadlineError::Interrupted) => {
                // Ctrl-C: abandon the half-typed form.
                buf.clear();
            }
            Err(ReadlineError::Eof) => break,
            Err(e) => {
                eprintln!("error: {e}");
                break;
            }
        }
    }

    if let Some(h) = &history {
        let _ = rl.save_history(h);
    }
    ExitCode::SUCCESS
}

/// Parse and run the complete forms in `src`; print verdicts. Returns true on `(exit)`.
fn run_forms(src: &str, script: &mut Script) -> bool {
    let cmds = match parse_script(src) {
        Ok(c) => c,
        Err(e) => {
            eprintln!("error: {e}");
            return false;
        }
    };
    for cmd in &cmds {
        let head = cmd
            .as_list()
            .and_then(|l| l.first())
            .and_then(Sexp::as_atom);
        if head == Some("exit") {
            return true;
        }
        match script.exec(cmd) {
            Ok(Some(r)) => {
                let mut out = r.answer.as_str().to_string();
                if let Some(exp) = r.expected {
                    if exp != r.answer {
                        out.push_str(&format!("   (!! :status said {})", exp.as_str()));
                    }
                }
                println!("{out}");
            }
            Ok(None) => {}
            Err(e) => eprintln!("error: {e}"),
        }
    }
    false
}

/// Byte length of the longest prefix of `buf` made up of complete top-level s-expressions (paren
/// depth back to 0), respecting `;` comments, `|quoted|` symbols, and `"strings"`.
fn complete_len(buf: &str) -> usize {
    let b = buf.as_bytes();
    let mut i = 0;
    let mut depth = 0i32;
    let mut cut = 0;
    while i < b.len() {
        match b[i] {
            b';' => {
                while i < b.len() && b[i] != b'\n' {
                    i += 1;
                }
            }
            b'|' => {
                i += 1;
                while i < b.len() && b[i] != b'|' {
                    i += 1;
                }
                if i < b.len() {
                    i += 1; // closing '|'
                }
            }
            b'"' => {
                i += 1;
                while i < b.len() {
                    if b[i] == b'"' {
                        if i + 1 < b.len() && b[i + 1] == b'"' {
                            i += 2; // escaped quote
                            continue;
                        }
                        i += 1; // closing quote
                        break;
                    }
                    i += 1;
                }
            }
            b'(' => {
                depth += 1;
                i += 1;
            }
            b')' => {
                i += 1;
                depth -= 1;
                if depth <= 0 {
                    depth = 0;
                    cut = i;
                }
            }
            _ => i += 1,
        }
    }
    cut
}

fn history_path() -> Option<std::path::PathBuf> {
    std::env::var_os("HOME").map(|h| std::path::PathBuf::from(h).join(".satcheck_smt_history"))
}
