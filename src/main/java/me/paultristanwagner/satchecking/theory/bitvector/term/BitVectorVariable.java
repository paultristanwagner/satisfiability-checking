package me.paultristanwagner.satchecking.theory.bitvector.term;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BitVectorVariable extends BitVectorTerm {

  private final int length;
  private final String name;

  private BitVectorVariable(int length, String name) {
    this.length = length;
    this.name = name;
  }

  public static BitVectorVariable bitvector(String name) {
    return bitvector(name, 32);
  }

  public static BitVectorVariable bitvector(String name, int length) {
    return new BitVectorVariable(length, name);
  }

  public String getName() {
    return name;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public Set<BitVectorVariable> getVariables() {
    return new HashSet<>(Set.of(this));
  }

  @Override
  public Set<BitVectorTerm> getProperSubTerms() {
    return new HashSet<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BitVectorVariable that = (BitVectorVariable) o;
    return length == that.length && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(length, name);
  }

  @Override
  public String toString() {
    return name;
  }
}
