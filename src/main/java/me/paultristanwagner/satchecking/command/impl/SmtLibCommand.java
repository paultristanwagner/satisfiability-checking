package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.AnsiColor;
import me.paultristanwagner.satchecking.command.Command;
import me.paultristanwagner.satchecking.parse.SmtLibParser;
import me.paultristanwagner.satchecking.parse.SmtLibParser.SmtLibScript;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.TheoryClause;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.Theory;
import me.paultristanwagner.satchecking.theory.solver.TheorySolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static me.paultristanwagner.satchecking.AnsiColor.*;

/**
 * Command that parses an SMT-LIB 2.6 subset script (inline or from a file), runs it through the
 * existing SMT pipeline and prints sat/unsat/unknown for each {@code (check-sat)}. When the script
 * carries {@code (set-info :status ...)} the verdict is compared against it.
 *
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 */
public class SmtLibCommand extends Command {

  public SmtLibCommand() {
    super(
        "smtlib",
        List.of(),
        "Parses and solves an SMT-LIB 2.6 subset script",
        "smtlib <file | inline script>",
        """
              Parses a subset of SMT-LIB 2.6 and runs it through the SMT pipeline.

              Supported logics: QF_LRA, QF_LIA, QF_EQ, QF_UF, QF_EQUF.
              Boolean structure:
                - equality logics (QF_EQ, QF_UF, QF_EQUF): arbitrary structure
                  (and/or/not/=>/xor/ite over equality atoms, incl. negation/distinct).
                - arithmetic logics (QF_LRA, QF_LIA): CNF fragment of positive atoms only.

              Examples:
                smtlib problem.smt2
                smtlib (set-logic QF_LRA)(declare-const x Real)(assert (<= x 5))(check-sat)
            """);
  }

  @Override
  public boolean execute(String label, String[] args) {
    if (args.length < 1) {
      return false;
    }

    String joined = String.join(" ", args).trim();

    String script;
    Path path = pathOrNull(joined);
    if (path != null) {
      try {
        script = Files.readString(path);
      } catch (IOException e) {
        System.out.printf("%sCould not read file '%s': %s%s%n", RED, joined, e.getMessage(), RESET);
        return true;
      }
    } else {
      script = joined;
    }

    try {
      runScript(script, true);
    } catch (SyntaxError e) {
      System.out.print(RED);
      e.printWithContext();
      System.out.print(RESET);
    }

    return true;
  }

  private Path pathOrNull(String candidate) {
    if (candidate.contains("(")) {
      return null; // an inline s-expression script
    }
    Path path = Path.of(candidate);
    if (Files.isRegularFile(path)) {
      return path;
    }
    return null;
  }

  /** Verdict of running a single SMT-LIB script. */
  public enum Verdict {
    SAT,
    UNSAT,
    UNKNOWN
  }

  /**
   * Parses and solves a script, returning the verdict. When {@code print} is true, prints the
   * verdict and (if present) whether it matches the declared {@code :status}. Throws
   * {@link SyntaxError} on unsupported / malformed input.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Verdict runScript(String script, boolean print) {
    SmtLibParser parser = new SmtLibParser(script);
    SmtLibScript parsed = parser.parse();

    if (print) {
      for (String warning : parsed.getWarnings()) {
        System.out.println(YELLOW + "Warning: " + warning + RESET);
      }
    }

    String theoryName = mapLogic(parsed.getLogic());
    Theory theory = Theory.get(theoryName);

    TheoryCNF theoryCNF = parsed.getTheoryCNF();
    if (theoryCNF == null) {
      List<TheoryClause<Constraint>> clauses = parsed.getClauses();
      theoryCNF = new TheoryCNF(clauses);
    }

    SMTSolver smtSolver = theory.getSMTSolver();
    smtSolver.setSATSolver(new DPLLCDCLSolver());
    TheorySolver theorySolver = theory.getTheorySolver();
    smtSolver.setTheorySolver(theorySolver);
    smtSolver.load(theoryCNF);

    SMTResult<?> result = smtSolver.solve();

    Verdict verdict;
    if (result.isUnknown()) {
      verdict = Verdict.UNKNOWN;
    } else if (result.isSatisfiable()) {
      verdict = Verdict.SAT;
    } else {
      verdict = Verdict.UNSAT;
    }

    if (print) {
      printVerdict(verdict, parsed.getStatus());
    }

    return verdict;
  }

  private static void printVerdict(Verdict verdict, String status) {
    String verdictString = verdict.name().toLowerCase();
    AnsiColor color =
        switch (verdict) {
          case SAT -> GREEN;
          case UNSAT -> RED;
          case UNKNOWN -> YELLOW;
        };
    System.out.println(color + verdictString + RESET);

    if (status != null) {
      boolean matches = status.equals(verdictString);
      if (matches) {
        System.out.println(GREEN + "MATCHES expected status: " + status + RESET);
      } else {
        System.out.println(RED + "MISMATCHES expected status: " + status + RESET);
      }
    }
  }

  /** Maps an SMT-LIB logic name to the internal {@link Theory} name. */
  public static String mapLogic(String logic) {
    if (logic == null) {
      throw new SyntaxError("no logic set (expected a (set-logic ...) command)", "", 0);
    }
    return switch (logic) {
      case "QF_LRA" -> "QF_LRA";
      case "QF_LIA" -> "QF_LIA";
      case "QF_EQ" -> "QF_EQ";
      case "QF_UF", "QF_EQUF" -> "QF_EQUF";
      default ->
          throw new SyntaxError(
              "unsupported logic '"
                  + logic
                  + "' (supported: QF_LRA, QF_LIA, QF_EQ, QF_UF, QF_EQUF)",
              logic,
              0);
    };
  }
}
