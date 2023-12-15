package me.paultristanwagner.satchecking.theory.bitvector;

import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicExpression;
import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;

import java.util.*;

import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicAnd.and;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicBiConditional.equivalence;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicImplication.implication;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicNegation.negation;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicOr.or;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicVariable.variable;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorConstant.constant;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorEqualConstraint.equal;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorLeftShift.leftShift;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorLessThanConstraint.lessThan;

public class BitVectorFlattener {

  private static final String BIT_VECTOR_VARIABLE_PREFIX = "bv_";

  private int bitVectorVariableCounter = 0;
  private final Map<BitVectorTerm, String> bitVectorVariableToName = new HashMap<>();

  private static final int DEFAULT_BIT_VECTOR_LENGTH = 32;

  public static void main(String[] args) {
    BitVectorTerm a = BitVectorVariable.bitvector("a", DEFAULT_BIT_VECTOR_LENGTH);
    BitVectorTerm b = BitVectorVariable.bitvector("b", DEFAULT_BIT_VECTOR_LENGTH);
    BitVectorTerm c = BitVectorVariable.bitvector("c", DEFAULT_BIT_VECTOR_LENGTH);

    BitVectorConstraint c1 = equal(
        c,
        leftShift(
            a,
            b
        )
    );

    BitVectorConstraint c2 = equal(
        a,
        constant(1, DEFAULT_BIT_VECTOR_LENGTH)
    );

    BitVectorConstraint c3 = lessThan(
        c,
        constant(35000, DEFAULT_BIT_VECTOR_LENGTH)
    );

    BitVectorConstraint c4 = lessThan(
        constant(0, DEFAULT_BIT_VECTOR_LENGTH),
        c
    );

    List<BitVectorConstraint> constraints = List.of(c1, c2, c3, c4);

    BitVectorFlattener flattener = new BitVectorFlattener();
    constraints.forEach(System.out::println);
    CNF cnf = flattener.flatten(constraints);

    SATSolver satSolver = new DPLLCDCLSolver();
    satSolver.load(cnf);
    Assignment assignment;
    while ((assignment = satSolver.nextModel()) != null) {
      BitVector aBitVector = flattener.reconstruct(a, assignment);
      BitVector bBitVector = flattener.reconstruct(b, assignment);
      BitVector cBitVector = flattener.reconstruct(c, assignment);

      System.out.printf(
          "a = %s (%d), " +
          "b = %s (%d), " +
          "c = %s (%d), " +
              "\n",
          aBitVector, aBitVector.asLong(),
          bBitVector, bBitVector.asLong(),
          cBitVector, cBitVector.asLong()
      );
    }
  }

  public CNF flatten(List<BitVectorConstraint> constraints) {
    List<PropositionalLogicExpression> expressions = new ArrayList<>();
    for (BitVectorConstraint constraint : constraints) {
      PropositionalLogicExpression expression = convertConstraint(constraint);
      if (expression != null) {
        expressions.add(expression);
      }
    }

    PropositionalLogicExpression conjunction = and(expressions);

    return PropositionalLogicParser.tseitin(conjunction);
  }

  private PropositionalLogicExpression convertConstraint(BitVectorConstraint constraint) {
    if (constraint instanceof BitVectorEqualConstraint equalConstraint) {
      return convertEqualConstraint(equalConstraint);
    } else if (constraint instanceof BitVectorLessThanConstraint lessThanConstraint) {
      return convertLessThanConstraint(lessThanConstraint);
    }

    throw new UnsupportedOperationException("Not implemented yet");
  }

  private PropositionalLogicExpression convertEqualConstraint(BitVectorEqualConstraint constraint) {
    BitVectorTerm term1 = constraint.getTerm1();
    BitVectorTerm term2 = constraint.getTerm2();

    if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorEqualConstraint: term1 and term2 must have the same length!");
    }

    PropositionalLogicExpression expression1 = convertTerm(term1);
    PropositionalLogicExpression expression2 = convertTerm(term2);

