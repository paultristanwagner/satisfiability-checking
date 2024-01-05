package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorProduct extends BitVectorBinaryTerm {

  private BitVectorProduct(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorProduct product(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorProduct(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " * " + term2 + ")";
  }
}
