package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.*;
import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;

import java.util.ArrayList;
import java.util.List;

import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicAnd.and;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicBiConditional.equivalence;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicNegation.negation;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicOr.or;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicVariable.variable;

public class BitVectorSolver {

  private static final int N = 32;
  private static final int DECIMAL_DIGITS = (int) Math.ceil(Math.log10(Math.pow(2, N)));

  public static void main(String[] args) {
    System.out.println("SMT solver for " + N + "-bit bit-vector arithmetic");
    System.out.println();
    System.out.println("φ := (m = x * y) & (s = x + y) & (m < s)");

    PropositionalLogicExpression x = createBitVectorValueExpression("x", 3L);
    PropositionalLogicExpression y = createBitVectorValueExpression("y", 2L);
    PropositionalLogicExpression multiplyXY = createMultiplicationExpression("x", "y", "m");
    PropositionalLogicExpression addXY = createMultiplicationExpression("x", "y", "s");
    PropositionalLogicExpression lessThan = createBitVectorLessThanExpression("m", "s");

    PropositionalLogicExpression formula = and(multiplyXY, addXY, lessThan);

    CNF cnf = PropositionalLogicParser.tseitin(formula);
    System.out.println("Eager conversion to CNF: ψ :=" + cnf);
    System.out.println();

    SATSolver solver = new DPLLCDCLSolver();
    solver.load(cnf);

    int models = 0;
    Assignment assignment;
    while(models < 10 && (assignment = solver.nextModel()) != null) {
      models++;

      long xValue = reconstructVariableValue(assignment, "x");
      long yValue = reconstructVariableValue(assignment, "y");
      long mValue = reconstructVariableValue(assignment, "m");
      long sValue = reconstructVariableValue(assignment, "s");

      System.out.printf(
          "x = 0b%s (%" + DECIMAL_DIGITS + "d), " +
          "y = 0b%s (%" + DECIMAL_DIGITS + "d), " +
          "m = 0b%s (%" + DECIMAL_DIGITS + "d), " +
          "s = 0b%s (%" + DECIMAL_DIGITS + "d)%n",
          asBinaryString(xValue), xValue,
          asBinaryString(yValue), yValue,
          asBinaryString(mValue), mValue,
          asBinaryString(sValue), sValue
      );
    }

    if(models == 0) {
      System.out.println("No models found");
    } else {
      System.out.printf("Found %d models\n", models);
    }
  }

  private static PropositionalLogicExpression createBitVectorValueExpression(String variable, long value) {
    PropositionalLogicExpression result = createBitValueExpression(variable + "_0", (value & 1) != 0);
    for (int i = 1; i < N; i++) {
      String bitVariable = variable + "_" + i;
      boolean bitValue = (value & (1L << i)) != 0;

      result = and(result, createBitValueExpression(bitVariable, bitValue));
    }

    return result;
  }

  private static PropositionalLogicExpression createEqualExpression(String variable1, String variable2) {
    List<PropositionalLogicExpression> equalExpressions = new ArrayList<>();
    for(int i = 0; i < N; i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;

      equalExpressions.add(
          equivalence(
              variable(bitVariable1),
              variable(bitVariable2)
          )
      );
    }

    return and(equalExpressions);
  }

  private static PropositionalLogicExpression createInequalityExpression(String variable1, String variable2) {
    List<PropositionalLogicExpression> inequalityExpressions = new ArrayList<>();
    for(int i = 0; i < N; i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;

      inequalityExpressions.add(
          equivalence(
              variable(bitVariable1),
              negation(variable(bitVariable2))
          )
      );
    }

    return or(inequalityExpressions);
  }

