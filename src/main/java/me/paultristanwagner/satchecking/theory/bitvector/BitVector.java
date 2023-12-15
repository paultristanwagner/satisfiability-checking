package me.paultristanwagner.satchecking.theory.bitvector;

public class BitVector {

  private final int length;
  private final boolean[] bits;

  public BitVector(int length) {
    this.length = length;
    this.bits = new boolean[length];
  }

  public BitVector(boolean[] bits) {
    this.length = bits.length;
    this.bits = bits;
  }

  public BitVector(int length, int value) {
    this.length = length;
    this.bits = new boolean[length];

    for (int i = 0; i < 32; i++) {
      bits[i] = (value & (1L << i)) != 0;
    }
  }

  public BitVector(long value, int length) {
    this.length = length;
    this.bits = new boolean[length];

    for (int i = 0; i < length; i++) {
      bits[i] = (value & (1L << i)) != 0;
    }
  }

  public int asInt() {
    int value = 0;
    for (int i = 0; i < Math.min(length, 32); i++) {
      if (bits[i]) {
        value |= (1L << i);
      }
    }
    return value;
  }

  public long asLong() {
    long value = 0;
    for (int i = 0; i < Math.min(length, 64); i++) {
      if (bits[i]) {
        value |= (1L << i);
      }
    }
    return value;
  }

  public boolean getBit(int index) {
    return bits[index];
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = bits.length - 1; i >= 0; i--) {
      sb.append(bits[i] ? 1 : 0);
    }

    return sb.toString();
  }
}
