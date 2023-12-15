package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicBiConditional;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicExpression;
import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicAnd.and;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicBiConditional.equivalence;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicImplication.implication;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicNegation.negation;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicOr.or;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicVariable.variable;

public class BitVectorSolver {

  // todo: use OOP to avoid multiple variables with the same name

  private static final int N = 8;
  private static final int DECIMAL_DIGITS = (int) Math.ceil(Math.log10(Math.pow(2, N)));

  public static void main(String[] args) {
    System.out.println("SMT solver for " + N + "-bit bit-vector arithmetic");
    System.out.println();
    System.out.println("φ := (a * b = c) & (b * a = c) & (x < y) & (y < x)");

    long time0 = System.currentTimeMillis();

    PropositionalLogicExpression multiplyAB = multiplicationFormula("a", "b", "c");
    PropositionalLogicExpression multiplyBA = multiplicationFormula("b", "a", "c");
    PropositionalLogicExpression lessThanXY = lessThanFormula("x", "y");
    PropositionalLogicExpression lessThanYX = lessThanFormula("y", "x");

    for(int i = 0; i < 10; i++) {
      Random random = new Random();

      PropositionalLogicExpression formula = and(
          valueFormula("a", random.nextInt(256)),
          valueFormula("b", random.nextInt(256)),
          remainderFormula("a", "b", "c")
      );

      long time1 = System.currentTimeMillis();
      CNF cnf = PropositionalLogicParser.tseitin(formula);
      long time2 = System.currentTimeMillis();
      System.out.println("Eager conversion to CNF: ψ :=" + cnf);
      System.out.println();

      long time3 = System.currentTimeMillis();
      SATSolver solver = new DPLLCDCLSolver();
      solver.load(cnf);

      Assignment assignment;
      while ((assignment = solver.nextModel()) != null) {
        long aValue = reconstructVariableValue(assignment, "a");
        long bValue = reconstructVariableValue(assignment, "b");
        long cValue = reconstructVariableValue(assignment, "c");
        long qValue = reconstructVariableValue(assignment, "q");

        long expectedCValue = aValue % bValue;
        if(expectedCValue != cValue) {
          System.out.println("Wrong result: " + aValue + " % " + bValue + " = " + cValue + " (expected: " + expectedCValue + ")");
        }

        System.out.printf(
            "a = 0b%s (%" + DECIMAL_DIGITS + "d), " +
                "b = 0b%s (%" + DECIMAL_DIGITS + "d), " +
                "c = 0b%s (%" + DECIMAL_DIGITS + "d), " +
                "q = 0b%s (%" + DECIMAL_DIGITS + "d), " +
                "\n",
            asBinaryString(aValue), aValue,
            asBinaryString(bValue), bValue,
            asBinaryString(cValue), cValue,
            asBinaryString(qValue), qValue
        );
      }

      System.out.println("No more models");

      long time4 = System.currentTimeMillis();

      long constructionTime = time1 - time0;
      long tseitinTime = time2 - time1;
      long solvingTime = time4 - time3;

      System.out.println();
      System.out.println("Construction time: " + constructionTime + "ms");
      System.out.println("Tseitin conversion time: " + tseitinTime + "ms");
      System.out.println("Solving time: " + solvingTime + "ms");
    }
  }

  private static PropositionalLogicExpression valueFormula(String variable, long value) {
    PropositionalLogicExpression result = bitValueFormula(variable + "_0", (value & 1) != 0);
    for (int i = 1; i < N; i++) {
      String bitVariable = variable + "_" + i;
      boolean bitValue = (value & (1L << i)) != 0;

      result = and(result, bitValueFormula(bitVariable, bitValue));
    }

    return result;
  }

