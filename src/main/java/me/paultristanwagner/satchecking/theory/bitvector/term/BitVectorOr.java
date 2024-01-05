package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorOr extends BitVectorBinaryTerm {

  private BitVectorOr(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorOr bitwiseOr(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorOr(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " | " + term2 + ")";
  }
}
