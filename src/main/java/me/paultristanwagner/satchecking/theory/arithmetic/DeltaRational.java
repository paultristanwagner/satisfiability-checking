package me.paultristanwagner.satchecking.theory.arithmetic;

import java.util.Objects;

/**
 * Represents a value of the form {@code c + k*delta} where {@code delta} is a positive
 * infinitesimal (Dutertre / de Moura). The {@code rational} field is the concrete part {@code c}
 * and the {@code delta} field is the coefficient {@code k} of the infinitesimal.
 *
 * <p>Comparison is lexicographic: first compare the rational parts, breaking ties on the delta
 * coefficient. This realizes the ordering over the totally ordered field of "delta-rationals" and
 * matches the standard encoding used to handle strict inequalities in a Simplex-based LRA solver.
 *
 * <p>Instances are immutable.
 */
public final class DeltaRational {

  private final Number rational;
  private final Number delta;

  public static final DeltaRational ZERO = new DeltaRational(Number.ZERO(), Number.ZERO());

  private DeltaRational(Number rational, Number delta) {
    this.rational = rational;
    this.delta = delta;
  }

  public static DeltaRational of(Number rational) {
    return new DeltaRational(rational, Number.ZERO());
  }

  public static DeltaRational of(Number rational, Number delta) {
    return new DeltaRational(rational, delta);
  }

  public Number getRational() {
    return rational;
  }

  public Number getDelta() {
    return delta;
  }

  public DeltaRational add(DeltaRational other) {
    return new DeltaRational(rational.add(other.rational), delta.add(other.delta));
  }

  public DeltaRational subtract(DeltaRational other) {
    return new DeltaRational(rational.subtract(other.rational), delta.subtract(other.delta));
  }

  /** Multiplies both the rational and the delta part by {@code factor}. */
  public DeltaRational scale(Number factor) {
    return new DeltaRational(rational.multiply(factor), delta.multiply(factor));
  }

  public DeltaRational negate() {
    return new DeltaRational(rational.negate(), delta.negate());
  }

  public boolean lessThan(DeltaRational other) {
    if (rational.lessThan(other.rational)) {
      return true;
    }
    if (rational.greaterThan(other.rational)) {
      return false;
    }
    return delta.lessThan(other.delta);
  }

  public boolean greaterThan(DeltaRational other) {
    return other.lessThan(this);
  }

  public boolean lessThanOrEqual(DeltaRational other) {
    return !other.lessThan(this);
  }

  public boolean greaterThanOrEqual(DeltaRational other) {
    return !this.lessThan(other);
  }

  public boolean equalsValue(DeltaRational other) {
    return rational.equals(other.rational) && delta.equals(other.delta);
  }

  public boolean isZero() {
    return rational.isZero() && delta.isZero();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DeltaRational that)) return false;
    return rational.equals(that.rational) && delta.equals(that.delta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rational, delta);
  }

  @Override
  public String toString() {
    if (delta.isZero()) {
      return rational.toString();
    }
    return "(" + rational + " + " + delta + "*delta)";
  }
}
