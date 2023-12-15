package me.paultristanwagner.satchecking.theory.bitvector;

import java.util.Set;

public class BitVectorLessThanConstraint extends BitVectorConstraint {

  private final BitVectorTerm term1;
  private final BitVectorTerm term2;

  private BitVectorLessThanConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    this.term1 = term1;
    this.term2 = term2;
  }

  public static BitVectorLessThanConstraint lessThan(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorLessThanConstraint(term1, term2);
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
  public String toString() {
    return "(" + term1 + " < " + term2 + ")";
  }
}
