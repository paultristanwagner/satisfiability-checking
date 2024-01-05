package me.paultristanwagner.satchecking.theory.bitvector.term;

import java.util.Set;

public abstract class BitVectorTerm {

  public boolean isSigned() {
    return true;
  }

  public abstract int getLength();

  public abstract Set<BitVectorVariable> getVariables();

  public abstract Set<BitVectorTerm> getMaximalProperSubTerms();
}
