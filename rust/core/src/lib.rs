//! Shared primitives for the solver: literals, three-valued logic, string interning, and
//! exact arithmetic. Deliberately allocation-light and `Copy`-friendly.

pub mod interner;
pub mod lit;
pub mod rational;

pub use interner::{Interner, Symbol};
pub use lit::{Lbool, Lit, Var};
pub use rational::{DeltaRational, Rational};
