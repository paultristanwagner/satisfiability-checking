package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable;

import java.util.HashSet;
import java.util.Set;

public abstract class BitVectorBinaryConstraint extends BitVectorConstraint {

  protected final BitVectorTerm term1;
  protected final BitVectorTerm term2;

  protected BitVectorBinaryConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    if (term1.isSigned() != term2.isSigned()) {
      throw new IllegalArgumentException("BitVectorTerms must have the same signedness!");
    } else if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorTerms must have the same length!");
    }

    this.term1 = term1;
    this.term2 = term2;
  }

  public BitVectorTerm getTerm1() {
    return term1;
  }

  public BitVectorTerm getTerm2() {
    return term2;
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    Set<BitVectorVariable> variables = term1.getVariables();
    variables.addAll(term2.getVariables());
    return variables;
  }

  @Override
  public Set<BitVectorTerm> getMaximalProperSubTerms() {
    Set<BitVectorTerm> terms = new HashSet<>();
    terms.add(term1);
    terms.add(term2);
    return terms;
  }

  @Override
  public boolean isNegatable() {
    return true;
  }

  /**
   * Value-based equality keyed on the concrete constraint class together with a STABLE structural
   * key of the two operand terms ({@link #termKey(BitVectorTerm)}). This makes the atom BiMap in the
   * SMT-LIB front-end deduplicate identical atoms and lets {@code negate().negate()} round-trip to an
   * object {@code equals} to the original.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BitVectorBinaryConstraint that = (BitVectorBinaryConstraint) o;
    return termKey(term1).equals(termKey(that.term1)) && termKey(term2).equals(termKey(that.term2));
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getClass(), termKey(term1), termKey(term2));
  }

  /**
   * A stable textual/structural key for a bit-vector term. BV terms have no uniform value-based
   * {@code equals}/{@code hashCode}, so we key on {@code toString()} (which is structural and stable
   * for the term classes used here) together with the declared length and signedness so that two
   * terms with the same printed form but different widths/signedness do not collide.
   */
  protected static String termKey(BitVectorTerm term) {
    return term.getLength() + (term.isSigned() ? "s" : "u") + ":" + term;
  }
}
