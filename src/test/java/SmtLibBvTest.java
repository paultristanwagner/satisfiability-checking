import me.paultristanwagner.satchecking.command.impl.SmtLibCommand;
import me.paultristanwagner.satchecking.command.impl.SmtLibCommand.Verdict;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the QF_BV (fixed-size bit-vector) SMT-LIB front-end. Covers the fragment the
 * bit-blasting theory can express (8-bit arithmetic, signed comparisons, bitwise ops, boolean
 * structure) and verifies that operations the theory cannot model SOUNDLY are REJECTED with a clear
 * "unsupported" {@link SyntaxError} rather than mis-encoded.
 *
 * <p>NOTE on signedness (issue #15): the bit-vector comparison constraints are SIGNED. The supported
 * comparison atoms are therefore the signed ones (bvsle/bvslt/bvsge/bvsgt); the unsigned comparisons
 * (bvule/bvult/bvuge/bvugt) are intentionally rejected as unsupported.
 */
public class SmtLibBvTest {

  private static Verdict run(String script) {
    return SmtLibCommand.runScript(script, false);
  }

  @Test
  public void testBvAddSatisfiable() {
    // x + 1 == 3  =>  x = 2 (8-bit).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (= (bvadd x #x01) #x03))(check-sat)"));
  }

  @Test
  public void testBvAddContradictionUnsat() {
    // x + 1 == 3 (so x = 2) AND x == 5  => unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (= (bvadd x #x01) #x03))(assert (= x #x05))(check-sat)"));
  }

  @Test
  public void testSignedComparisonSqueezeUnsat() {
    // x <= 5 AND x >= 5 AND x != 5  => unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (bvsle x #x05))(assert (bvsge x #x05))(assert (distinct x #x05))(check-sat)"));
  }

  @Test
  public void testBooleanStructureSatisfiable() {
    // (x = 2 OR x = 7) AND 1 <= x <= 9 (signed)  => sat (x = 2 or 7).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (or (= x #x02) (= x #x07)))"
                + "(assert (bvsle x #x09))(assert (bvsge x #x01))(check-sat)"));
  }

  @Test
  public void testDisjointSignedIntervalsUnsat() {
    // (x <= 0 OR x >= 10) AND 1 <= x <= 9 is EMPTY under SIGNED comparison  => unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (or (bvsle x #x00) (bvsge x #x0a)))"
                + "(assert (bvsle x #x09))(assert (bvsge x #x01))(check-sat)"));
  }

  @Test
  public void testBitwiseAndSat() {
    // (x & 0x0f) = 0x05  is satisfiable (e.g. x = 0x05).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (= (bvand x #x0f) #x05))(check-sat)"));
  }

  @Test
  public void testBvNegSat() {
    // bvneg x == 0xfd (i.e. -3) AND x == 0x03  => sat (two's complement of 3 is 0xfd).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (= (bvneg x) #xfd))(assert (= x #x03))(check-sat)"));
  }

  @Test
  public void testBinaryAndDecimalLiteralsAgree() {
    // #b00000010, #x02 and (_ bv2 8) all denote the 8-bit value 2.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                + "(assert (= x #b00000010))(assert (= x #x02))(assert (= x (_ bv2 8)))(check-sat)"));
  }

  @Test
  public void testUnsignedComparisonRejected() {
    SyntaxError e =
        assertThrows(
            SyntaxError.class,
            () ->
                run(
                    "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                        + "(assert (bvult x #x05))(check-sat)"));
    assertTrue(e.getMessage().contains("unsupported"), e.getMessage());
  }

  @Test
  public void testArithmeticShiftRejected() {
    SyntaxError e =
        assertThrows(
            SyntaxError.class,
            () ->
                run(
                    "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                        + "(assert (= (bvashr x #x01) #x00))(check-sat)"));
    assertTrue(e.getMessage().contains("unsupported"), e.getMessage());
  }

  @Test
  public void testExtractRejected() {
    SyntaxError e =
        assertThrows(
            SyntaxError.class,
            () ->
                run(
                    "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                        + "(assert (= ((_ extract 3 0) x) #x0))(check-sat)"));
    assertTrue(e.getMessage().contains("unsupported"), e.getMessage());
  }

  @Test
  public void testWidthMismatchRejected() {
    // x is 8-bit, the literal #x0001 is 16-bit; no implicit extension is supported.
    SyntaxError e =
        assertThrows(
            SyntaxError.class,
            () ->
                run(
                    "(set-logic QF_BV)(declare-const x (_ BitVec 8))"
                        + "(assert (= x #x0001))(check-sat)"));
    assertTrue(e.getMessage().contains("width"), e.getMessage());
  }
}
