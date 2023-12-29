package me.paultristanwagner.satchecking.theory.bitvector;

import static me.paultristanwagner.satchecking.parse.BitVectorExtension.extend;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicAnd.and;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicBiConditional.equivalence;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicImplication.implication;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicNegation.negation;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicOr.or;
import static me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicVariable.variable;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorEqualConstraint.equal;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorLessThanConstraint.lessThan;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorAddition.addition;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorConstant.constant;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorProduct.product;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable.bitvector;

import java.util.*;
import me.paultristanwagner.satchecking.parse.BitVectorExtension;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicExpression;
import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorConstraint;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorEqualConstraint;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorLessThanConstraint;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorUnequalConstraint;
import me.paultristanwagner.satchecking.theory.bitvector.term.*;

public class BitVectorFlattener {

  private static final String BIT_VECTOR_VARIABLE_PREFIX = "bv_";

  private int bitVectorVariableCounter = 0;
  public final Map<BitVectorTerm, String> bitVectorVariableToName = new HashMap<>();
  public final Map<BitVectorTerm, PropositionalLogicExpression> termToExpression =
      new HashMap<>();
  public final Map<BitVectorConstraint, PropositionalLogicExpression> constraintToExpression =
      new HashMap<>();

  /*
   * todo:
   *  - Implement binary bitvector terms : DONE
   *  - Implement unary bitvector terms : MAYBE NOT NEEDED
   *  - Change conversion to store expressions in a map
   *
   *  - If we convert a term, we need to store the variable name in a map
   */

  public CNF flatten(List<BitVectorConstraint> constraints) {
    List<PropositionalLogicExpression> expressions = new ArrayList<>();
    Set<BitVectorTerm> terms = new HashSet<>();
    for (BitVectorConstraint constraint : constraints) {
      convertConstraint(constraint);

      PropositionalLogicExpression expression = constraintToExpression.get(constraint);
      expressions.add(expression);

      terms.addAll(constraint.getTerms());
    }

    for (BitVectorTerm term : terms) {
      PropositionalLogicExpression expression = termToExpression.get(term);
      expressions.add(expression);
    }

    PropositionalLogicExpression conjunction = and(expressions);

    return PropositionalLogicParser.tseitin(conjunction);
  }

