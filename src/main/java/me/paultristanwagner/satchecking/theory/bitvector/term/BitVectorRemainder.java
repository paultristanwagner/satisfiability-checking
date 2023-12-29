package me.paultristanwagner.satchecking.theory.bitvector.term;

public class BitVectorRemainder extends BitVectorBinaryTerm {

  // extended(l, 2 * len) = (l/r) * extended(r, 2 * len) + extended(l % r, 2 * len)

  private final BitVectorTerm extendedTerm1;
  private final BitVectorTerm extendedTerm2;

  private final BitVectorTerm coefficientTerm;


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
