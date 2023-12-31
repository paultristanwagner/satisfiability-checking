package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorRemainder extends BitVectorBinaryTerm {

  private BitVectorRemainder(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorRemainder remainder(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorRemainder(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " % " + term2 + ")";
  }
}
