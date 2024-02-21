package me.paultristanwagner.satchecking.theory.nonlinear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Exponent implements Comparable<Exponent> {

  private List<Integer> values;

  private Exponent(List<Integer> values) {
    this.values = values;
  }

  public static Exponent exponent(List<Integer> values) {
    return new Exponent(values);
  }

  public static Exponent exponent(Integer... values) {
    return new Exponent(Arrays.asList(values));
  }

  public static Exponent constantExponent(int length) {
    Integer[] zeros = new Integer[length];
    Arrays.fill(zeros, 0);
    return exponent(zeros);
  }

  @Override
  public int compareTo(Exponent o) {
    if (this.values.size() != o.values.size()) {
      throw new IllegalStateException("Cannot compare exponents");
    }

    for (int r = values.size() - 1; r >= 0; r--) {
      int comparison = Integer.compare(this.values.get(r), o.values.get(r));
      if (comparison != 0) {
        return comparison;
      }
    }

    return 0;
  }

  public Exponent add(Exponent other) {
    if (this.values.size() != other.values.size()) {
      throw new IllegalArgumentException("Exponent size does not match");
    }

    List<Integer> values = new ArrayList<>();
    for (int i = 0; i < this.values.size(); i++) {
      values.add(this.get(i) + other.get(i));
    }

    return exponent(values);
  }

  public Exponent subtract(Exponent other) {
    if (this.values.size() != other.values.size()) {
      throw new IllegalArgumentException("Exponent size does not match");
    }

    List<Integer> values = new ArrayList<>();
    for (int i = 0; i < this.values.size(); i++) {
      int result = this.get(i) - other.get(i);
      if (result < 0) {
        throw new IllegalArgumentException("Cannot subtract exponent if result would be negative");
      }

      values.add(result);
    }

    return exponent(values);
  }

  public boolean divides(Exponent other) {
    if (this.values.size() != other.values.size()) {
      throw new IllegalArgumentException("Exponent size does not match");
    }

    for (int i = 0; i < this.values.size(); i++) {
      if (this.get(i) > other.get(i)) {
        return false;
      }
    }

    return true;
  }

  public int get(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Cannot get negative exponent index");
    } else if (index >= values.size()) {
      throw new IllegalArgumentException("Invalid exponent index");
    }

    return values.get(index);
  }

  public int highestNonZeroIndex() {
    for (int r = values.size() - 1; r >= 0; r--) {
      if (values.get(r) > 0) {
        return r;
      }
    }

    return -1;
  }

  public boolean isConstantExponent() {
    for (Integer exponent : values) {
      if (exponent != 0) {
        return false;
      }
    }

    return true;
  }

  public static Exponent project(
      Exponent from, List<String> originVariables, List<String> targetVariables) {
    Integer[] newExponentsArray = new Integer[targetVariables.size()];
    Arrays.fill(newExponentsArray, 0);
    for (int i = 0; i < from.values.size(); i++) {
      String variable = originVariables.get(i);
      int variableIndex = targetVariables.indexOf(variable); // todo: inefficient

      if (variableIndex == -1) {
        continue;
      }

      int exponent = from.get(i);

      newExponentsArray[variableIndex] = exponent;
    }

    return exponent(newExponentsArray);
  }

  public List<Integer> getValues() {
    return values;
  }

  @Override
  public String toString() {
    return values.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Exponent exponent = (Exponent) o;
    return Objects.equals(values, exponent.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(values);
  }
}
