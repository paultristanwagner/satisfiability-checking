package me.paultristanwagner.satchecking.theory;

import java.util.*;

public class EqualityFunctionConstraint implements Constraint {

  private final Function left;
  private final Function right;
  private final boolean equal;

  public EqualityFunctionConstraint(Function left, Function right, boolean equal) {
    this.left = left;
    this.right = right;
    this.equal = equal;
  }

  public Function getLeft() {
    return left;
  }

  public Function getRight() {
    return right;
  }

  public boolean areEqual() {
    return equal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EqualityFunctionConstraint that = (EqualityFunctionConstraint) o;
    return equal == that.equal
        && Objects.equals(left, that.left)
        && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right, equal);
  }

  @Override
  public String toString() {
    if (equal) {
      return left + "=" + right;
    } else {
      return left + "!=" + right;
    }
  }

  public record Function(String name, List<Function> parameters) {

    public static Function of(String name, List<Function> parameters) {
      return new Function(name, parameters);
    }

    public static Function of(String name, Function... parameters) {
      return new Function(name, Arrays.asList(parameters));
    }

    public Set<Function> getAllSubTerms() {
      Set<Function> subTerms = new HashSet<>();
      subTerms.add(this);
      for (Function parameter : this.parameters) {
        subTerms.addAll(parameter.getAllSubTerms());
      }

      return subTerms;
    }

    @Override
    public String toString() {
      if (parameters.isEmpty()) {
        return name;
      }

      String parameterString =
          String.join(", ", parameters.stream().map(Function::toString).toArray(String[]::new));

      return name + "(" + parameterString + ")";
    }
  }
}
