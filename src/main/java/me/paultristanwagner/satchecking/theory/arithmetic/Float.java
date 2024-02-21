package me.paultristanwagner.satchecking.theory.arithmetic;

import java.math.BigInteger;
import java.util.Objects;

public class Float implements Number {

  protected final double value;

  public Float(double value) {
    this.value = value;
  }

  public static Float ZERO() {
    return new Float(0);
  }

  public static Float ONE() {
    return new Float(1);
  }

  @Override
  public Float add(Number other) {
    if (!(other instanceof Float otherFloat)) {
      throw new IllegalArgumentException("Cannot add " + other + " to " + this);
    }

    return new Float(value + otherFloat.value);
  }

  @Override
  public Float subtract(Number other) {
    return add(other.negate());
  }

  @Override
  public Float multiply(Number other) {
    if (!(other instanceof Float otherFloat)) {
      throw new IllegalArgumentException("Cannot add " + other + " to " + this);
    }

    return new Float(value * otherFloat.value);
  }

  @Override
  public Float pow(int exponent) {
    return new Float(Math.pow(value, exponent));
  }

  @Override
  public Float divide(Number other) {
    if (!(other instanceof Float otherFloat)) {
      throw new IllegalArgumentException("Cannot add " + other + " to " + this);
    }

    return new Float(value / otherFloat.value);
  }

  @Override
  public Float midpoint(Number other) {
    if (!(other instanceof Float otherFloat)) {
      throw new IllegalArgumentException("Cannot add " + other + " to " + this);
    }

    return new Float((value + otherFloat.value) / 2);
  }

  @Override
  public Number mediant(Number other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Float negate() {
    return new Float(-value);
  }

  @Override
  public Float abs() {
    return new Float(Math.abs(value));
  }

  @Override
  public boolean isZero() {
    return value == 0;
  }

  @Override
  public boolean isOne() {
    return value == 1;
  }

  @Override
  public boolean isInteger() {
    return value == Math.floor(value);
  }

  @Override
  public Number ceil() {
    return new Float(Math.ceil(value));
  }

  @Override
  public Number floor() {
    return new Float(Math.floor(value));
  }

  @Override
  public Number gcd(Number other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Number lcm(Number other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigInteger getNumerator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigInteger getDenominator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean lessThan(Number other) {
    if (!(other instanceof Float otherFloat)) {
      throw new IllegalArgumentException("Cannot compare " + other + " to " + this);
    }

    return value < otherFloat.value;
  }

  @Override
  public boolean lessThanOrEqual(Number other) {
    return this.equals(other) || lessThan(other);
  }

  @Override
  public boolean isPositive() {
    return value > 0;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Float aFloat = (Float) o;
    return Double.compare(value, aFloat.value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  public static Float parse(String string) {
    if(string.contains("/")) {
      return parseFraction(string);
    }

    return new Float(Double.parseDouble(string));
  }

  private static Float parseFraction(String string) {
    String[] parts = string.split("/");

    String numerator = parts[0];
    String denominator = parts.length > 1 ? parts[1] : "1";

    return new Float(Double.parseDouble(numerator) / Double.parseDouble(denominator));
  }

  @Override
  public float approximateAsFloat() {
    return (float) value;
  }

  @Override
  public double approximateAsDouble() {
    return value;
  }
}
