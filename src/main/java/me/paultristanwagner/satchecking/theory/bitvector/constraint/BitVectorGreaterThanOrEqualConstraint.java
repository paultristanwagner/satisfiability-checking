package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;

public class BitVectorGreaterThanOrEqualConstraint extends BitVectorBinaryConstraint {

  private BitVectorGreaterThanOrEqualConstraint(BitVectorTerm left, BitVectorTerm right) {
    super(left, right);
  }

  public static BitVectorGreaterThanOrEqualConstraint greaterThanOrEqual(BitVectorTerm left, BitVectorTerm right) {
    return new BitVectorGreaterThanOrEqualConstraint(left, right);
  }

  @Override
  public String toString() {
    return "(" + term1 + " >= " + term2 + ")";
  }
}
