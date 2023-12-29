package me.paultristanwagner.satchecking.theory.bitvector.term;

import java.util.HashSet;
import java.util.Set;

import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorAddition.addition;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorConstant.constant;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorNegation.negation;

public class BitVectorSubtraction extends BitVectorBinaryTerm {

  private final BitVectorTerm constantTerm;
  private final BitVectorTerm negatedTerm;
  private final BitVectorTerm twoComplementTerm;
  private final BitVectorTerm additionTerm;

  private BitVectorSubtraction(BitVectorTerm term1, BitVectorTerm term2) {
    super(term1, term2);

    constantTerm = constant(1, term2.getLength());
    negatedTerm = negation(term2);
    twoComplementTerm = addition(negatedTerm, constantTerm);
    additionTerm = addition(term1, twoComplementTerm);
  }

  public static BitVectorSubtraction subtraction(BitVectorTerm term1, BitVectorTerm term2) {
    return new BitVectorSubtraction(term1, term2);
  }

  @Override
  public String toString() {
    return "(" + term1 + " - " + term2 + ")";
  }

  @Override
  public Set<BitVectorTerm> getDefiningTerms() {
    Set<BitVectorTerm> definingTerms = new HashSet<>();
    definingTerms.add(constantTerm);
    definingTerms.add(negatedTerm);
    definingTerms.add(twoComplementTerm);
    definingTerms.add(additionTerm);
    return definingTerms;
  }

  public BitVectorTerm getAdditionTerm() {
    return additionTerm;
  }
}
