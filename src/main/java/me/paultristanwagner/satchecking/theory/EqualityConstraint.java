package me.paultristanwagner.satchecking.theory;

import java.util.Objects;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class EqualityConstraint implements Constraint {

  private final String left;
  private final String right;
  private final boolean equal;

  public EqualityConstraint(String left, String right, boolean equal) {
    this.left = left;
    this.right = right;
    this.equal = equal;
  }

  public String getLeft() {
    return left;
  }

  public String getRight() {
    return right;
  }

  public boolean areEqual() {
    return equal;
  }

  @Override
  public boolean isNegatable() {
    return true;
  }

  @Override
  public Constraint negate() {
    return new EqualityConstraint(left, right, !equal);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EqualityConstraint that = (EqualityConstraint) o;
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
}
