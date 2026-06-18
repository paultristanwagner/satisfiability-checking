//! Minimal string interner: maps identifiers to dense `u32` [`Symbol`]s for O(1) comparison.
//!
//! Follows the standard Rust interner pattern (cf. matklad's "Fast and Simple Rust Interner"
//! and rustc's `Symbol`). Used by the (later) theory/parser layers to avoid `String`-keyed
//! hot maps; included in `core` so all crates share one symbol type.

use rustc_hash::FxHashMap;

/// An interned string, identified by a dense 0-based index.
#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Debug)]
pub struct Symbol(u32);

impl Symbol {
    #[inline]
    pub fn index(self) -> usize {
        self.0 as usize
    }
}

#[derive(Default)]
pub struct Interner {
    map: FxHashMap<Box<str>, Symbol>,
    strings: Vec<Box<str>>,
}

impl Interner {
    pub fn new() -> Interner {
        Interner::default()
    }

    /// Intern `s`, returning its (stable) [`Symbol`]. Repeated calls with the same string
    /// return the same symbol; comparison is then a `u32` compare.
    pub fn intern(&mut self, s: &str) -> Symbol {
        if let Some(&sym) = self.map.get(s) {
            return sym;
        }
        let sym = Symbol(self.strings.len() as u32);
        let boxed: Box<str> = s.into();
        self.strings.push(boxed.clone());
        self.map.insert(boxed, sym);
        sym
    }

    /// Resolve a symbol back to its string.
    pub fn resolve(&self, sym: Symbol) -> &str {
        &self.strings[sym.index()]
    }

    pub fn len(&self) -> usize {
        self.strings.len()
    }
    pub fn is_empty(&self) -> bool {
        self.strings.is_empty()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn intern_dedups_and_resolves() {
        let mut it = Interner::new();
        let a = it.intern("x");
        let b = it.intern("y");
        let a2 = it.intern("x");
        assert_eq!(a, a2);
        assert_ne!(a, b);
        assert_eq!(it.resolve(a), "x");
        assert_eq!(it.resolve(b), "y");
        assert_eq!(it.len(), 2);
    }
}
