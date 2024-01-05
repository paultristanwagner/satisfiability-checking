package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.smt.solver.FullLazySMTSolver;
import me.paultristanwagner.satchecking.smt.solver.LessLazySMTSolver;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorConstraint;
import me.paultristanwagner.satchecking.theory.solver.*;

import java.util.List;

public class Theory {

  private static final String QF_LRA_NAME = "QF_LRA";
  public static final Theory QF_LRA = new Theory(QF_LRA_NAME, true);

  private static final String QF_LIA_NAME = "QF_LIA";
  public static final Theory QF_LIA = new Theory(QF_LIA_NAME, false);

  private static final String QF_EQ_NAME = "QF_EQ";
  public static final Theory QF_EQ = new Theory(QF_EQ_NAME, true);

  private static final String QF_EQUF_NAME = "QF_EQUF";
  public static final Theory QF_EQUF = new Theory(QF_EQUF_NAME, true);

  private static final String QF_BV_NAME = "QF_BV";
  public static final Theory QF_BV = new Theory(QF_BV_NAME, true);

  private final static List<Theory> theories = List.of(QF_LRA, QF_LIA, QF_EQ, QF_EQUF, QF_BV);

  private final String name;
  private final boolean complete;

  private Theory(String name, boolean complete) {
    this.name = name;
    this.complete = complete;
  }

  public static Theory get(String name) {
    for (Theory theory : theories) {
      if (theory.name.equalsIgnoreCase(name)) {
        return theory;
      }
    }

    throw new IllegalArgumentException("Unknown theory: " + name);
  }

  @SuppressWarnings("rawtypes")
  public TheoryCNFParser getCNFParser() {
    return switch (name) {
      case QF_LRA_NAME, QF_LIA_NAME -> new TheoryCNFParser<>(LinearConstraint.class);
      case QF_EQ_NAME -> new TheoryCNFParser<>(EqualityConstraint.class);
      case QF_EQUF_NAME -> new TheoryCNFParser<>(EqualityFunctionConstraint.class);
      case QF_BV_NAME -> new TheoryCNFParser<>(BitVectorConstraint.class);
      default -> throw new IllegalArgumentException("Unknown theory: " + name);
    };
  }

  @SuppressWarnings("rawtypes")
  public TheorySolver getTheorySolver() {
    return switch (name) {
      case QF_LRA_NAME -> new SimplexOptimizationSolver();
      case QF_LIA_NAME -> new LinearIntegerSolver();
      case QF_EQ_NAME -> new EqualityLogicSolver();
      case QF_EQUF_NAME -> new EqualityFunctionSolver();
      case QF_BV_NAME -> new BitVectorSolver();
      default -> throw new IllegalArgumentException("Unknown theory: " + name);
    };
  }

  @SuppressWarnings("rawtypes")
  public SMTSolver getSMTSolver() {
    return switch (name) {
      case QF_LRA_NAME, QF_LIA_NAME, QF_EQUF_NAME, QF_BV_NAME -> new FullLazySMTSolver<>();
      case QF_EQ_NAME -> new LessLazySMTSolver<>();
      default -> throw new IllegalArgumentException("Unknown theory: " + name);
    };
  }

  public boolean isComplete() {
    return complete;
  }
}
