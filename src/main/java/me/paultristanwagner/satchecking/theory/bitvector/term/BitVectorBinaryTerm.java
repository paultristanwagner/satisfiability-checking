package me.paultristanwagner.satchecking.theory.bitvector.term;

import java.util.HashSet;
import java.util.Set;

public abstract class BitVectorBinaryTerm extends BitVectorTerm {

  protected final BitVectorTerm term1;
  protected final BitVectorTerm term2;

  protected BitVectorBinaryTerm(BitVectorTerm term1, BitVectorTerm term2) {
    if (term1.isSigned() != term2.isSigned()) {
      throw new IllegalArgumentException(
          "BitVectorBinaryTerm: term1 and term2 must have the same signedness!");
    } else if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException(
          "BitVectorBinaryTerm: term1 and term2 must have the same length!");
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
  public Set<BitVectorTerm> getMaximalProperSubTerms() {
    Set<BitVectorTerm> maximalProperSubTerms = new HashSet<>();
    maximalProperSubTerms.add(term1);
    maximalProperSubTerms.add(term2);
    return maximalProperSubTerms;
  }
}
