package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;

public class BitVectorLessThanOrEqualConstraint extends BitVectorBinaryConstraint {

  private BitVectorLessThanOrEqualConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorLessThanOrEqualConstraint lessThanOrEqual(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorLessThanOrEqualConstraint(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " <= " + term2 + ")";
  }
}
