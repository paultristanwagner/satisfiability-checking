# satcheck-rs — a from-scratch, performance-first SAT/SMT solver in Rust

A fresh, idiomatic Rust reimplementation of this project's solver, built data-oriented and
performance-first. The Java solver in the parent repo is the *algorithm spec*; Z3 and a
brute-force enumerator are *oracles*. See the design plan for the full rationale and roadmap.

## Status — Milestone 1 (Phases 0–2): an excellent CDCL SAT core ✅

- **`core`** — `Var`/`Lit` packed in a `u32`, `Lbool`, a `Symbol` string interner, exact
  `Rational` + `DeltaRational` (for the future arithmetic theories).
- **`sat`** — a CDCL solver with **two-watched literals**, **VSIDS** (indexed binary heap),
  **1-UIP** conflict analysis with non-chronological backjumping, **Luby restarts**, **LBD-based
  clause-DB reduction**, and phase saving. Index-based throughout (clauses in a `Vec` addressed
  by `ClauseRef`; per-variable struct-of-arrays; watch lists indexed by `Lit::index()`). Plus a
  robust DIMACS reader, a brute-force enumerator oracle, and a `satcheck <file.cnf>` CLI.

### Verification (all green)

- **20,000 random CNFs** differential-tested vs the brute-force enumerator: **0 verdict
  mismatches, 0 invalid models** (`cargo test -p satcheck-sat --test differential`).
- **80 random DIMACS**, three-way vs **Z3 4.16** and the **Java solver**: **0 disagreements**.
- **Performance** (phase-transition random 3-SAT, wall-clock): matches Z3 within ~1 ms and beats
  the Java solver by **7–90×** (widening with size) — e.g. at n=130: Rust 11 ms, Z3 10 ms,
  Java 1028 ms.

## Build & test

```sh
cd rust
cargo test --all            # unit + the 20k-CNF differential gate
cargo build --release       # optimized build (lto, codegen-units=1)
./target/release/satcheck path/to/problem.cnf   # prints s SATISFIABLE / s UNSATISFIABLE
```

## Roadmap (later milestones)

Phase 3: a DPLL(T) framework (one incremental, propagating `Theory` trait). Phase 4: theories
(EUF → LRA/LIA → BV → NRA). Phase 5: SMT-LIB 2.6 front-end + headless I/O. Each phase
differential-gated against Z3 and the Java solver.
