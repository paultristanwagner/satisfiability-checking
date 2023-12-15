package me.paultristanwagner.satchecking.theory.bitvector;

import java.util.Set;

public class BitVectorProduct extends BitVectorTerm {

  private final BitVectorTerm term1;
  private final BitVectorTerm term2;

  private BitVectorProduct(BitVectorTerm term1, BitVectorTerm term2) {
    if(term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorMultiplication: term1 and term2 must have the same length!");
    }

    this.term1 = term1;
    this.term2 = term2;
  }

  public static BitVectorProduct product(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorProduct(term1, term2);
  }

  public BitVectorTerm getTerm1() {
    return term1;
  }

  public BitVectorTerm getTerm2() {
    return term2;
  }

  @Override
  public int getLength() {
    return term1.getLength();
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    Set<BitVectorVariable> variables = term1.getVariables();
    variables.addAll(term2.getVariables());
    return variables;
  }

  @Override
  public String toString() {
    return "(" + term1 + " * " + term2 + ")";
  }
}
