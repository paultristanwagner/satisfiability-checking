package me.paultristanwagner.satchecking.theory.bitvector.term;

import java.util.HashSet;
import java.util.Set;

public class BitVectorNegation extends BitVectorTerm {

  private final BitVectorTerm term;

  private BitVectorNegation(BitVectorTerm term) {
    this.term = term;
  }

  public static BitVectorNegation negation(BitVectorTerm term) {
    return new BitVectorNegation(term);
  }

  public BitVectorTerm getTerm() {
    return term;
  }

  @Override
  public String toString() {
    return "~(" + term + ")";
  }

  @Override
  public int getLength() {
    return term.getLength();
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    return term.getVariables();
  }

  @Override
  public Set<BitVectorTerm> getMaximalProperSubTerms() {
    Set<BitVectorTerm> maximalProperSubTerms = new HashSet<>();
    maximalProperSubTerms.add(term);
    return maximalProperSubTerms;
  }
}
