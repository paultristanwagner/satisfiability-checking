package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorXor extends BitVectorBinaryTerm {

  private BitVectorXor(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorXor bitwiseXor(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorXor(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " ^ " + term2 + ")";
  }
}
