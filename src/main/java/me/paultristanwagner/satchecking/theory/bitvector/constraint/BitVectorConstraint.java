package me.paultristanwagner.satchecking.theory.bitvector.constraint;

import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable;

import java.util.Set;

public abstract class BitVectorConstraint implements Constraint {

  public abstract Set<BitVectorVariable> getVariables();

  public abstract Set<BitVectorTerm> getMaximalProperSubTerms();
}