    String expression1Name = bitVectorVariableToName.get(term1);
    String expression2Name = bitVectorVariableToName.get(term2);

    List<PropositionalLogicExpression> bitEqualFormulas = new ArrayList<>();
    for (int i = 0; i < term1.getLength(); i++) {
      String bitVariable1 = expression1Name + "_" + i;
      String bitVariable2 = expression2Name + "_" + i;

      PropositionalLogicExpression bitEqualFormula = equivalence(
          variable(bitVariable1),
          variable(bitVariable2)
      );

      bitEqualFormulas.add(bitEqualFormula);
    }

    return and(expression1, expression2, and(bitEqualFormulas));
  }

  private PropositionalLogicExpression convertLessThanConstraint(BitVectorLessThanConstraint constraint) {
    BitVectorTerm term1 = constraint.getTerm1();
    BitVectorTerm term2 = constraint.getTerm2();

    if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorLessThanConstraint: term1 and term2 must have the same length!");
    }

    PropositionalLogicExpression expression1 = convertTerm(term1);
    PropositionalLogicExpression expression2 = convertTerm(term2);

    String expression1Name = bitVectorVariableToName.get(term1);
    String expression2Name = bitVectorVariableToName.get(term2);

    List<PropositionalLogicExpression> caseExpressions = new ArrayList<>();

    for (int r = term1.getLength() - 1; r >= 0; r--) {
      String bitVariable1 = expression1Name + "_" + r;
      String bitVariable2 = expression2Name + "_" + r;

      List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();
      for (int s = term1.getLength() - 1; s > r; s--) {
        String bitVariable1_s = expression1Name + "_" + s;
        String bitVariable2_s = expression2Name + "_" + s;

        bitValueFormulas.add(
            equivalence(
                variable(bitVariable1_s),
                variable(bitVariable2_s)
            )
        );
      }

      bitValueFormulas.add(
          and(
              negation(variable(bitVariable1)),
              variable(bitVariable2)
          )
      );

      caseExpressions.add(
          and(bitValueFormulas)
      );
    }

    PropositionalLogicExpression caseDisjunction = or(caseExpressions);
    return and(expression1, expression2, caseDisjunction);
  }

  private PropositionalLogicExpression convertTerm(BitVectorTerm term) {
    if (term instanceof BitVectorVariable variable) {
      String variableName = freshBitVectorVariableName(variable);
      return null;
    } else if (term instanceof BitVectorConstant constant) {
      return convertConstant(constant);
    } else if (term instanceof BitVectorAddition addition) {
      return convertAddition(addition);
    } else if (term instanceof BitVectorProduct product) {
      return convertProduct(product);
    } else if (term instanceof BitVectorLeftShift leftShift) {
      return convertLeftShift(leftShift);
    }

    throw new UnsupportedOperationException("Not implemented yet");
  }

  private PropositionalLogicExpression convertConstant(BitVectorConstant constant) {
    String variableName = freshBitVectorVariableName(constant);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

    for (int i = 0; i < constant.getLength(); i++) {
      String bitVariable = variableName + "_" + i;
      boolean bitValue = constant.getBitVector().getBit(i);

      bitValueFormulas.add(bitValueFormula(bitVariable, bitValue));
    }

    return and(bitValueFormulas);
  }

  private PropositionalLogicExpression convertAddition(BitVectorAddition addition) {
    BitVectorTerm term1 = addition.getTerm1();
    BitVectorTerm term2 = addition.getTerm2();

    if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorAddition: term1 and term2 must have the same length!");
    }

    PropositionalLogicExpression expression1 = convertTerm(term1);
    PropositionalLogicExpression expression2 = convertTerm(term2);

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(addition);
    String carryVariable = freshAnonymousVariableName();

    List<PropositionalLogicExpression> expressions = new ArrayList<>();

    expressions.add(
        halfAdderFormula(
            variable1 + "_0",
            variable2 + "_0",
            resultVariable + "_0",
            carryVariable + "_0"
        )
    );

    for (int i = 1; i < addition.getLength(); i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;
      String bitVariableResult = resultVariable + "_" + i;
      String previousCarryVariable = carryVariable + "_" + (i - 1);
      String bitCarryVariable = carryVariable + "_" + i;

      expressions.add(
          fullAdderFormula(
              bitVariable1,
              bitVariable2,
              previousCarryVariable,
              bitVariableResult,
              bitCarryVariable
          )
      );
    }

    return and(and(expressions), expression1, expression2);
  }

  private PropositionalLogicExpression convertLeftShift(BitVectorLeftShift leftShift) {
    if(leftShift.getTerm2() instanceof BitVectorConstant) {
      return convertConstantLeftShift(leftShift);
    }

    BitVectorTerm term1 = leftShift.getTerm1();
    BitVectorTerm term2 = leftShift.getTerm2();

    if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorLeftShift: term1 and term2 must have the same length!");
    }

    PropositionalLogicExpression expression1 = convertTerm(term1);
    PropositionalLogicExpression expression2 = convertTerm(term2);

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(leftShift);
    String helperVariable = freshAnonymousVariableName();

    List<PropositionalLogicExpression> shifts = new ArrayList<>();

    for (int s = 0; s < leftShift.getLength(); s++) {
      String previousRoundVariable = s > 0 ? helperVariable + "_" + s : variable1;

      for (int i = 0; i < leftShift.getLength(); i++) {
        String bitVariableResult = resultVariable + "_" + i;

        long shift = 1L << s;

        String currentRoundBitVariable = s < leftShift.getLength() - 1 ? helperVariable + "_" + (s + 1) + "_" + i : bitVariableResult;
        String previousRoundBitVariable =  previousRoundVariable + "_" + i;

        if(shift < 0 || i < shift) {
          shifts.add(
              implication(
                  variable(variable2 + "_" + s),
                  negation(variable(currentRoundBitVariable))
              )
          );
        } else {
          String shiftedPreviousRoundBitVariable = previousRoundVariable + "_" + (i - shift);

          shifts.add(
              implication(
                  variable(variable2 + "_" + s),
                  equivalence(
                      variable(shiftedPreviousRoundBitVariable),
                      variable(currentRoundBitVariable)
                  )
              )
          );
        }

        shifts.add(
            implication(
                negation(variable(variable2 + "_" + s)),
                equivalence(
                    variable(previousRoundBitVariable),
                    variable(currentRoundBitVariable)
                )
            )
        );
      }
    }

    return and(expression1, expression2, and(shifts));
  }

  private PropositionalLogicExpression convertConstantLeftShift(BitVectorLeftShift leftShift) {
    if(!(leftShift.getTerm2() instanceof BitVectorConstant term2)) {
      throw new IllegalArgumentException("BitVectorLeftShift: term2 must be a constant!");
    }

    BitVectorTerm term1 = leftShift.getTerm1();

    PropositionalLogicExpression expression1 = convertTerm(term1);
    PropositionalLogicExpression expression2 = convertTerm(term2);

    String variable1 = bitVectorVariableToName.get(term1);

    String resultVariable = freshBitVectorVariableName(leftShift);

    List<PropositionalLogicExpression> expressions = new ArrayList<>();

    for (int i = 0; i < leftShift.getLength(); i++) {
      long shift = term2.getBitVector().asLong(); // todo: this might not be correct

      String bitVariableResult = resultVariable + "_" + i;

      if(i < shift) {
        expressions.add(
            negation(variable(bitVariableResult))
        );
      } else {
        String bitVariable1 = variable1 + "_" + (i - shift);

        expressions.add(
            equivalence(
                variable(bitVariableResult),
                variable(bitVariable1)
            )
        );
      }
    }

    return and(expression1, expression2, and(expressions));
  }

  private PropositionalLogicExpression convertProduct(BitVectorProduct product) {
    BitVectorTerm term1 = product.getTerm1();
    BitVectorTerm term2 = product.getTerm2();

    if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException("BitVectorAddition: term1 and term2 must have the same length!");
    }

    PropositionalLogicExpression expression1 = convertTerm(term1);
    PropositionalLogicExpression expression2 = convertTerm(term2);

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(product);
    String individualProductVariable = freshAnonymousVariableName();
    String carryVariable = freshAnonymousVariableName();
    String helperVariable = freshAnonymousVariableName();

    List<PropositionalLogicExpression> individualProductsList = new ArrayList<>();
    for (int i = 0; i < product.getLength(); i++) {
      for (int j = 0; j < product.getLength(); j++) {
        PropositionalLogicParser.PropositionalLogicBiConditional definition = equivalence(
            variable(individualProductVariable + "_" + i + "_" + j),
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
        variable(individualProductVariable + "_0_0")
    );

    List<PropositionalLogicExpression> adderList = new ArrayList<>();

    adderList.add(
        halfAdderFormula(
            individualProductVariable +  "_1_0",
            individualProductVariable + "_0_1",
            resultVariable + "_1",
            carryVariable + "_1_0")
    );

    for (int i = 2; i < product.getLength(); i++) {
      adderList.add(
          fullAdderFormula(
              individualProductVariable + "_" + i + "_0",
              individualProductVariable + "_" + (i - 1) + "_1",
              carryVariable + "_" + (i - 1) + "_0",
              helperVariable + "_" + i + "_0",
              carryVariable + "_" + i + "_0"
          )
      );
    }

    for (int i = 2; i < product.getLength(); i++) {
      for (int j = 1; j < i; j++) {
        if (j < i - 1) {
          adderList.add(
              fullAdderFormula(
                  helperVariable + "_" + i + "_" + (j - 1),
                  individualProductVariable + "_" + (i - j - 1) + "_" + (j + 1),
                  carryVariable + "_" + (i - 1) + "_" + j,
                  helperVariable + "_" + i + "_" + j,
                  carryVariable + "_" + i + "_" + j
              )
          );
        } else {
          adderList.add(
              halfAdderFormula(
                  helperVariable + "_" + i + "_" + (j - 1),
                  individualProductVariable + "_" + (i - j - 1) + "_" + (j + 1),
                  resultVariable + "_" + i,
                  carryVariable + "_" + i + "_" + j
              )
          );
        }
      }
    }

    PropositionalLogicExpression adderExpression = and(adderList);

    return and(expression1, expression2, individualProducts, r0, adderExpression);
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

  private static PropositionalLogicExpression bitValueFormula(String bitVariable, boolean value) {
    if (value) {
      return variable(bitVariable);
    } else {
      return negation(variable(bitVariable));
    }
  }

  private String freshAnonymousVariableName() {
    return freshBitVectorVariableName(null);
  }

  private String freshBitVectorVariableName(BitVectorTerm term) {
    if(term != null && bitVectorVariableToName.containsKey(term)) {
      return bitVectorVariableToName.get(term);
    }

    String name = BIT_VECTOR_VARIABLE_PREFIX + bitVectorVariableCounter;
    bitVectorVariableCounter++;

    if (term != null) {
      bitVectorVariableToName.put(term, name);
    }

    return name;
  }

  public BitVector reconstruct(BitVectorTerm term, Assignment assignment) {
    if (!bitVectorVariableToName.containsKey(term)) {
      return new BitVector(term.getLength());
    }

    String variableName = bitVectorVariableToName.get(term);

    boolean[] bits = new boolean[term.getLength()];
    for (int i = 0; i < term.getLength(); i++) {
      String bitVariableName = variableName + "_" + i;
      try {
        bits[i] = assignment.getValue(bitVariableName);
      } catch (NullPointerException e) {}
    }

    return new BitVector(bits);
  }
}
