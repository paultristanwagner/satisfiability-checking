package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;

public class BitVectorLessThanConstraint extends BitVectorBinaryConstraint {

  private BitVectorLessThanConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorLessThanConstraint lessThan(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorLessThanConstraint(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " < " + term2 + ")";
  }
}
