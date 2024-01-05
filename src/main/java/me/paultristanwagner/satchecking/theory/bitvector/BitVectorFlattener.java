package me.paultristanwagner.satchecking.theory.bitvector;

import me.paultristanwagner.satchecking.parse.BitVectorExtension;
import me.paultristanwagner.satchecking.parse.BitVectorParenthesisTerm;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicExpression;
import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.*;
import me.paultristanwagner.satchecking.theory.bitvector.term.*;

import java.util.*;

import static me.paultristanwagner.satchecking.parse.BitVectorExtension.extend;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicAnd.and;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicBiConditional.equivalence;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicImplication.implication;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicNegation.negation;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicOr.or;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicVariable.variable;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorEqualConstraint.equal;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorGreaterThanConstraint.greaterThan;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorLessThanConstraint.lessThan;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorAbsoluteValue.absoluteValue;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorAddition.addition;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorConstant.constant;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorProduct.product;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable.bitvector;

public class BitVectorFlattener {

  private static final String BIT_VECTOR_VARIABLE_PREFIX = "bv_";

  private int bitVectorVariableCounter = 0;
  public final Map<BitVectorTerm, String> bitVectorVariableToName = new HashMap<>();
  public final Map<BitVectorTerm, PropositionalLogicExpression> termToExpression =
      new HashMap<>();
  public final Map<BitVectorConstraint, PropositionalLogicExpression> constraintToExpression =
      new HashMap<>();

  public CNF flatten(List<BitVectorConstraint> constraints) {
    List<PropositionalLogicExpression> expressions = new ArrayList<>();
    for (BitVectorConstraint constraint : constraints) {
      PropositionalLogicExpression expression = convertConstraintRecursive(constraint);
      expressions.add(expression);
    }

    PropositionalLogicExpression conjunction = and(expressions);

    return PropositionalLogicParser.tseitin(conjunction);
  }

  private PropositionalLogicExpression convertConstraintRecursive(BitVectorConstraint constraint) {
    return convertConstraint(constraint, true);
  }

  private PropositionalLogicExpression convertConstraintNonRecursive(BitVectorConstraint constraint) {
    return convertConstraint(constraint, false);
  }

