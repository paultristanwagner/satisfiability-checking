package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.smt.solver.FullLazySMTSolver;
import me.paultristanwagner.satchecking.smt.solver.LessLazySMTSolver;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.solver.EqualityFunctionSolver;
import me.paultristanwagner.satchecking.theory.solver.EqualityLogicSolver;
import me.paultristanwagner.satchecking.theory.solver.SimplexOptimizationSolver;
import me.paultristanwagner.satchecking.theory.solver.TheorySolver;

public class Theory {

  private static final String QF_LRA_NAME = "QF_LRA";
  public static final Theory QF_LRA = new Theory(QF_LRA_NAME);

  private static final String QF_EQ_NAME = "QF_EQ";
  public static final Theory QF_EQ = new Theory(QF_EQ_NAME);

  private static final String QF_EQUF_NAME = "QF_EQUF";
  public static final Theory QF_EQUF = new Theory(QF_EQUF_NAME);

  private final String name;

  private Theory(String name) {
    this.name = name;
  }

  public static Theory get(String name) {
    return switch (name) {
      case QF_LRA_NAME -> QF_LRA;
      case QF_EQ_NAME -> QF_EQ;
      case QF_EQUF_NAME -> QF_EQUF;
      default -> throw new IllegalArgumentException("Unknown theory: " + name);
    };
  }

  @SuppressWarnings("rawtypes")
  public TheoryCNFParser getCNFParser() {
    return switch (name) {
      case QF_LRA_NAME -> new TheoryCNFParser<>(LinearConstraint.class);
      case QF_EQ_NAME -> new TheoryCNFParser<>(EqualityConstraint.class);
      case QF_EQUF_NAME -> new TheoryCNFParser<>(EqualityFunctionConstraint.class);
      default -> throw new IllegalArgumentException("Unknown theory: " + name);
    };
  }

  @SuppressWarnings("rawtypes")
  public TheorySolver getTheorySolver() {
    return switch (name) {
      case QF_LRA_NAME -> new SimplexOptimizationSolver();
      case QF_EQ_NAME -> new EqualityLogicSolver();
      case QF_EQUF_NAME -> new EqualityFunctionSolver();
      default -> throw new IllegalArgumentException("Unknown theory: " + name);
    };
  }

  @SuppressWarnings("rawtypes")
  public SMTSolver getSMTSolver() {
    return switch (name) {
      case QF_LRA_NAME, QF_EQUF_NAME -> new FullLazySMTSolver<>();
      case QF_EQ_NAME -> new LessLazySMTSolver<>();
      default -> throw new IllegalArgumentException("Unknown theory: " + name);
    };
  }
}
