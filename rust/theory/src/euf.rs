//! EUF — equality with uninterpreted functions, via congruence closure.
//!
//! Public API contract (implemented as proof-producing incremental congruence closure):
//! - `mk_const`/`mk_app` build terms *and* register them in the e-graph;
//! - `register_eq_atom(var, a, b)` ties a SAT variable to the equality atom `a = b`;
//! - the [`Theory`] impl drives assert/check/propagate/explain/push/pop.
//!
//! NOTE: this is a compiling stub defining the API; the congruence-closure body is filled in
//! (the `Theory` methods here are intentionally trivial until then).

use satcheck_core::{Lit, Theory, Var};
use satcheck_term::{TermArena, TermId};

pub struct Euf {
    arena: TermArena,
}

impl Default for Euf {
    fn default() -> Self {
        Euf::new()
    }
}

impl Euf {
    pub fn new() -> Euf {
        Euf {
            arena: TermArena::new(),
        }
    }

    /// Build (and register) a 0-ary constant.
    pub fn mk_const(&mut self, name: &str) -> TermId {
        self.arena.constant(name)
    }

    /// Build (and register) the application `name(args)`.
    pub fn mk_app(&mut self, name: &str, args: Vec<TermId>) -> TermId {
        self.arena.func(name, args)
    }

    /// Tie SAT variable `var` to the equality atom `a = b`.
    pub fn register_eq_atom(&mut self, _var: Var, _a: TermId, _b: TermId) {}

    pub fn num_terms(&self) -> usize {
        self.arena.num_terms()
    }
}

impl Theory for Euf {
    fn assert(&mut self, _lit: Lit) {}
    fn check(&mut self, _complete: bool) -> Result<(), Vec<Lit>> {
        Ok(())
    }
    fn propagate(&mut self) -> Vec<Lit> {
        Vec::new()
    }
    fn explain(&mut self, _lit: Lit) -> Vec<Lit> {
        Vec::new()
    }
    fn push(&mut self) {}
    fn pop(&mut self, _levels: usize) {}
}
