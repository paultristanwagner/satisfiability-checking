package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorLeftShift extends BitVectorBinaryTerm {

  private BitVectorLeftShift(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorLeftShift leftShift(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorLeftShift(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " << " + term2 + ")";
  }
}
