package me.paultristanwagner.satchecking.theory.arithmetic;

import me.paultristanwagner.satchecking.Config;

public interface Number {

  static Number ZERO() {
    if (Config.get().useFloats()) {
      return new Float(0);
    } else {
      return new Rational(0);
    }
  }

  static Number ONE() {
    if (Config.get().useFloats()) {
      return new Float(1);
    } else {
      return new Rational(1);
    }
  }

  static Number parse(String string) {
    if (Config.get().useFloats()) {
      return Float.parse(string);
    } else {
      return Rational.parse(string);
    }
  }

  static Number number(long numerator, long denominator) {
    if (Config.get().useFloats()) {
      return new Float((double) numerator / denominator);
    } else {
      return new Rational(numerator, denominator);
    }
  }

  static Number number(long numerator) {
    return number(numerator, 1);
  }

  Number add(Number other);

  Number subtract(Number other);

  Number multiply(Number other);

  Number pow(int exponent);

  Number divide(Number other);

  Number midpoint(Number other);

  Number mediant(Number other);

  Number negate();

  Number abs();

  boolean isZero();

  boolean isOne();

  boolean isInteger();

  Number ceil();

  Number floor();

  boolean lessThan(Number other);

  boolean lessThanOrEqual(Number other);

  default boolean greaterThan(Number other) {
    return other.lessThan(this);
  }

  default boolean greaterThanOrEqual(Number other) {
    return other.lessThanOrEqual(this);
  }

  default boolean isNonZero() {
    return !isZero();
  }

  boolean isPositive();

  default boolean isNegative() {
    return isNonZero() && !isPositive();
  }

  default boolean isNonNegative() {
    return isZero() || isPositive();
  }

  default int sign() {
    if (isZero()) {
      return 0;
    } else if (isPositive()) {
      return 1;
    } else {
      return -1;
    }
  }

  float approximateAsFloat();

  double approximateAsDouble();
}
