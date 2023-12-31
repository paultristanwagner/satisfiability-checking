package me.paultristanwagner.satchecking.theory.bitvector.term;

import me.paultristanwagner.satchecking.theory.bitvector.BitVector;

import java.util.HashSet;
import java.util.Set;

public class BitVectorConstant extends BitVectorTerm {

  private final int length;
  private final BitVector bitVector;

  private BitVectorConstant(BitVector bitVector) {
    this.length = bitVector.getLength();
    this.bitVector = bitVector;
  }

  public static BitVectorConstant constant(long value, int length) {
    return constant(new BitVector(value, length));
  }

  public static BitVectorConstant constant(BitVector bitVector) {
    return new BitVectorConstant(bitVector);
  }

  public BitVector getBitVector() {
    return bitVector;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    return new HashSet<>();
  }

  @Override
  public Set<BitVectorTerm> getProperSubTerms() {
    return new HashSet<>();
  }

  @Override
  public String toString() {
    return bitVector.toString();
  }
}
