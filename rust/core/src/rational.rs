//! Exact arithmetic for the (future) arithmetic theories.
//!
//! [`Rational`] is exact (`num_rational::BigRational`). [`DeltaRational`] is the
//! Dutertre–de Moura infinitesimal representation `value + delta·δ` used to encode strict
//! inequalities in the Simplex core (`x < c` ⇔ `x ≤ c − δ`). Comparison is lexicographic
//! (`value` first, then `delta`) — which the derived `Ord` gives us for free given the field
//! order. Not used by the Milestone-1 SAT core; included per the Phase-1 plan.

use num_rational::BigRational;
use num_traits::Zero;

pub type Rational = BigRational;

#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Debug)]
pub struct DeltaRational {
    pub value: Rational,
    pub delta: Rational,
}

impl DeltaRational {
    pub fn of(value: Rational) -> Self {
        Self {
            value,
            delta: Rational::zero(),
        }
    }
    pub fn new(value: Rational, delta: Rational) -> Self {
        Self { value, delta }
    }
    pub fn zero() -> Self {
        Self::of(Rational::zero())
    }
    pub fn add(&self, o: &Self) -> Self {
        Self {
            value: &self.value + &o.value,
            delta: &self.delta + &o.delta,
        }
    }
    pub fn sub(&self, o: &Self) -> Self {
        Self {
            value: &self.value - &o.value,
            delta: &self.delta - &o.delta,
        }
    }
    /// Multiply both components by a (non-infinitesimal) rational factor.
    pub fn scale(&self, f: &Rational) -> Self {
        Self {
            value: &self.value * f,
            delta: &self.delta * f,
        }
    }
    pub fn neg(&self) -> Self {
        Self {
            value: -&self.value,
            delta: -&self.delta,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use num_traits::One;

    fn r(n: i64, d: i64) -> Rational {
        Rational::new(n.into(), d.into())
    }

    #[test]
    fn arithmetic() {
        let a = DeltaRational::new(r(1, 2), r(1, 1));
        let b = DeltaRational::new(r(1, 3), r(-2, 1));
        assert_eq!(a.add(&b), DeltaRational::new(r(5, 6), r(-1, 1)));
        assert_eq!(a.sub(&b), DeltaRational::new(r(1, 6), r(3, 1)));
        assert_eq!(a.scale(&r(2, 1)), DeltaRational::new(r(1, 1), r(2, 1)));
        assert_eq!(a.neg(), DeltaRational::new(r(-1, 2), r(-1, 1)));
    }

    #[test]
    fn lexicographic_order() {
        let five = Rational::from_integer(5.into());
        // strict `< 5`  -> (5, -1);  strict `> 5` -> (5, +1);  non-strict `5` -> (5, 0)
        let strict_lt = DeltaRational::new(five.clone(), -Rational::one());
        let exact = DeltaRational::of(five.clone());
        let strict_gt = DeltaRational::new(five, Rational::one());
        assert!(strict_lt < exact);
        assert!(exact < strict_gt);
        assert!(strict_lt < strict_gt);
        // value dominates delta
        let big = DeltaRational::new(Rational::from_integer(6.into()), -Rational::one());
        assert!(strict_gt < big);
    }
}
