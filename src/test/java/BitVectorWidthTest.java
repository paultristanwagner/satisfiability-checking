import me.paultristanwagner.satchecking.theory.bitvector.BitVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitVectorWidthTest {

  // Regression for #16: BitVector(int, length) used to fill only min(8, 32) bits, truncating any
  // value wider than 8 bits regardless of the requested length.
  @Test
  public void valuesWiderThanEightBitsAreNotTruncated() {
    assertEquals(300, new BitVector(300, 16).asInt(), "300 must fit in 16 bits");
    assertEquals(1000, new BitVector(1000, 16).asInt());
    assertEquals(70000, new BitVector(70000, 32).asInt());
  }

  @Test
  public void smallValuesStillWork() {
    assertEquals(5, new BitVector(5, 16).asInt());
    assertEquals(255, new BitVector(255, 8).asInt());
  }

  @Test
  public void intAndLongConstructorsAgree() {
    for (int v : new int[] {0, 1, 7, 200, 300, 65535, 70000}) {
      assertEquals(
          new BitVector((long) v, 32).asInt(),
          new BitVector(v, 32).asInt(),
          "int and long constructors must agree for value " + v);
    }
  }
}
