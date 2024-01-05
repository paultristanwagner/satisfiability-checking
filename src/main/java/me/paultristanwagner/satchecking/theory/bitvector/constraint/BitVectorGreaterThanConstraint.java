package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;

public class BitVectorGreaterThanConstraint extends BitVectorBinaryConstraint {

  private BitVectorGreaterThanConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorGreaterThanConstraint greaterThan(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorGreaterThanConstraint(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " > " + term2 + ")";
  }
}
