package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorSubtraction extends BitVectorBinaryTerm {

  private BitVectorSubtraction(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorSubtraction subtraction(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorSubtraction(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " - " + term2 + ")";
  }
}
