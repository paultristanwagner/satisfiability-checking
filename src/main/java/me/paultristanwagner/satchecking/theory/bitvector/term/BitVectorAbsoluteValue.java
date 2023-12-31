package me.paultristanwagner.satchecking.theory.bitvector.term;

import java.util.Set;

public class BitVectorAbsoluteValue extends BitVectorTerm {

  private final BitVectorTerm term;

  private BitVectorAbsoluteValue(BitVectorTerm term) {
    if(!term.isSigned()) {
      throw new IllegalArgumentException("Cannot take absolute value of unsigned term!");
    }

    this.term = term;
  }

  public static BitVectorAbsoluteValue absoluteValue(BitVectorTerm term) {
    return new BitVectorAbsoluteValue(term);
  }

  public BitVectorTerm getTerm() {
    return term;
  }

  @Override
  public boolean isSigned() {
    return false;
  }

  @Override
  public int getLength() {
    return term.getLength();
  }

  @Override
  public String toString() {
    return "|" + term + "|";
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    return term.getVariables();
  }

  @Override
  public Set<BitVectorTerm> getProperSubTerms() {
    Set<BitVectorTerm> subTerms = term.getProperSubTerms();
    subTerms.add(term);
    return subTerms;
  }
}