  private PropositionalLogicExpression convertConstraint(BitVectorConstraint constraint, boolean convertSubTerms) {
    if (constraintToExpression.containsKey(constraint)) {
      return constraintToExpression.get(constraint);
    }

    PropositionalLogicExpression subTermExpression = null;
    if (convertSubTerms && !constraint.getMaximalProperSubTerms().isEmpty()) {
      subTermExpression = convertTermsRecursive(constraint.getMaximalProperSubTerms());
    }

    PropositionalLogicExpression constraintExpression;
    if (constraint instanceof BitVectorEqualConstraint equalConstraint) {
      constraintExpression = convertEqualConstraint(equalConstraint);
    } else if (constraint instanceof BitVectorUnequalConstraint unequalConstraint) {
      constraintExpression = convertUnequalConstraint(unequalConstraint);
    } else if (constraint instanceof BitVectorLessThanConstraint lessThanConstraint) {
      constraintExpression = convertLessThanConstraint(lessThanConstraint);
    } else if (constraint instanceof BitVectorGreaterThanConstraint greaterThanConstraint) {
      constraintExpression = convertGreaterThanConstraint(greaterThanConstraint);
    } else if (constraint instanceof BitVectorLessThanOrEqualConstraint lessThanOrEqualConstraint) {
      constraintExpression = convertLessThanOrEqualConstraint(lessThanOrEqualConstraint);
    } else if (constraint instanceof BitVectorGreaterThanOrEqualConstraint greaterThanOrEqualConstraint) {
      constraintExpression = convertGreaterThanOrEqualConstraint(greaterThanOrEqualConstraint);
    } else if (constraint instanceof BitVectorBitConstraint bitConstraint) {
      constraintExpression = convertBitSetConstraint(bitConstraint);
    } else {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    PropositionalLogicExpression result = and(constraintExpression, subTermExpression);

    constraintToExpression.put(constraint, result);

    return result;
  }

  private PropositionalLogicExpression convertBitSetConstraint(BitVectorBitConstraint bitConstraint) {
    BitVectorTerm term = bitConstraint.getTerm();
    int bit = bitConstraint.getBit();

    String variableName = bitVectorVariableToName.get(term);

    String bitVariable = variableName + "_" + bit;

    return variable(bitVariable);
  }

  private PropositionalLogicExpression convertEqualConstraint(BitVectorEqualConstraint constraint) {
    BitVectorTerm term1 = constraint.getTerm1();
    BitVectorTerm term2 = constraint.getTerm2();

    String expression1Name = bitVectorVariableToName.get(term1);
    String expression2Name = bitVectorVariableToName.get(term2);

    List<PropositionalLogicExpression> bitEqualFormulas = new ArrayList<>();
    for (int i = 0; i < term1.getLength(); i++) {
      String bitVariable1 = expression1Name + "_" + i;
      String bitVariable2 = expression2Name + "_" + i;

      PropositionalLogicExpression bitEqualFormula =
          equivalence(variable(bitVariable1), variable(bitVariable2));

      bitEqualFormulas.add(bitEqualFormula);
    }

    return and(bitEqualFormulas);
  }

  private PropositionalLogicExpression convertUnequalConstraint(
      BitVectorUnequalConstraint constraint) {
    BitVectorTerm term1 = constraint.getTerm1();
    BitVectorTerm term2 = constraint.getTerm2();

    String expression1Name = bitVectorVariableToName.get(term1);
    String expression2Name = bitVectorVariableToName.get(term2);

    List<PropositionalLogicExpression> bitUnequalFormula = new ArrayList<>();
    for (int i = 0; i < term1.getLength(); i++) {
      String bitVariable1 = expression1Name + "_" + i;
      String bitVariable2 = expression2Name + "_" + i;

      PropositionalLogicExpression bitEqualFormula =
          equivalence(variable(bitVariable1), negation(variable(bitVariable2)));

      bitUnequalFormula.add(bitEqualFormula);
    }

    return or(bitUnequalFormula);
  }

  private PropositionalLogicExpression convertLessThanConstraint(
      BitVectorLessThanConstraint constraint) {
    BitVectorTerm term1 = constraint.getTerm1();
    BitVectorTerm term2 = constraint.getTerm2();

    String expression1Name = bitVectorVariableToName.get(term1);
    String expression2Name = bitVectorVariableToName.get(term2);

    List<PropositionalLogicExpression> caseExpressions = new ArrayList<>();

    if (term1.isSigned()) {
      caseExpressions.add(
          and(
              variable(expression1Name + "_" + (term1.getLength() - 1)),
              negation(variable(expression2Name + "_" + (term2.getLength() - 1)))));
    }

    int highestValueBit = term1.isSigned() ? term1.getLength() - 2 : term1.getLength() - 1;

    for (int r = highestValueBit; r >= 0; r--) {
      String bitVariable1 = expression1Name + "_" + r;
      String bitVariable2 = expression2Name + "_" + r;

      List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();
      for (int s = term1.getLength() - 1; s > r; s--) {
        String bitVariable1_s = expression1Name + "_" + s;
        String bitVariable2_s = expression2Name + "_" + s;

        bitValueFormulas.add(equivalence(variable(bitVariable1_s), variable(bitVariable2_s)));
      }

      bitValueFormulas.add(and(negation(variable(bitVariable1)), variable(bitVariable2)));

      caseExpressions.add(and(bitValueFormulas));
    }

    PropositionalLogicExpression caseDisjunction = or(caseExpressions);
    return and(caseDisjunction);
  }

  private PropositionalLogicExpression convertGreaterThanConstraint(
      BitVectorGreaterThanConstraint constraint) {

    return convertLessThanConstraint(lessThan(constraint.getTerm2(), constraint.getTerm1()));
  }

  private PropositionalLogicExpression convertLessThanOrEqualConstraint(BitVectorLessThanOrEqualConstraint constraint) {
    return or(
        convertLessThanConstraint(lessThan(constraint.getTerm1(), constraint.getTerm2())),
        convertEqualConstraint(equal(constraint.getTerm1(), constraint.getTerm2()))
    );
  }

  private PropositionalLogicExpression convertGreaterThanOrEqualConstraint(BitVectorGreaterThanOrEqualConstraint constraint) {
    return or(
        convertGreaterThanConstraint(greaterThan(constraint.getTerm1(), constraint.getTerm2())),
        convertEqualConstraint(equal(constraint.getTerm1(), constraint.getTerm2()))
    );
  }

  private PropositionalLogicExpression convertTermsNonRecursive(Collection<BitVectorTerm> terms) {
    List<PropositionalLogicExpression> expressions = new ArrayList<>();
    for (BitVectorTerm term : terms) {
      PropositionalLogicExpression expression = convertTermNonRecursive(term);

      expressions.add(expression);
    }

    return and(expressions);
  }

  private PropositionalLogicExpression convertTermsRecursive(Collection<BitVectorTerm> terms) {
    List<PropositionalLogicExpression> expressions = new ArrayList<>();
    for (BitVectorTerm term : terms) {
      PropositionalLogicExpression expression = convertTermRecursive(term);

      expressions.add(expression);
    }

    return and(expressions);
  }

  private PropositionalLogicExpression convertTermRecursive(BitVectorTerm term) {
    return convertTerm(term, true);
  }

  private PropositionalLogicExpression convertTermNonRecursive(BitVectorTerm term) {
    return convertTerm(term, false);
  }

  private PropositionalLogicExpression convertTerm(BitVectorTerm term, boolean convertSubTerms) {
    if (termToExpression.containsKey(term)) {
      return termToExpression.get(term);
    }

    PropositionalLogicExpression subTermExpression = null;
    if (convertSubTerms && !term.getMaximalProperSubTerms().isEmpty()) {
      subTermExpression = convertTermsRecursive(term.getMaximalProperSubTerms());
    }

    if (term instanceof BitVectorVariable) {
      freshBitVectorVariableName(term);
      return null;
    }

    if (term instanceof BitVectorParenthesisTerm parenthesisTerm) {
      BitVectorTerm subTerm = parenthesisTerm.getTerm();
      PropositionalLogicExpression expression = convertTermRecursive(subTerm);
      String variableName = bitVectorVariableToName.get(subTerm);
      bitVectorVariableToName.put(term, variableName);

      return expression;
    }

    PropositionalLogicExpression termExpression;
    if (term instanceof BitVectorConstant constant) {
      termExpression = constantExpression(constant);
    } else if (term instanceof BitVectorExtension extension) {
      termExpression = extensionExpression(extension);
    } else if (term instanceof BitVectorAddition addition) {
      termExpression = additionExpression(addition);
    } else if (term instanceof BitVectorSubtraction subtraction) {
      termExpression = subtractionExpression(subtraction);
    } else if (term instanceof BitVectorProduct product) {
      termExpression = productExpression(product);
    } else if (term instanceof BitVectorDivision division) {
      termExpression = divisionExpression(division);
    } else if (term instanceof BitVectorRemainder remainder) {
      termExpression = remainderExpression(remainder);
    } else if (term instanceof BitVectorLeftShift leftShift) {
      termExpression = leftShiftExpression(leftShift);
    } else if (term instanceof BitVectorRightShift rightShift) {
      termExpression = rightShiftExpression(rightShift);
    } else if (term instanceof BitVectorNegation negation) {
      termExpression = negationExpression(negation);
    } else if (term instanceof BitVectorOr bitwiseOr) {
      termExpression = orExpression(bitwiseOr);
    } else if (term instanceof BitVectorXor bitwiseXor) {
      termExpression = xorExpression(bitwiseXor);
    } else if (term instanceof BitVectorAnd bitwiseAnd) {
      termExpression = andExpression(bitwiseAnd);
    } else if (term instanceof BitVectorAbsoluteValue absoluteValue) {
      termExpression = absoluteValueExpression(absoluteValue);
    } else {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    PropositionalLogicExpression result = and(termExpression, subTermExpression);

    termToExpression.put(term, result);

    return result;
  }

  private PropositionalLogicExpression constantExpression(BitVectorConstant constant) {
    String variableName = freshBitVectorVariableName(constant);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

    for (int i = 0; i < constant.getLength(); i++) {
      String bitVariable = variableName + "_" + i;
      boolean bitValue = constant.getBitVector().getBit(i);

      bitValueFormulas.add(bitValueFormula(bitVariable, bitValue));
    }

    return and(bitValueFormulas);
  }

  private PropositionalLogicExpression extensionExpression(BitVectorExtension extension) {
    BitVectorTerm term = extension.getTerm();

    String variableName = bitVectorVariableToName.get(term);
    bitVectorVariableToName.put(extension, variableName);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

    if (extension.isSigned()) {
      String signBitVariable = variableName + "_" + (term.getLength() - 1);

      for (int i = 0; i < term.getLength() - 1; i++) {
        String bitVariable = variableName + "_" + i;
        String bitResultVariable = variableName + "_" + i;

        bitValueFormulas.add(equivalence(variable(bitResultVariable), variable(bitVariable)));
      }

      for (int i = term.getLength() - 1; i < extension.getLength() - 1; i++) {
        String bitResultVariable = variableName + "_" + i;

        bitValueFormulas.add(equivalence(variable(bitResultVariable), variable(signBitVariable)));
      }

      return and(bitValueFormulas);
    }

    for (int i = term.getLength(); i < extension.getLength(); i++) {
      String bitResultVariable = variableName + "_" + i;

      bitValueFormulas.add(negation(variable(bitResultVariable)));
    }

    return and(bitValueFormulas);
  }

  private PropositionalLogicExpression negationExpression(BitVectorNegation negation) {
    BitVectorTerm term = negation.getTerm();

    String variableName = bitVectorVariableToName.get(term);

    String resultVariable = freshBitVectorVariableName(negation);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

    for (int i = 0; i < term.getLength(); i++) {
      String bitResultVariable = resultVariable + "_" + i;
      String bitTermVariable = variableName + "_" + i;

      bitValueFormulas.add(
          equivalence(variable(bitResultVariable), negation(variable(bitTermVariable))));
    }

    return and(bitValueFormulas);
  }

  private PropositionalLogicExpression orExpression(BitVectorOr bitwiseOr) {
    BitVectorTerm term1 = bitwiseOr.getTerm1();
    BitVectorTerm term2 = bitwiseOr.getTerm2();

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(bitwiseOr);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

    for (int i = 0; i < bitwiseOr.getLength(); i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;
      String bitVariableResult = resultVariable + "_" + i;

      bitValueFormulas.add(
          equivalence(
              variable(bitVariableResult),
              or(
                  variable(bitVariable1),
                  variable(bitVariable2)
              )
          )
      );
    }

    return and(bitValueFormulas);
  }

  private PropositionalLogicExpression xorExpression(BitVectorXor bitwiseXor) {
    BitVectorTerm term1 = bitwiseXor.getTerm1();
    BitVectorTerm term2 = bitwiseXor.getTerm2();

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(bitwiseXor);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

    for (int i = 0; i < bitwiseXor.getLength(); i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;
      String bitVariableResult = resultVariable + "_" + i;

      bitValueFormulas.add(
          equivalence(
              variable(bitVariableResult),
              or(
                  and(variable(bitVariable1), negation(variable(bitVariable2))),
                  and(negation(variable(bitVariable1)), variable(bitVariable2))
              )
          )
      );
    }

    return and(bitValueFormulas);
  }

  private PropositionalLogicExpression andExpression(BitVectorAnd bitwiseAnd) {
    BitVectorTerm term1 = bitwiseAnd.getTerm1();
    BitVectorTerm term2 = bitwiseAnd.getTerm2();

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(bitwiseAnd);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

    for (int i = 0; i < bitwiseAnd.getLength(); i++) {
      String bitVariable1 = variable1 + "_" + i;
      String bitVariable2 = variable2 + "_" + i;
      String bitVariableResult = resultVariable + "_" + i;

      bitValueFormulas.add(
          equivalence(
              variable(bitVariableResult),
              and(
                  variable(bitVariable1),
                  variable(bitVariable2)
              )
          )
      );
    }

    return and(bitValueFormulas);
  }

  private PropositionalLogicExpression additionExpression(BitVectorAddition addition) {
    BitVectorTerm term1 = addition.getTerm1();
    BitVectorTerm term2 = addition.getTerm2();

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(addition);
    String carryVariable = freshAnonymousVariableName();

    List<PropositionalLogicExpression> expressions = new ArrayList<>();

    expressions.add(
        halfAdderFormula(
            variable1 + "_0", variable2 + "_0", resultVariable + "_0", carryVariable + "_0"));

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
              bitCarryVariable));
    }

