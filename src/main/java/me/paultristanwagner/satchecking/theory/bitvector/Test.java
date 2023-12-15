package me.paultristanwagner.satchecking.theory.bitvector;

import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorAddition.addition;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorLessThanConstraint.lessThan;

public class Test {

  public static void main(String[] args) {
    BitVectorVariable a = BitVectorVariable.bitvector("a");
    BitVectorVariable b = BitVectorVariable.bitvector("b");
    BitVectorTerm term1 = addition(a, b);

    BitVectorVariable c = BitVectorVariable.bitvector("c");
    BitVectorVariable d = BitVectorVariable.bitvector("d");
    BitVectorTerm term2 = addition(c, d);

    BitVectorConstraint constraint = lessThan(term1, term2);
  }
}
