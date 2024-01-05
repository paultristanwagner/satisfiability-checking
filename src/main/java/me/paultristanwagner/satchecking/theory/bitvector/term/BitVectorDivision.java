package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorDivision extends BitVectorBinaryTerm {

  private BitVectorDivision(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorDivision division(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorDivision(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " / " + term2 + ")";
  }
}
