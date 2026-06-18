package me.paultristanwagner.satchecking.theory;

public interface Constraint {

  /**
   * Whether this constraint has an exact boolean negation expressible as another {@link Constraint}.
   * Defaults to {@code false}; theories whose atom negation is exact (e.g. equality logics) override
   * this to {@code true}.
   */
  default boolean isNegatable() {
    return false;
  }

  /**
   * Returns the constraint representing the boolean negation of this atom. Only valid when
   * {@link #isNegatable()} returns {@code true}.
   */
  default Constraint negate() {
    throw new UnsupportedOperationException();
  }
}