  private static PropositionalLogicExpression createBitVectorLessThanExpression(String variable1, String variable2) {
    PropositionalLogicExpression result = null;
    for (int r = N - 1; r >= 0; r--) {
      String bitVariable1 = variable1 + "_" + r;
      String bitVariable2 = variable2 + "_" + r;

      PropositionalLogicExpression caseExpression = null;

      for(int s = N - 1; s > r; s--) {
        String bitVariable1_s = variable1 + "_" + s;
        String bitVariable2_s = variable2 + "_" + s;

        PropositionalLogicExpression equalExpression = new PropositionalLogicBiConditional(
            new PropositionalLogicVariable(bitVariable1_s),
            new PropositionalLogicVariable(bitVariable2_s)
        );

        if (s == N - 1) {
          caseExpression = equalExpression;
        } else {
          caseExpression = new PropositionalLogicAnd(caseExpression, equalExpression);
        }
      }

      PropositionalLogicExpression differenceExpression = new PropositionalLogicAnd(
          new PropositionalLogicNegation(new PropositionalLogicVariable(bitVariable1)),
          new PropositionalLogicVariable(bitVariable2)
      );

      if(caseExpression != null) {
        caseExpression = new PropositionalLogicAnd(caseExpression, differenceExpression);
      } else {
        caseExpression = differenceExpression;
      }

      if(r == N - 1) {
        result = caseExpression;
      } else {
        result = new PropositionalLogicOr(result, caseExpression);
      }
    }

    return result;
  }

  private static PropositionalLogicExpression createAdditionExpression(String variable1, String variable2, String resultVariable) {
    PropositionalLogicExpression result = halfAdder(variable1 + "_0", variable2 + "_0", resultVariable + "_0", "carry_add_" + variable1 + "_" + variable2 + "_0");

    for (int i = 1; i < N; i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;
      String bitVariableResult = resultVariable + "_" + i;

      PropositionalLogicExpression fullAdder = fullAdder(
          bitVariable1,
          bitVariable2,
          "carry_add_" + variable1 + "_" + variable2 + "_" + (i - 1),
          bitVariableResult,
          "carry_add_" + variable1 + "_" + variable2 + "_" + i
      );

      result = and(result, fullAdder);
    }

    return result;
  }

  private static PropositionalLogicExpression createMultiplicationExpression(String variable1, String variable2, String resultVariable) {
    List<PropositionalLogicExpression> individualProductsList = new ArrayList<>();
    for(int i = 0; i < N; i++) {
      for(int j = 0; j < N; j++) {
        PropositionalLogicBiConditional definition = equivalence(
            variable("mult_h_" + variable1 + "_" + variable2 + "_" + i + "_" + j),
            and(
                variable(variable1 + "_" + i),
                variable(variable2 + "_" + j)
            )
        );

        individualProductsList.add(definition);
      }
    }

    PropositionalLogicExpression individualProducts = and(individualProductsList);

    PropositionalLogicExpression r0 = equivalence(
        variable(resultVariable + "_0"),
        variable("mult_h_" + variable1 + "_" + variable2 + "_0_0")
    );

    List<PropositionalLogicExpression> adderList = new ArrayList<>();

    adderList.add(
        halfAdder(
            "mult_h_" + variable1 + "_" + variable2 + "_1_0",
            "mult_h_" + variable1 + "_" + variable2 + "_0_1",
            resultVariable + "_1",
            "carry_mult_" + variable1 + "_" + variable2 + "_1_0")
    );

    for(int i = 2; i < N; i++) {
      adderList.add(
        fullAdder(
            "mult_h_" + variable1 + "_" + variable2 + "_" + i + "_0",
            "mult_h_" + variable1 + "_" + variable2 + "_" + (i - 1) + "_1",
            "carry_mult_" + variable1 + "_" + variable2 + "_" + (i - 1) + "_0",
            "mult_value_" + variable1 + "_" + variable2 + "_" + i + "_0",
            "carry_mult_" + variable1 + "_" + variable2 + "_" + i + "_0"
        )
      );
    }

    for(int i = 2; i < N; i++) {
      for(int j = 1; j < i; j++) {
        if(j < i - 1) {
          adderList.add(
              fullAdder(
                  "mult_value_" + variable1 + "_" + variable2 + "_" + i + "_" + (j - 1),
                  "mult_h_" + variable1 + "_" + variable2 + "_" + (i - j - 1) + "_" + (j + 1),
                  "carry_mult_" + variable1 + "_" + variable2 + "_" + (i - 1) + "_" + j,
                  "mult_value_" + variable1 + "_" + variable2 + "_" + i + "_" + j,
                  "carry_mult_" + variable1 + "_" + variable2 + "_" + i + "_" + j
              )
          );
        } else {
          adderList.add(
              halfAdder(
                  "mult_value_" + variable1 + "_" + variable2 + "_" + i + "_" + (j - 1),
                  "mult_h_" + variable1 + "_" + variable2 + "_" + (i - j - 1) + "_" + (j + 1),
                  resultVariable + "_" + i,
                  "carry_mult_" + variable1 + "_" + variable2 + "_" + i + "_" + j
              )
          );
        }
      }
    }

    PropositionalLogicExpression adderExpression = and(adderList);

    return and(individualProducts, r0, adderExpression);
  }

