package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorRightShift extends BitVectorBinaryTerm {

  private BitVectorRightShift(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorRightShift rightShift(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorRightShift(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " >> " + term2 + ")";
  }
}
