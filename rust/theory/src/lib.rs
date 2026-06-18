//! Theory solvers implementing [`satcheck_core::Theory`].
//!
//! Milestone 2: [`euf::Euf`] — equality with uninterpreted functions (congruence closure) for
//! QF_UF / QF_EQ.

pub mod euf;

pub use euf::Euf;
