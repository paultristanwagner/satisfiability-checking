package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable;

import java.util.Set;

public class BitVectorParenthesisTerm extends BitVectorTerm {

  private final BitVectorTerm term;

  private BitVectorParenthesisTerm(BitVectorTerm term) {
    this.term = term;
  }

  public static BitVectorParenthesisTerm parenthesis(BitVectorTerm term) {
    return new BitVectorParenthesisTerm(term);
  }

  @Override
  public String toString() {
    return "(" + term + ")";
  }

  public BitVectorTerm getTerm() {
    return term;
  }

  @Override
  public int getLength() {
    return term.getLength();
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    return term.getVariables();
  }

  @Override
  public Set<BitVectorTerm> getProperSubTerms() {
    return term.getProperSubTerms();
  }
}
