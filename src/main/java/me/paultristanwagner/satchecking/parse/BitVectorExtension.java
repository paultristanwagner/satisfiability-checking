package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable;

import java.util.Set;

public class BitVectorExtension extends BitVectorTerm {

  private final BitVectorTerm term;
  private final int length;

  private BitVectorExtension(BitVectorTerm term, int length) {
    if (term.getLength() >= length) {
      throw new IllegalArgumentException("BitVectorExtension: length must be greater than the length of the term!");
    }

    this.term = term;
    this.length = length;
  }

  public static BitVectorExtension extend(BitVectorTerm term, int length) {
    return new BitVectorExtension(term, length);
  }

  @Override
  public int getLength() {
    return length;
  }

  public BitVectorTerm getTerm() {
    return term;
  }

  @Override
  public String toString() {
    return "extend(" + term + ", " + length + ")";
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    return term.getVariables();
  }

  @Override
  public Set<BitVectorTerm> getMaximalProperSubTerms() {
    Set<BitVectorTerm> subTerms = term.getMaximalProperSubTerms();
    subTerms.add(term);
    return subTerms;
  }
}
