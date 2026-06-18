package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;

import java.util.ArrayList;
import java.util.List;

/**
 * A robust reader for the DIMACS CNF format.
 *
 * <p>DIMACS is a whitespace/line oriented format, so this parser tokenizes on whitespace rather
 * than using the generic {@link Lexer} (which would choke on comment lines). The grammar handled
 * here is:
 *
 * <ul>
 *   <li>Lines beginning with {@code c} are comments and are skipped. Blank lines are skipped.
 *   <li>A single header line {@code p cnf <numVars> <numClauses>} (extra whitespace tolerated).
 *   <li>Clauses: sequences of non-zero integers terminated by {@code 0}. A clause may span multiple
 *       lines. A positive id {@code n} maps to {@code new Literal("l" + n)} and a negative id
 *       {@code -n} maps to {@code new Literal("l" + n, true)}.
 *   <li>A trailing {@code %} / {@code 0} end marker after the last clause is tolerated.
 * </ul>
 *
 * <p>The parser is lenient about benign inconsistencies (an out-of-range literal, or a clause count
 * that does not match the header): these produce a {@code Warning: ...} on stderr but parsing
 * continues. A {@link SyntaxError} (always carrying a real message) is only thrown on genuine
 * structural problems: a missing/garbled {@code p cnf} header, a non-integer where an integer is
 * expected, or EOF before a clause's {@code 0} terminator.
 */
public class DimacsParser implements Parser<CNF> {

  @Override
  public ParseResult<CNF> parseWithRemaining(String string) {
    int numVars = -1;
    int numClauses = -1;
    boolean headerSeen = false;

    List<Clause> clauses = new ArrayList<>();
    List<Literal> currentLiterals = new ArrayList<>();
    boolean inClause = false;

    String[] lines = string.split("\n", -1);
    int cursor = 0; // running character offset for SyntaxError positions

    for (String rawLine : lines) {
      int lineStart = cursor;
      cursor += rawLine.length() + 1; // +1 for the consumed '\n'

      String line = rawLine.trim();
      if (line.isEmpty()) {
        continue;
      }

      char first = line.charAt(0);

      // Comment line.
      if (first == 'c') {
        continue;
      }

      // End marker that some files append after the last clause.
      if (first == '%') {
        break;
      }

      // Header line.
      if (first == 'p') {
        if (headerSeen) {
          throw new SyntaxError("Duplicate 'p cnf' header", string, lineStart);
        }

        String[] parts = line.split("\\s+");
        if (parts.length < 4 || !parts[0].equals("p") || !parts[1].equals("cnf")) {
          throw new SyntaxError(
              "Malformed DIMACS header, expected 'p cnf <numVars> <numClauses>'",
              string,
              lineStart);
        }

        numVars = parsePositiveInt(parts[2], "number of variables", string, lineStart);
        numClauses = parsePositiveInt(parts[3], "number of clauses", string, lineStart);
        headerSeen = true;
        continue;
      }

      // Anything that is not a comment/header before the header has been seen is an error.
      if (!headerSeen) {
        throw new SyntaxError(
            "Missing 'p cnf' header before clause data", string, lineStart);
      }

      // Clause data. A clause may span multiple lines, so we accumulate literals until a 0.
      String[] tokens = line.split("\\s+");
      for (String token : tokens) {
        if (token.isEmpty()) {
          continue;
        }

        int value;
        try {
          value = Integer.parseInt(token);
        } catch (NumberFormatException e) {
          throw new SyntaxError(
              "Expected an integer in clause data, got '" + token + "'", string, lineStart);
        }

        if (value == 0) {
          if (!inClause) {
            // A lone 0 with no preceding literals is a trailing end marker some files append
            // after the last clause; ignore it rather than producing a spurious empty clause.
            continue;
          }
          clauses.add(new Clause(currentLiterals));
          currentLiterals = new ArrayList<>();
          inClause = false;
          continue;
        }

        inClause = true;

        int abs = Math.abs(value);
        if (numVars >= 0 && abs > numVars) {
          System.err.printf(
              "Warning: literal %d exceeds declared number of variables (%d)%n", value, numVars);
        }

        if (value > 0) {
          currentLiterals.add(new Literal("l" + value));
        } else {
          currentLiterals.add(new Literal("l" + abs, true));
        }
      }
    }

    if (!headerSeen) {
      throw new SyntaxError("Missing 'p cnf' header", string, 0);
    }

    // A clause was started (literals read) but never terminated with a 0.
    if (inClause) {
      throw new SyntaxError(
          "Unexpected end of input: clause not terminated with '0'", string, string.length());
    }

    if (numClauses >= 0 && clauses.size() != numClauses) {
      System.err.printf(
          "Warning: declared %d clauses but read %d%n", numClauses, clauses.size());
    }

    return new ParseResult<>(new CNF(clauses), string.length(), true);
  }

  private int parsePositiveInt(String token, String what, String input, int index) {
    int value;
    try {
      value = Integer.parseInt(token);
    } catch (NumberFormatException e) {
      throw new SyntaxError(
          "Expected an integer for the " + what + ", got '" + token + "'", input, index);
    }

    if (value < 0) {
      throw new SyntaxError(
          "Expected a non-negative " + what + ", got " + value, input, index);
    }

    return value;
  }
}