  private static PropositionalLogicExpression halfAdder(String bitVariable1, String bitVariable2, String bitVariableResult, String carryVariable) {
    PropositionalLogicExpression add = equivalence(
        variable(bitVariableResult),
        or(
            and(
                variable(bitVariable1),
                negation(variable(bitVariable2))
            ),
            and(
                negation(variable(bitVariable1)),
                variable(bitVariable2)
            )
        )
    );

    PropositionalLogicExpression carry = equivalence(
        variable(carryVariable),
        and(
            variable(bitVariable1),
            variable(bitVariable2)
        )
    );

    return and(add, carry);
  }

  private static PropositionalLogicExpression fullAdder(String bitVariable1, String bitVariable2, String bitVariable3, String bitVariableResult, String bitCarryVariable) {
    PropositionalLogicExpression add = equivalence(
        variable(bitVariableResult),
        or(
            and(
                variable(bitVariable1),
                negation(variable(bitVariable2)),
                negation(variable(bitVariable3))
            ),
            and(
                negation(variable(bitVariable1)),
                variable(bitVariable2),
                negation(variable(bitVariable3))
            ),
            and(
                negation(variable(bitVariable1)),
                negation(variable(bitVariable2)),
                variable(bitVariable3)
            ),
            and(
                variable(bitVariable1),
                variable(bitVariable2),
                variable(bitVariable3)
            )
        )
    );

    PropositionalLogicExpression carry = equivalence(
        variable(bitCarryVariable),
        or(
            and(
                variable(bitVariable1),
                variable(bitVariable2),
                negation(variable(bitVariable3))
            ),
            and(
                variable(bitVariable1),
                negation(variable(bitVariable2)),
                variable(bitVariable3)
            ),
            and(
                negation(variable(bitVariable1)),
                variable(bitVariable2),
                variable(bitVariable3)
            ),
            and(
                variable(bitVariable1),
                variable(bitVariable2),
                variable(bitVariable3)
            )
        )
    );

    return and(add, carry);
  }

  private static PropositionalLogicExpression createBitValueExpression(String bitVariable, boolean value) {
    if (value) {
      return new PropositionalLogicVariable(bitVariable);
    } else {
      return new PropositionalLogicNegation(new PropositionalLogicVariable(bitVariable));
    }
  }

  private static long reconstructVariableValue(Assignment assignment, String bitVariable) {
    long result = 0;
    for(int i = 0; i < N; i++) {
      String bitVariable_i = bitVariable + "_" + i;
      if(assignment.getValue(bitVariable_i)) {
        result |= 1L << i;
      }
    }

    return result;
  }

  public static String asBinaryString(long n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < N; i++) {
      sb.append(n & 1L);
      n >>>= 1L;
    }
    return sb.reverse().toString();
  }
}
