package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;

public class BitVectorEqualConstraint extends BitVectorBinaryConstraint {

  private BitVectorEqualConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorEqualConstraint equal(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorEqualConstraint(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " = " + term2 + ")";
  }
}