  private void convertConstraint(BitVectorConstraint constraint) {
    if(constraintToExpression.containsKey(constraint)) {
      return;
    }

    PropositionalLogicExpression expression;
    if (constraint instanceof BitVectorEqualConstraint equalConstraint) {
      expression = convertEqualConstraint(equalConstraint);
    } else if (constraint instanceof BitVectorUnequalConstraint unequalConstraint) {
      expression = convertUnequalConstraint(unequalConstraint);
    } else if (constraint instanceof BitVectorLessThanConstraint lessThanConstraint) {
      expression = convertLessThanConstraint(lessThanConstraint);
    } else {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    constraintToExpression.put(constraint, expression);
  }

  private PropositionalLogicExpression convertEqualConstraint(BitVectorEqualConstraint constraint) {
    BitVectorTerm term1 = constraint.getTerm1();
    BitVectorTerm term2 = constraint.getTerm2();

    convertTerm(term1);
    convertTerm(term2);

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

    convertTerm(term1);
    convertTerm(term2);

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

    convertTerm(term1);
    convertTerm(term2);

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

  private void convertTerms(Collection<BitVectorTerm> terms) {
    for (BitVectorTerm term : terms) {
      convertTerm(term);
    }
  }

  private void convertTerm(BitVectorTerm term) {
    if (termToExpression.containsKey(term)) {
      return;
    }

    convertTerms(term.getProperSubTerms());
    convertTerms(term.getDefiningTerms());

    PropositionalLogicExpression expression;

    if(term instanceof BitVectorVariable) {
      freshBitVectorVariableName(term);
      return;
    }

    if (term instanceof BitVectorConstant constant) {
      expression = constantExpression(constant);
    } else if (term instanceof BitVectorExtension extension) {
      expression = extensionExpression(extension);
    } else if (term instanceof BitVectorAddition addition) {
      expression = additionExpression(addition);
    } else if (term instanceof BitVectorSubtraction subtraction) {
      expression = convertSubtraction(subtraction);
    } else if (term instanceof BitVectorProduct product) {
      expression = productExpression(product);
    } else if (term instanceof BitVectorRemainder remainder) {
      expression = convertRemainder(remainder);
    } else if (term instanceof BitVectorLeftShift leftShift) {
      expression = convertLeftShift(leftShift);
    } else if (term instanceof BitVectorNegation negation) {
      expression = negationExpression(negation);
    } else {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    termToExpression.put(term, expression);
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

  // todo: add support for signed terms
  private PropositionalLogicExpression extensionExpression(BitVectorExtension extension) {
    BitVectorTerm term = extension.getTerm();

    String variableName = bitVectorVariableToName.get(term);
    bitVectorVariableToName.put(extension, variableName);

    List<PropositionalLogicExpression> bitValueFormulas = new ArrayList<>();

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

  private PropositionalLogicExpression convertSubtraction(BitVectorSubtraction subtraction) {
    BitVectorTerm additionTerm = subtraction.getAdditionTerm();
    String additionVariable = bitVectorVariableToName.get(additionTerm);
    bitVectorVariableToName.put(subtraction, additionVariable);

    List<PropositionalLogicExpression> expressions = new ArrayList<>();

    expressions.add(termToExpression.get(additionTerm));

    for(BitVectorTerm definingTerm : subtraction.getDefiningTerms()) {
      PropositionalLogicExpression expression = termToExpression.get(definingTerm);
      expressions.add(expression);
    }

    return and(expressions);
  }

  private PropositionalLogicExpression convertLeftShift(BitVectorLeftShift leftShift) {
    if (leftShift.getTerm2() instanceof BitVectorConstant) {
      return convertConstantLeftShift(leftShift);
    }

    BitVectorTerm term1 = leftShift.getTerm1();
    BitVectorTerm term2 = leftShift.getTerm2();

    if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException(
          "BitVectorLeftShift: term1 and term2 must have the same length!");
    }

    convertTerm(term1);
    convertTerm(term2);

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

  private PropositionalLogicExpression convertConstantLeftShift(BitVectorLeftShift leftShift) {
    if (!(leftShift.getTerm2() instanceof BitVectorConstant term2)) {
      throw new IllegalArgumentException("BitVectorLeftShift: term2 must be a constant!");
    }

    BitVectorTerm term1 = leftShift.getTerm1();

    convertTerm(term1);
    convertTerm(term2);

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

  private PropositionalLogicExpression productExpression(BitVectorProduct product) {
    BitVectorTerm term1 = product.getTerm1();
    BitVectorTerm term2 = product.getTerm2();

    convertTerm(term1);
    convertTerm(term2);

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

  // todo: this works for unsigned terms but is very hacky :(
  // todo: idea: turn extension into an actual extension and perform proper logic for signed terms
  private PropositionalLogicExpression convertRemainder(BitVectorRemainder remainder) {
    BitVectorTerm term1 = remainder.getTerm1();
    BitVectorTerm term2 = remainder.getTerm2();

    if (term1.getLength() != term2.getLength()) {
      throw new IllegalArgumentException(
          "BitVectorAddition: term1 and term2 must have the same length!");
    }

    // express that l = (l/r) * r + remainder
    // for this we need to extend all the variables in the formula to 2 * l bits

    BitVectorTerm extendedTerm1 = extend(term1, term1.getLength() * 2);
    BitVectorTerm extendedTerm2 = extend(term2, term1.getLength() * 2);

    String coefficientVariable = freshAnonymousVariableName();
    BitVectorVariable coefficient = bitvector(coefficientVariable, term1.getLength());
    BitVectorTerm extendedCoefficient = extend(coefficient, term1.getLength() * 2);

    String extraRemainderVariable = freshAnonymousVariableName();
    BitVectorVariable extraRemainder = bitvector(extraRemainderVariable, term1.getLength());
    BitVectorTerm extendedRemainder = extend(extraRemainder, term1.getLength() * 2);

    BitVectorTerm product = product(extendedCoefficient, extendedTerm2);
    BitVectorTerm extendedAddition = addition(product, extendedRemainder);
    BitVectorEqualConstraint equalConstraint = equal(extendedTerm1, extendedAddition);

    PropositionalLogicExpression equalExpression = convertEqualConstraint(equalConstraint);

    BitVectorLessThanConstraint lessThanConstraint = lessThan(extendedRemainder, extendedTerm2);
    PropositionalLogicExpression lessThanExpression = convertLessThanConstraint(lessThanConstraint);

    String extraRemainderVariableName = bitVectorVariableToName.get(extraRemainder);
    String resultVariable = freshBitVectorVariableName(remainder);
    List<PropositionalLogicExpression> bitEqualFormulas = new ArrayList<>();
    for (int i = 0; i < term1.getLength(); i++) {
      String bitVariable = resultVariable + "_" + i;
      String extendedBitVariable = extraRemainderVariableName + "_" + i;

      bitEqualFormulas.add(equivalence(variable(bitVariable), variable(extendedBitVariable)));
    }

    return and(equalExpression, lessThanExpression, and(bitEqualFormulas));
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