    return and(expressions);
  }

  private PropositionalLogicExpression subtractionExpression(BitVectorSubtraction subtraction) {
    BitVectorTerm term1 = subtraction.getTerm1();
    BitVectorTerm term2 = subtraction.getTerm2();

    BitVectorTerm constantTerm = constant(1, term2.getLength());
    BitVectorTerm negatedTerm = BitVectorNegation.negation(term2);
    BitVectorTerm twoComplementTerm = addition(negatedTerm, constantTerm);
    BitVectorTerm additionTerm = addition(term1, twoComplementTerm);

    PropositionalLogicExpression additionExpression = convertTermRecursive(additionTerm);
    String additionVariable = bitVectorVariableToName.get(additionTerm);
    bitVectorVariableToName.put(subtraction, additionVariable);

    return and(
        convertTermRecursive(constantTerm),
        convertTermRecursive(negatedTerm),
        convertTermRecursive(twoComplementTerm),
        additionExpression
    );
  }

  private PropositionalLogicExpression absoluteValueExpression(BitVectorAbsoluteValue absoluteValue) {
    BitVectorTerm term = absoluteValue.getTerm();
    String resultVariable = freshBitVectorVariableName(absoluteValue);

    BitVectorTerm constantTerm = constant(1, term.getLength());
    BitVectorTerm negatedTerm = BitVectorNegation.negation(term);
    BitVectorTerm twoComplementTerm = addition(negatedTerm, constantTerm);

    PropositionalLogicExpression constantExpression = convertTermRecursive(constantTerm);
    PropositionalLogicExpression negatedExpression = convertTermNonRecursive(negatedTerm);
    PropositionalLogicExpression twoComplementExpression = convertTermNonRecursive(twoComplementTerm);

    List<PropositionalLogicExpression> positiveCase = new ArrayList<>();
    List<PropositionalLogicExpression> negativeCase = new ArrayList<>();
    for (int i = 0; i < term.getLength(); i++) {
      String bitVariable = bitVectorVariableToName.get(term) + "_" + i;
      String resultBitVariable = resultVariable + "_" + i;
      positiveCase.add(
          equivalence(
              variable(resultBitVariable),
              variable(bitVariable)
          )
      );

      String twoComplementBitVariable = bitVectorVariableToName.get(twoComplementTerm) + "_" + i;
      negativeCase.add(
          equivalence(
              variable(resultBitVariable),
              variable(twoComplementBitVariable)
          )
      );
    }

    String signBitVariable = bitVectorVariableToName.get(term) + "_" + (term.getLength() - 1);

    return and(
        constantExpression,
        negatedExpression,
        twoComplementExpression,
        implication(
            variable(signBitVariable),
            and(negativeCase)
        ),
        implication(
            negation(variable(signBitVariable)),
            and(positiveCase)
        )
    );
  }

  private PropositionalLogicExpression leftShiftExpression(BitVectorLeftShift leftShift) {
    if (leftShift.getTerm2() instanceof BitVectorConstant) {
      return constantLeftShiftExpression(leftShift);
    }

    BitVectorTerm term1 = leftShift.getTerm1();
    BitVectorTerm term2 = leftShift.getTerm2();

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

        String currentRoundBitVariable =
            s < leftShift.getLength() - 1
                ? helperVariable + "_" + (s + 1) + "_" + i
                : bitVariableResult;
        String previousRoundBitVariable = previousRoundVariable + "_" + i;

        if (shift < 0 || i < shift) {
          shifts.add(
              implication(
                  variable(variable2 + "_" + s), negation(variable(currentRoundBitVariable))));
        } else {
          String shiftedPreviousRoundBitVariable = previousRoundVariable + "_" + (i - shift);

          shifts.add(
              implication(
                  variable(variable2 + "_" + s),
                  equivalence(
                      variable(shiftedPreviousRoundBitVariable),
                      variable(currentRoundBitVariable))));
        }

        shifts.add(
            implication(
                negation(variable(variable2 + "_" + s)),
                equivalence(
                    variable(previousRoundBitVariable), variable(currentRoundBitVariable))));
      }
    }

    return and(shifts);
  }

  private PropositionalLogicExpression constantLeftShiftExpression(BitVectorLeftShift leftShift) {
    if (!(leftShift.getTerm2() instanceof BitVectorConstant term2)) {
      throw new IllegalArgumentException("BitVectorLeftShift: term2 must be a constant!");
    }

    BitVectorTerm term1 = leftShift.getTerm1();

    String variable1 = bitVectorVariableToName.get(term1);

    String resultVariable = freshBitVectorVariableName(leftShift);

    List<PropositionalLogicExpression> expressions = new ArrayList<>();

    for (int i = 0; i < leftShift.getLength(); i++) {
      long shift = term2.getBitVector().asLong(); // todo: this might not be correct

      String bitVariableResult = resultVariable + "_" + i;

      if (i < shift) {
        expressions.add(negation(variable(bitVariableResult)));
      } else {
        String bitVariable1 = variable1 + "_" + (i - shift);

        expressions.add(equivalence(variable(bitVariableResult), variable(bitVariable1)));
      }
    }

    return and(expressions);
  }

  private PropositionalLogicExpression constantRightShiftExpression(BitVectorRightShift rightShift) {
    if (!(rightShift.getTerm2() instanceof BitVectorConstant term2)) {
      throw new IllegalArgumentException("BitVectorRightShift: term2 must be a constant!");
    }

    BitVectorTerm term1 = rightShift.getTerm1();
    String variable1 = bitVectorVariableToName.get(term1);

    String resultVariable = freshBitVectorVariableName(rightShift);

    List<PropositionalLogicExpression> expressions = new ArrayList<>();

    for (int i = 0; i < rightShift.getLength(); i++) {
      long shift = term2.getBitVector().asLong(); // todo: this might not be correct

      String bitVariableResult = resultVariable + "_" + i;

      if (i >= rightShift.getLength() - shift) {
        expressions.add(negation(variable(bitVariableResult)));
      } else {
        String bitVariable1 = variable1 + "_" + (i + shift);

        expressions.add(equivalence(variable(bitVariableResult), variable(bitVariable1)));
      }
    }

    return and(expressions);
  }

  private PropositionalLogicExpression rightShiftExpression(BitVectorRightShift rightShift) {
    if (rightShift.getTerm2() instanceof BitVectorConstant) {
      return constantRightShiftExpression(rightShift);
    }

    BitVectorTerm term1 = rightShift.getTerm1();
    BitVectorTerm term2 = rightShift.getTerm2();

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(rightShift);
    String helperVariable = freshAnonymousVariableName();

    List<PropositionalLogicExpression> shifts = new ArrayList<>();

    for (int s = 0; s < rightShift.getLength(); s++) {
      String previousRoundVariable = s > 0 ? helperVariable + "_" + s : variable1;

      for (int i = 0; i < rightShift.getLength(); i++) {
        String bitVariableResult = resultVariable + "_" + i;

        long shift = 1L << s;

        String currentRoundBitVariable =
            s < rightShift.getLength() - 1
                ? helperVariable + "_" + (s + 1) + "_" + i
                : bitVariableResult;
        String previousRoundBitVariable = previousRoundVariable + "_" + i;

        if (shift < 0 || i >= rightShift.getLength() - shift) {
          shifts.add(
              implication(
                  variable(variable2 + "_" + s), negation(variable(currentRoundBitVariable))));
        } else {
          String shiftedPreviousRoundBitVariable = previousRoundVariable + "_" + (i + shift);

          shifts.add(
              implication(
                  variable(variable2 + "_" + s),
                  equivalence(
                      variable(shiftedPreviousRoundBitVariable),
                      variable(currentRoundBitVariable))));
        }

        shifts.add(
            implication(
                negation(variable(variable2 + "_" + s)),
                equivalence(
                    variable(previousRoundBitVariable), variable(currentRoundBitVariable))));
      }
    }

    return and(shifts);
  }

  private PropositionalLogicExpression productExpression(BitVectorProduct product) {
    BitVectorTerm term1 = product.getTerm1();
    BitVectorTerm term2 = product.getTerm2();

    String variable1 = bitVectorVariableToName.get(term1);
    String variable2 = bitVectorVariableToName.get(term2);

    String resultVariable = freshBitVectorVariableName(product);
    String individualProductVariable = freshAnonymousVariableName();
    String carryVariable = freshAnonymousVariableName();
    String helperVariable = freshAnonymousVariableName();

    List<PropositionalLogicExpression> individualProductsList = new ArrayList<>();
    for (int i = 0; i < product.getLength(); i++) {
      for (int j = 0; j < product.getLength(); j++) {
        PropositionalLogicParser.PropositionalLogicBiConditional definition =
            equivalence(
                variable(individualProductVariable + "_" + i + "_" + j),
                and(variable(variable1 + "_" + i), variable(variable2 + "_" + j)));

        individualProductsList.add(definition);
      }
    }

    PropositionalLogicExpression individualProducts = and(individualProductsList);

    PropositionalLogicExpression r0 =
        equivalence(variable(resultVariable + "_0"), variable(individualProductVariable + "_0_0"));

    List<PropositionalLogicExpression> adderList = new ArrayList<>();

    adderList.add(
        halfAdderFormula(
            individualProductVariable + "_1_0",
            individualProductVariable + "_0_1",
            resultVariable + "_1",
            carryVariable + "_1_0"));

    for (int i = 2; i < product.getLength(); i++) {
      adderList.add(
          fullAdderFormula(
              individualProductVariable + "_" + i + "_0",
              individualProductVariable + "_" + (i - 1) + "_1",
              carryVariable + "_" + (i - 1) + "_0",
              helperVariable + "_" + i + "_0",
              carryVariable + "_" + i + "_0"));
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
                  carryVariable + "_" + i + "_" + j));
        } else {
          adderList.add(
              halfAdderFormula(
                  helperVariable + "_" + i + "_" + (j - 1),
                  individualProductVariable + "_" + (i - j - 1) + "_" + (j + 1),
                  resultVariable + "_" + i,
                  carryVariable + "_" + i + "_" + j));
        }
      }
    }

    PropositionalLogicExpression adderExpression = and(adderList);

    return and(individualProducts, r0, adderExpression);
  }

  // todo: add the same logic as for the remainder, try to find a way to avoid the duplication
  private PropositionalLogicExpression divisionExpression(BitVectorDivision division) {
    BitVectorTerm term1 = division.getTerm1();
    BitVectorTerm term2 = division.getTerm2();

    // express that l = (l/r) * r + remainder
    // for this we need to extend all the variables in the formula to 2 * l bits

    BitVectorTerm extendedTerm1 = extend(term1, term1.getLength() * 2);
    BitVectorTerm extendedTerm2 = extend(term2, term1.getLength() * 2);

    freshBitVectorVariableName(division);
    BitVectorTerm extendedCoefficient = extend(division, term1.getLength() * 2);

    String remainderVariable = freshAnonymousVariableName();
    BitVectorVariable remainder = bitvector(remainderVariable, term1.getLength());
    BitVectorExtension extendedRemainder = extend(remainder, term1.getLength() * 2);

    BitVectorTerm product = product(extendedCoefficient, extendedTerm2);
    BitVectorTerm addition = addition(product, extendedRemainder);
    BitVectorEqualConstraint equalConstraint = equal(extendedTerm1, addition);

    PropositionalLogicExpression lessThanExpression;
    BitVectorConstraint lessThanConstraint;
    if (term1.isSigned()) {
      BitVectorTerm remainderAbsolute = absoluteValue(remainder);
      BitVectorTerm term2Absolute = absoluteValue(term2);

      lessThanConstraint = lessThan(remainderAbsolute, term2Absolute);

      lessThanExpression = and(
          convertTermNonRecursive(remainderAbsolute),
          convertTermNonRecursive(term2Absolute),
          convertConstraintNonRecursive(lessThanConstraint)
      );
    } else {
      lessThanConstraint = lessThan(remainder, term2);

      lessThanExpression = convertConstraintNonRecursive(lessThanConstraint);
    }

    PropositionalLogicExpression remainderRestriction = null;
    if (term1.isSigned()) {
      List<PropositionalLogicExpression> zeroCase = new ArrayList<>();
      for (int i = 0; i < remainder.getLength(); i++) {
        String bitVariable = bitVectorVariableToName.get(remainder) + "_" + i;
        zeroCase.add(negation(variable(bitVariable)));
      }

      String signBitVariableTerm1 = bitVectorVariableToName.get(term1) + "_" + (term1.getLength() - 1);
      String signBitVariableRemainder = bitVectorVariableToName.get(remainder) + "_" + (remainder.getLength() - 1);

      remainderRestriction = equivalence(
          or(
              and(zeroCase),
              variable(signBitVariableRemainder)
          ),
          variable(signBitVariableTerm1)
      );
    }

    return and(
        convertTermNonRecursive(extendedCoefficient),
        convertTermNonRecursive(extendedTerm1),
        convertTermNonRecursive(extendedTerm2),
        convertTermNonRecursive(extendedRemainder),
        convertTermNonRecursive(product),
        convertTermNonRecursive(addition),
        convertConstraintNonRecursive(equalConstraint),
        lessThanExpression,
        remainderRestriction
    );
  }

  private PropositionalLogicExpression remainderExpression(BitVectorRemainder remainder) {
    BitVectorTerm term1 = remainder.getTerm1();
    BitVectorTerm term2 = remainder.getTerm2();

    // express that l = (l/r) * r + remainder
    // for this we need to extend all the variables in the formula to 2 * l bits

    BitVectorTerm extendedTerm1 = extend(term1, term1.getLength() * 2);
    BitVectorTerm extendedTerm2 = extend(term2, term1.getLength() * 2);

    String coefficientVariable = freshAnonymousVariableName();
    BitVectorVariable coefficient = bitvector(coefficientVariable, term1.getLength());

    BitVectorTerm extendedCoefficient = extend(coefficient, term1.getLength() * 2);

    freshBitVectorVariableName(remainder);
    BitVectorExtension extendedResult = extend(remainder, term1.getLength() * 2);

    BitVectorTerm product = product(extendedCoefficient, extendedTerm2);
    BitVectorTerm addition = addition(product, extendedResult);
    BitVectorEqualConstraint equalConstraint = equal(extendedTerm1, addition);

    PropositionalLogicExpression lessThanExpression;
    BitVectorConstraint lessThanConstraint;
    if (term1.isSigned()) {
      BitVectorTerm remainderAbsolute = absoluteValue(remainder);
      BitVectorTerm term2Absolute = absoluteValue(term2);

      lessThanConstraint = lessThan(remainderAbsolute, term2Absolute);

      lessThanExpression = and(
          convertTermNonRecursive(remainderAbsolute),
          convertTermNonRecursive(term2Absolute),
          convertConstraintNonRecursive(lessThanConstraint)
      );
    } else {
      lessThanConstraint = lessThan(remainder, term2);

      lessThanExpression = convertConstraintNonRecursive(lessThanConstraint);
    }

    PropositionalLogicExpression remainderRestriction = null;
    if (term1.isSigned()) {
      List<PropositionalLogicExpression> zeroCase = new ArrayList<>();
      for (int i = 0; i < remainder.getLength(); i++) {
        String bitVariable = bitVectorVariableToName.get(remainder) + "_" + i;
        zeroCase.add(negation(variable(bitVariable)));
      }

      String signBitVariableTerm1 = bitVectorVariableToName.get(term1) + "_" + (term1.getLength() - 1);
      String signBitVariableRemainder = bitVectorVariableToName.get(remainder) + "_" + (remainder.getLength() - 1);

      remainderRestriction = or(
          and(zeroCase),
          equivalence(
              variable(signBitVariableRemainder),
              variable(signBitVariableTerm1)
          )
      );
    }

    return and(
        convertTermRecursive(extendedCoefficient),
        convertTermNonRecursive(extendedTerm1),
        convertTermNonRecursive(extendedTerm2),
        convertTermNonRecursive(extendedResult),
        convertTermNonRecursive(product),
        convertTermNonRecursive(addition),
        convertConstraintNonRecursive(equalConstraint),
        lessThanExpression,
        remainderRestriction
    );
  }

  private static PropositionalLogicExpression halfAdderFormula(
      String bitVariable1, String bitVariable2, String bitVariableResult, String carryVariable) {
    PropositionalLogicExpression add =
        equivalence(
            variable(bitVariableResult),
            or(
                and(variable(bitVariable1), negation(variable(bitVariable2))),
                and(negation(variable(bitVariable1)), variable(bitVariable2))));

    PropositionalLogicExpression carry =
        equivalence(variable(carryVariable), and(variable(bitVariable1), variable(bitVariable2)));

    return and(add, carry);
  }

  private static PropositionalLogicExpression fullAdderFormula(
      String bitVariable1,
      String bitVariable2,
      String bitVariable3,
      String bitVariableResult,
      String bitCarryVariable) {
    PropositionalLogicExpression add =
        equivalence(
            variable(bitVariableResult),
            or(
                and(
                    variable(bitVariable1),
                    negation(variable(bitVariable2)),
                    negation(variable(bitVariable3))),
                and(
                    negation(variable(bitVariable1)),
                    variable(bitVariable2),
                    negation(variable(bitVariable3))),
                and(
                    negation(variable(bitVariable1)),
                    negation(variable(bitVariable2)),
                    variable(bitVariable3)),
                and(variable(bitVariable1), variable(bitVariable2), variable(bitVariable3))));

    PropositionalLogicExpression carry =
        equivalence(
            variable(bitCarryVariable),
            or(
                and(
                    variable(bitVariable1),
                    variable(bitVariable2),
                    negation(variable(bitVariable3))),
                and(
                    variable(bitVariable1),
                    negation(variable(bitVariable2)),
                    variable(bitVariable3)),
                and(
                    negation(variable(bitVariable1)),
                    variable(bitVariable2),
                    variable(bitVariable3)),
                and(variable(bitVariable1), variable(bitVariable2), variable(bitVariable3))));

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
    if (term != null && bitVectorVariableToName.containsKey(term)) {
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
      } catch (NullPointerException e) {
      }
    }

    return new BitVector(bits);
  }
}
