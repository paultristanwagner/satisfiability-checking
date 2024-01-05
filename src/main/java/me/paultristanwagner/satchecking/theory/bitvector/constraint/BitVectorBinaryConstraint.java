package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable;

import java.util.HashSet;
import java.util.Set;

public abstract class BitVectorBinaryConstraint extends BitVectorConstraint {

  protected final BitVectorTerm term1;
  protected final BitVectorTerm term2;

  protected BitVectorBinaryConstraint(BitVectorTerm term1, BitVectorTerm term2) {
    if(term1.isSigned() != term2.isSigned()) {
      throw new IllegalArgumentException("BitVectorTerms must have the same signedness!");
    } else if(term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorTerms must have the same length!");
    }

    this.term1 = term1;
    this.term2 = term2;
  }

  public BitVectorTerm getTerm1() {
    return term1;
  }

  public BitVectorTerm getTerm2() {
    return term2;
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    Set<BitVectorVariable> variables = term1.getVariables();
    variables.addAll(term2.getVariables());
    return variables;
  }

  @Override
  public Set<BitVectorTerm> getMaximalProperSubTerms() {
    Set<BitVectorTerm> terms = new HashSet<>();
    terms.add(term1);
    terms.add(term2);
    return terms;
  }
}
