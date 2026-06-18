# satcheck-rs — full SMT solver implementation roadmap

## Where we are
**Milestone 1 (done, PR #59):** a from-scratch CDCL SAT core (VSIDS, restarts, LBD reduction,
1-UIP, watched literals), a robust DIMACS reader, a brute-force enumerator oracle, and a CLI —
all index-based and data-oriented. Verified: 20k random CNFs vs the enumerator (0 mismatches),
80 DIMACS three-way vs Z3 4.16 + the Java solver (0 disagreements), and **matches Z3 within ~1 ms
while running 7–90× faster than the Java solver** on phase-transition 3-SAT. The 3-oracle
differential spine (enumerator / Z3 / Java) is in place and reused by every later phase.

## Target
A sound, performant SMT solver for **QF_UF, QF_EQ, QF_LRA, QF_LIA, QF_BV, QF_NRA** with an
SMT-LIB 2.6 front-end, continuously validated against Z3 and the Java solver.

## Cross-cutting architecture (the keystones)

1. **Hash-consed term DAG** (new `term` crate): `Sort {Bool, Int, Real, BitVec(u32), Uninterp(Symbol)}`,
   `Term`/`NodeId`, a hash-cons table, interned symbols/functions. Unique terms ⇒ `id == id`
   equality and structural sharing. Shared by the parser and every theory. (Idiomatically retires
   the Java "identity-keyed maps / `equals` that mutates" smells.)

2. **One `Theory` trait — incremental and propagating** (collapses the Java `FullLazy`/`LessLazy`
   split into a single DPLL(T) loop):
   ```rust
   trait Theory {
       fn assert(&mut self, lit: Lit);                 // an atom literal was assigned
       fn check(&mut self, complete: bool) -> Result<(), Vec<Lit>>; // Err = conflict clause (¬lits)
       fn propagate(&mut self) -> Vec<Lit>;            // theory-implied literals
       fn explain(&mut self, lit: Lit) -> Vec<Lit>;    // lazy reason clause for a propagated lit
       fn push(&mut self);  fn pop(&mut self, n: usize);  // align with decision levels
   }
   ```

3. **DPLL(T) loop**: extend the M1 `Solver` to be theory-aware. After BCP fixpoint →
   `theory.propagate()` (enqueue implied lits with a lazy theory reason) → `theory.check()`; on a
   theory conflict, learn the explanation as a watched clause (generalizing M1's
   `learn_theory_lemma`); `push`/`pop` track decision levels; SAT = all atoms assigned + theory
   check clean. Theory propagation + good explanations are what make eq_diamond-class problems fast.

4. **Boolean abstraction**: SMT-LIB `assert`s are Tseitin-encoded into clauses over **atom
   literals** (each theory atom ↔ a `Lit`; aux Tseitin vars are pure boolean lits the theory
   ignores). This is the one place the boolean structure meets the theory.

5. **Theory combination (Nelson–Oppen)** for logics sharing equalities (e.g. QF_UFLIA) — deferred;
   single-theory first.

## Milestones (each shippable, differential-gated vs Z3 + Java)

- **M2 — DPLL(T) + EUF + equality-fragment front-end** *(the keystone; first end-to-end SMT)*.
  `Theory` trait + DPLL(T) integration; EUF over the hash-consed DAG (congruence closure via
  union-find + signature/use-list table, proof-forest explanations, `push`/`pop`); SMT-LIB parser
  for QF_UF/QF_EQ (set-logic, declares, `define-fun`, `assert` with full boolean structure →
  Tseitin, check-sat, push/pop, `:status`). **Gate:** 0 mismatches vs Z3 + Java on QF_UF/QF_EQ;
  **eq_diamond solved fast** (showing DPLL(T) ≫ the Java FullLazy enumeration). *Spec:*
  `theory/solver/EqualityFunctionSolver.java`, `smt/solver/*`, `parse/SmtLibParser.java`.

- **M3 — LRA + LIA.** General Simplex (Dutertre–de Moura) over `DeltaRational` (bound propagation,
  infeasibility-row explanations, incremental push/pop); LIA via branch-and-bound on the LRA
  relaxation. Parser extended to linear arithmetic atoms (`=` split into `≤ ∧ ≥`). **Gate:** 0
  mismatches vs Z3 + Java on QF_LRA/QF_LIA. *Spec:* `SimplexFeasibilitySolver.java`,
  `LinearIntegerSolver.java`, `theory/arithmetic/DeltaRational.java`.

- **M4 — BV (eager bit-blasting).** Term-level BV ops → CNF over the M1 SAT core, with **correct
  SMT-LIB semantics from the start**: `extract`/`concat`/`ite`, signed+unsigned comparison, shifts
  incl. `bvashr`, `bvmul`/`bvudiv`/`bvurem`/`bvsdiv`/`bvsrem` (div-by-zero per SMT-LIB),
  overflow/wraparound, multi-width. Parser QF_BV. **Gate:** 0 mismatches vs Z3 on QF_BV (Z3 is the
  oracle; the Java BV theory is too narrow). This fixes the Java BV gaps (#15/#16/#17/#48) by design.

- **M5 — NRA (CAD).** Hash-consed multivariate polynomials + exact arithmetic; real-root isolation
  (Sturm), real-algebraic numbers, CAD projection/lifting. The long pole; will trail Z3 on hard
  instances. **Gate:** 0 mismatches vs Z3 where it terminates, timeout-bounded. *Spec:*
  `theory/nonlinear/*`. (Consider NLSAT/MCSAT later for competitiveness.)

- **M6 — combination + polish.** Nelson–Oppen combination (QF_UFLIA, …); incremental solving across
  `check-sat`s with clause reuse; SMT-LIB-compliant headless I/O (`get-model`/`get-value`/
  `get-unsat-core`, exit codes, `:timeout`); rustyline REPL; **nightly Z3-differential CI** on a
  Zenodo benchmark subset; criterion perf tracking.

## Workspace evolution
`core` · `term` (new: hash-consed DAG) · `sat` · `theory` (the trait + `euf`/`lra`/`lia`/`bv`/`nra`
modules) · `smt` (DPLL(T) + boolean abstraction + combination) · `parse` · `cli` · `fuzz`.

## Verification (continuous, per milestone)
Unit + `proptest` + `criterion` + the **3-oracle differential** (enumerator for SAT; **Z3** + the
**Java solver** for SMT) with **UNSAT-only spurious-SAT gates**; a nightly Z3-differential on real
SMT-LIB benchmarks. Standing assets: the Z3 4.16 binary, the `/tmp/bench` corpus, and the Java
`smtlib`/`dimacs` oracle commands.

## Risks & sequencing
- **M2 is the keystone** — the theory propagation / explanation / backtrack contract must be right;
  everything else builds on it. EUF explanations should be near-minimal (proof forest) for speed.
- **M3** — Simplex incrementality + `DeltaRational` conflict explanations are subtle.
- **M4** — get BV semantics exactly right (the Java gaps are the cautionary tale); eager blasting
  keeps it tractable.
- **M5/NRA** is the long pole and the least competitive; isolate and timeout-bound it.
- **N-O combination** only when a multi-theory logic is actually targeted.
