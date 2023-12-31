package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable;

import java.util.Set;

public class BitVectorBitConstraint extends BitVectorConstraint {

  private final BitVectorTerm term;

  private final int bit;

  private BitVectorBitConstraint(BitVectorTerm term, int bit) {
    this.term = term;
    bit %= term.getLength();
    if (bit < 0) {
      bit += term.getLength();
    }
    this.bit = bit;
  }

  public static BitVectorBitConstraint bitSet(BitVectorTerm term, int bit) {
    return new BitVectorBitConstraint(term, bit);
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    return term.getVariables();
  }

  public BitVectorTerm getTerm() {
    return term;
  }

  public int getBit() {
    return bit;
  }

  @Override
  public Set<BitVectorTerm> getTerms() {
    Set<BitVectorTerm> terms = term.getProperSubTerms();
    terms.add(term);
    return terms;
  }

  @Override
  public String toString() {
    return "(" + term + "[" + bit + "])";
  }
}
