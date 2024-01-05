package me.paultristanwagner.satchecking.theory.arithmetic;

import java.math.BigInteger;
import java.util.Objects;

import static java.math.BigInteger.TEN;

public class Rational implements Number {

  protected BigInteger numerator;
  protected BigInteger denominator;

  public Rational(BigInteger numerator, BigInteger denominator) {
    if(denominator.equals(BigInteger.ZERO)){
      throw new ArithmeticException("Cannot divide by zero");
    }

    this.numerator = numerator;
    this.denominator = denominator;

    if(denominator.compareTo(BigInteger.ZERO) < 0){
      this.numerator = this.numerator.negate();
      this.denominator = this.denominator.negate();
    }

    reduce();
  }

  public Rational(long numerator, long denominator) {
    this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  public Rational(BigInteger numerator) {
    this(numerator, BigInteger.ONE);
  }

  public Rational(long numerator) {
    this(numerator, 1);
  }

  public static Rational ZERO() {
    return new Rational(0);
  }

  public static Rational ONE() {
    return new Rational(1);
  }

  @Override
  public Rational add(Number other) {
    if (!(other instanceof Rational otherExact)) {
      throw new IllegalArgumentException("Cannot add " + other + " to " + this);
    }

    BigInteger num =
        numerator.multiply(otherExact.denominator).add(otherExact.numerator.multiply(denominator));
    BigInteger den = denominator.multiply(otherExact.denominator);

    return new Rational(num, den);
  }

  @Override
  public Rational subtract(Number other) {
    return add(other.negate());
  }

  @Override
  public Rational multiply(Number other) {
    if (!(other instanceof Rational otherExact)) {
      throw new IllegalArgumentException("Cannot add " + other + " to " + this);
    }

    BigInteger num = numerator.multiply(otherExact.numerator);
    BigInteger den = denominator.multiply(otherExact.denominator);

    return new Rational(num, den);
  }

  @Override
  public Rational divide(Number other) {
    if (!(other instanceof Rational otherExact)) {
      throw new IllegalArgumentException("Cannot add " + other + " to " + this);
    }

    if (otherExact.denominator.equals(BigInteger.ZERO)) {
      throw new ArithmeticException("Cannot divide by zero");
    }

    BigInteger num = numerator.multiply(otherExact.denominator);
    BigInteger den = denominator.multiply(otherExact.numerator);

    return new Rational(num, den);
  }

  @Override
  public Rational negate() {
    return new Rational(numerator.negate(), denominator);
  }

  @Override
  public Number abs() {
    return new Rational(numerator.abs(), denominator.abs());
  }

  public boolean isZero() {
    return numerator.equals(BigInteger.ZERO);
  }

  @Override
  public boolean isOne() {
    return numerator.equals(denominator);
  }

  @Override
  public boolean isInteger() {
    return denominator.equals(BigInteger.ONE);
  }

  @Override
  public Number ceil() {
    if(isInteger()) {
      return new Rational(numerator, denominator);
    }

    BigInteger div = numerator.divide(denominator);

    if(isNegative()) {
      return new Rational(div);
    }

    return new Rational(div.add(BigInteger.ONE));
  }

  @Override
  public Number floor() {
    if(isInteger()) {
      return new Rational(numerator, denominator);
    }

    BigInteger div = numerator.divide(denominator);

    if(isNegative()) {
      return new Rational(div.subtract(BigInteger.ONE));
    }

    return new Rational(div);
  }

  @Override
  public boolean lessThan(Number other) {
    if(!(other instanceof Rational otherExact)){
      throw new IllegalArgumentException("Cannot compare " + other + " to " + this);
    }

    return numerator.multiply(otherExact.denominator).compareTo(otherExact.numerator.multiply(denominator)) < 0;
  }

  @Override
  public boolean lessThanOrEqual(Number other) {
    return this.equals(other) || lessThan(other);
  }

  @Override
  public boolean isNonZero() {
    return !isZero();
  }

  @Override
  public boolean isPositive() {
    return numerator.compareTo(BigInteger.ZERO) == denominator.compareTo(BigInteger.ZERO);
  }

  @Override
  public boolean isNegative() {
    return isNonZero() && !isPositive();
  }

  @Override
  public boolean isNonNegative() {
    return !isNegative();
  }

  private void reduce() {
    BigInteger gcd = numerator.gcd(denominator);
    numerator = numerator.divide(gcd);
    denominator = denominator.divide(gcd);
  }

  @Override
  public String toString() {
    if(isInteger()){
      return numerator.toString();
    }

    return numerator + "/" + denominator;
  }

  public static Rational parse(String value) {
    if(value.contains(".")){
      return parseDecimal(value);
    }

    return parseRational(value);
  }

  private static Rational parseDecimal(String value) {
    String[] parts = value.split("\\.");

    String integerPart = parts[0];
    String fractionalPart = parts.length > 1 ? parts[1] : "";

    int fractionalDigits = fractionalPart.length();

    BigInteger numerator = new BigInteger(integerPart + fractionalPart);
    BigInteger denominator = TEN.pow(fractionalDigits);

    return new Rational(numerator, denominator);
  }

  private static Rational parseRational(String value) {
    String[] parts = value.split("/");

    String numerator = parts[0];
    String denominator = parts.length > 1 ? parts[1] : "1";

    return new Rational(new BigInteger(numerator), new BigInteger(denominator));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Rational that = (Rational) o;

    return numerator.multiply(that.denominator).equals(that.numerator.multiply(denominator));
  }

  @Override
  public int hashCode() {
    return Objects.hash(numerator, denominator);
  }
}
