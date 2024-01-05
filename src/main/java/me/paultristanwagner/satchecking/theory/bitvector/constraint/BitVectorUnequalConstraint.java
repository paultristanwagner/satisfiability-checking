package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;

public class BitVectorUnequalConstraint extends BitVectorBinaryConstraint {

  private BitVectorUnequalConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);
  }

  public static BitVectorUnequalConstraint unequal(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorUnequalConstraint(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " != " + term2 + ")";
  }
}