  private static PropositionalLogicExpression equalFormula(String variable1, String variable2) {
    List<PropositionalLogicExpression> equalExpressions = new ArrayList<>();
    for (int i = 0; i < N; i++) {
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

  private static PropositionalLogicExpression inequalityFormula(String variable1, String variable2) {
    List<PropositionalLogicExpression> inequalityExpressions = new ArrayList<>();
    for (int i = 0; i < N; i++) {
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

  private static PropositionalLogicExpression lessThanFormula(String variable1, String variable2) {
    PropositionalLogicExpression result = null;
    for (int r = N - 1; r >= 0; r--) {
      String bitVariable1 = variable1 + "_" + r;
      String bitVariable2 = variable2 + "_" + r;

      PropositionalLogicExpression caseExpression = null;

      for (int s = N - 1; s > r; s--) {
        String bitVariable1_s = variable1 + "_" + s;
        String bitVariable2_s = variable2 + "_" + s;

        PropositionalLogicExpression equalExpression = equivalence(
            variable(bitVariable1_s),
            variable(bitVariable2_s)
        );

        if (s == N - 1) {
          caseExpression = equalExpression;
        } else {
          caseExpression = and(caseExpression, equalExpression);
        }
      }

      PropositionalLogicExpression differenceExpression = and(
          negation(variable(bitVariable1)),
          variable(bitVariable2)
      );

      if (caseExpression != null) {
        caseExpression = and(caseExpression, differenceExpression);
      } else {
        caseExpression = differenceExpression;
      }

      if (r == N - 1) {
        result = caseExpression;
      } else {
        result = or(result, caseExpression);
      }
    }

    return result;
  }

  private static PropositionalLogicExpression additionFormula(String variable1, String variable2, String resultVariable) {
    PropositionalLogicExpression result = halfAdderFormula(variable1 + "_0", variable2 + "_0", resultVariable + "_0", "carry_add_" + variable1 + "_" + variable2 + "_0");

    for (int i = 1; i < N; i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;
      String bitVariableResult = resultVariable + "_" + i;

      PropositionalLogicExpression fullAdder = fullAdderFormula(
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

  private static PropositionalLogicExpression multiplicationFormula(String variable1, String variable2, String resultVariable) {
    List<PropositionalLogicExpression> individualProductsList = new ArrayList<>();
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
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
        halfAdderFormula(
            "mult_h_" + variable1 + "_" + variable2 + "_1_0",
            "mult_h_" + variable1 + "_" + variable2 + "_0_1",
            resultVariable + "_1",
            "carry_mult_" + variable1 + "_" + variable2 + "_1_0")
    );

    for (int i = 2; i < N; i++) {
      adderList.add(
          fullAdderFormula(
              "mult_h_" + variable1 + "_" + variable2 + "_" + i + "_0",
              "mult_h_" + variable1 + "_" + variable2 + "_" + (i - 1) + "_1",
              "carry_mult_" + variable1 + "_" + variable2 + "_" + (i - 1) + "_0",
              "mult_value_" + variable1 + "_" + variable2 + "_" + i + "_0",
              "carry_mult_" + variable1 + "_" + variable2 + "_" + i + "_0"
          )
      );
    }

    for (int i = 2; i < N; i++) {
      for (int j = 1; j < i; j++) {
        if (j < i - 1) {
          adderList.add(
              fullAdderFormula(
                  "mult_value_" + variable1 + "_" + variable2 + "_" + i + "_" + (j - 1),
                  "mult_h_" + variable1 + "_" + variable2 + "_" + (i - j - 1) + "_" + (j + 1),
                  "carry_mult_" + variable1 + "_" + variable2 + "_" + (i - 1) + "_" + j,
                  "mult_value_" + variable1 + "_" + variable2 + "_" + i + "_" + j,
                  "carry_mult_" + variable1 + "_" + variable2 + "_" + i + "_" + j
              )
          );
        } else {
          adderList.add(
              halfAdderFormula(
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

  // todo: this does not work because of overflows
  private static PropositionalLogicExpression remainderFormula(String variable1, String variable2, String resultVariable) {
    List<PropositionalLogicExpression> components = new ArrayList<>();
    components.add(
        implication(
            lessThanFormula(variable1, variable2),
            equivalence(
                variable(resultVariable),
                variable(variable1)
            )
        )
    );

    components.add(multiplicationFormula("q", variable2, "product_q_" + variable2));
    components.add(additionFormula(resultVariable, "product_q_" + variable2, variable1));
    components.add(lessThanFormula(resultVariable, variable2));
    components.add(lessThanFormula("q", variable1));

    // product_q_variable2 = q * variable2
    // variable1 = resultVariable + product_q_variable2

    return and(components);
  }

  private static PropositionalLogicExpression halfAdderFormula(String bitVariable1, String bitVariable2, String bitVariableResult, String carryVariable) {
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

  private static PropositionalLogicExpression fullAdderFormula(String bitVariable1, String bitVariable2, String bitVariable3, String bitVariableResult, String bitCarryVariable) {
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

  public static PropositionalLogicExpression staticLeftShiftFormula(String variable, String resultVariable, long shift) {
    List<PropositionalLogicExpression> individualShifts = new ArrayList<>();
    for (int i = 0; i < N; i++) {
      String bitVariable = variable + "_" + (i - shift);
      String bitVariableResult = resultVariable + "_" + i;

      if (shift < 0 || shift > i) { // negative shift values can represent huge positive values
        individualShifts.add(bitValueFormula(bitVariableResult, false));
      } else {
        individualShifts.add(equivalence(
            variable(bitVariableResult),
            variable(bitVariable)
        ));
      }
    }

    return and(individualShifts);
  }

  private static PropositionalLogicExpression staticRightShiftFormula(String variable, String resultVariable, long shift) {
    List<PropositionalLogicExpression> individualShifts = new ArrayList<>();
    for (int i = 0; i < N; i++) {
      String bitVariable = variable + "_" + (i + shift);
      String bitVariableResult = resultVariable + "_" + i;

      if (shift < 0 || shift > N - i - 1) {
        individualShifts.add(bitValueFormula(bitVariableResult, false));
      } else {
        individualShifts.add(equivalence(
            variable(bitVariableResult),
            variable(bitVariable)
        ));
      }
    }

    return and(individualShifts);
  }

  // todo: we can do better by reducing the number of rounds to log2(N)
  private static PropositionalLogicExpression leftShiftFormula(String variable1, String variable2, String resultVariable) {
    List<PropositionalLogicExpression> shifts = new ArrayList<>();

    for (int s = 0; s < N; s++) {
      shifts.add(
          implication(
              variable(variable2 + "_" + s),
              staticLeftShiftFormula(
                  s > 0 ? "shift_value_" + variable1 + "_" + variable2 + "_" + s : variable1,
                  s < N - 1 ? "shift_value_" + variable1 + "_" + variable2 + "_" + (s + 1) : resultVariable,
                  1L << s
              )
          )
      );

      for (int i = 0; i < N; i++) {
        shifts.add(
            implication(
                negation(variable(variable2 + "_" + s)),
                equivalence(
                    variable(s > 0 ? "shift_value_" + variable1 + "_" + variable2 + "_" + s + "_" + i : variable1 + "_" + i),
                    variable(s < N - 1 ? "shift_value_" + variable1 + "_" + variable2 + "_" + (s + 1) + "_" + i : resultVariable + "_" + i)
                )
            )
        );
      }
    }

    return and(shifts);
  }

  // todo: we can do better by reducing the number of rounds to log2(N)
  private static PropositionalLogicExpression rightShiftFormula(String variable1, String variable2, String resultVariable) {
    List<PropositionalLogicExpression> shifts = new ArrayList<>();

    for (int s = 0; s < N; s++) {
      shifts.add(
          implication(
              variable(variable2 + "_" + s),
              staticRightShiftFormula(
                  s < N - 1 ? "shift_value_" + variable1 + "_" + variable2 + "_" + s : variable1,
                  s > 0 ? "shift_value_" + variable1 + "_" + variable2 + "_" + (s - 1) : resultVariable,
                  1L << s
              )
          )
      );

      for (int i = 0; i < N; i++) {
        shifts.add(
            implication(
                negation(variable(variable2 + "_" + s)),
                equivalence(
                    variable(s < N - 1 ? "shift_value_" + variable1 + "_" + variable2 + "_" + s + "_" + i : variable1 + "_" + i),
                    variable(s > 0 ? "shift_value_" + variable1 + "_" + variable2 + "_" + (s - 1) + "_" + i : resultVariable + "_" + i)
                )
            )
        );
      }
    }

    return and(shifts);
  }

  private static PropositionalLogicExpression bitValueFormula(String bitVariable, boolean value) {
    if (value) {
      return variable(bitVariable);
    } else {
      return negation(variable(bitVariable));
    }
  }

  private static long reconstructVariableValue(Assignment assignment, String bitVariable) {
    long result = 0;
    for (int i = 0; i < N; i++) {
      String bitVariable_i = bitVariable + "_" + i;
      try {
        if (assignment.getValue(bitVariable_i)) {
          result |= 1L << i;
        }
      } catch (Exception ignored) {
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

  private static long log2(long n) {
    if(n <= 0) {
      throw new IllegalArgumentException("n must be positive");
    }

    return 63 - Long.numberOfLeadingZeros(n);
  }
}
