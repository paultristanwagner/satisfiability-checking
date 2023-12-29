package me.paultristanwagner.satchecking.theory.bitvector.term;

import java.util.Set;

public abstract class BitVectorTerm {

  public boolean isSigned() {
    return false;
  }

  public abstract int getLength();

  public abstract Set<BitVectorVariable> getVariables();

  public Set<BitVectorTerm> getDefiningTerms() {
    return Set.of();
  }

  public abstract Set<BitVectorTerm> getProperSubTerms();
}
