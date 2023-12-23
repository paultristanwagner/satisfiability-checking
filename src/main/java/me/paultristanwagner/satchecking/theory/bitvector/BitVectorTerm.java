package me.paultristanwagner.satchecking.theory.bitvector;

import java.util.Set;

public abstract class BitVectorTerm {

  public boolean isSigned() {
    return false;
  }

  public abstract int getLength();

  public abstract Set<BitVectorVariable> getVariables();
}
