package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorAddition extends BitVectorBinaryTerm {

  private BitVectorAddition(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorAddition addition(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorAddition(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " + " + term2 + ")";
  }
}
