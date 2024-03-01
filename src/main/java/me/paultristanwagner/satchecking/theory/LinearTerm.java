package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;

public class LinearTerm {

  protected final Set<String> variables;
  protected final Map<String, Number> coefficients;
  private Number constant;

  public LinearTerm() {
    this.variables = new HashSet<>();
    this.coefficients = new HashMap<>();
    this.constant = ZERO();
  }

  public LinearTerm(LinearTerm term) {
    this.variables = new HashSet<>(term.variables);
    this.coefficients = new HashMap<>(term.coefficients);
    this.constant = term.constant;
  }

  public void setCoefficient(String variable, Number coefficient) {
    variables.add(variable);
    coefficients.put(variable, coefficient);

    if(coefficients.get(variable).equals(ZERO())) {
      coefficients.remove(variable);
      variables.remove(variable);
    }
  }

  public void addCoefficient(String variable, Number coefficient) {
    if (coefficients.containsKey(variable)) {
      coefficients.put(variable, coefficients.get(variable).add(coefficient));
    } else {
      setCoefficient(variable, coefficient);
    }

    if(coefficients.get(variable).equals(ZERO())) {
      coefficients.remove(variable);
      variables.remove(variable);
    }
  }

  public Set<String> getVariables() {
    return variables;
  }

  public Map<String, Number> getCoefficients() {
    return coefficients;
  }

  public Number getConstant() {
    return constant;
  }

  public void setConstant(Number constant) {
    this.constant = constant;
  }

  public void addConstant(Number value) {
    this.constant = this.constant.add(value);
  }

  public LinearTerm add(LinearTerm term) {
    LinearTerm result = new LinearTerm(this);
    term.coefficients.forEach(result::addCoefficient);
    result.addConstant(term.constant);
    return result;
  }

  public LinearTerm subtract(LinearTerm term) {
    LinearTerm result = new LinearTerm(this);
    term.coefficients.forEach((variable, coefficient) -> result.addCoefficient(variable, coefficient.negate()));
    result.addConstant(term.constant.negate());
    return result;
  }

  public LinearTerm offset(String variable, String substitute, Number offset) {
    LinearTerm term = new LinearTerm(this);
    if (!coefficients.containsKey(variable)) {
      return this;
    }

    Number coeff = coefficients.get(variable);
    term.variables.remove(variable);
    term.coefficients.remove(variable);
    term.setCoefficient(substitute, coeff);

    term.constant = constant.subtract(
        coeff.multiply(offset)
    );

    return term;
  }

  public LinearTerm positiveNegativeSubstitute(
      String variable, String positive, String negative) {
    Number coeff = coefficients.get(variable);

    LinearTerm term = new LinearTerm(this);
    term.variables.remove(variable);
    term.coefficients.remove(variable);
    term.setCoefficient(positive, coeff);
    term.setCoefficient(negative, coeff.negate());

    return term;
  }

  public Number evaluate(VariableAssignment<Number> assignment) {
    Number result = ZERO();
    for (String variable : variables) {
      Number summand = coefficients.get(variable).multiply(assignment.getAssignment(variable));
      result = result.add(summand);
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    coefficients.forEach(
        (variable, coefficient) -> {
          if (coefficient.isNonNegative()) {
            if (!sb.isEmpty()) {
              sb.append("+");
            }
          } else {
            sb.append("-");
          }

          Number absolute = coefficient.abs();
          if (!absolute.isOne()) {
            sb.append(absolute);
          }

          sb.append(variable);
        });

    if(sb.isEmpty()) {
      sb.append("0");
    }

    return sb.toString();
  }
}
