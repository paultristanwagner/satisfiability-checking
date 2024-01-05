package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorAnd extends BitVectorBinaryTerm {

  private BitVectorAnd(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorAnd bitwiseAnd(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorAnd(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " & " + term2 + ")";
  }
}
