package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.PolynomialParser;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.*;
import java.util.stream.Collectors;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;
import static me.paultristanwagner.satchecking.theory.nonlinear.Exponent.exponent;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.IntervalBoundType.CLOSED;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.interval;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.pointInterval;
import static me.paultristanwagner.satchecking.theory.nonlinear.Matrix.matrix;
import static me.paultristanwagner.satchecking.theory.nonlinear.Polynomial.polynomial;

public class MultivariatePolynomial {

  public static void main(String[] args) {
    Parser<MultivariatePolynomial> parser = new PolynomialParser();
    Scanner scanner = new Scanner(System.in);
    System.out.print("p: ");
    MultivariatePolynomial p = parser.parse(scanner.nextLine());
    System.out.print("q: ");
    MultivariatePolynomial q = parser.parse(scanner.nextLine());
    System.out.print("enter variable: ");
    String variable = scanner.nextLine();

    System.out.println("p = " + p);
    System.out.println("q = " + q);

    long start = System.currentTimeMillis();
    System.out.println("Res(p, q, " + variable + ") = " + p.resultant(q, variable));
    System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
  }

  public Map<Exponent, Number> coefficients;
  public List<String> variables;

  private MultivariatePolynomial(Map<Exponent, Number> coefficients, List<String> variables) {
    this.coefficients = coefficients;
    this.variables = variables;
  }

  public static MultivariatePolynomial multivariatePolynomial(
      Map<Exponent, Number> coefficients, List<String> variables) {
    return new MultivariatePolynomial(coefficients, variables);
  }

  public static MultivariatePolynomial constant(Number number) {
    return multivariatePolynomial(Map.of(exponent(), number), new ArrayList<>());
  }

  public static MultivariatePolynomial variable(String variable) {
    return multivariatePolynomial(Map.of(exponent(1), ONE()), List.of(variable));
  }

  public static MultivariatePolynomial monomial(Exponent exponent, List<String> variables) {
    return multivariatePolynomial(Map.of(exponent, ONE()), variables);
  }

  public static MultivariatePolynomial ZERO() {
    return multivariatePolynomial(new HashMap<>(), new ArrayList<>());
  }

  public String highestVariable() {
    int highestVariableIndex = 0;
    for (Exponent exponent : coefficients.keySet()) {
      int variableIndex = exponent.highestNonZeroIndex();
      highestVariableIndex = Math.max(highestVariableIndex, variableIndex);
    }

    return variables.get(highestVariableIndex);
  }

  public int degree(String variable) {
    int variableIndex = this.variables.indexOf(variable);
    int highestExponent = 0;

    if (variableIndex == -1) {
      return 0;
    }

    for (Exponent exponent : this.coefficients.keySet()) {
      if(this.coefficients.get(exponent).isZero()) {
        continue;
      }

      int value = exponent.get(variableIndex);
      highestExponent = Math.max(highestExponent, value);
    }

    return highestExponent;
  }

  public MultivariatePolynomial leadingCoefficient(String variable) {
    List<MultivariatePolynomial> coefficients = this.getCoefficients(variable);
    return coefficients.get(coefficients.size() - 1);
  }

  public Exponent leadingMonomial(String variable) {
    int variableIndex = this.variables.indexOf(variable);
    int highest = -1;

    Exponent highestExponent = null;

    for (Exponent exponent : this.coefficients.keySet()) {
      if(this.coefficients.get(exponent).isZero()) {
        continue;
      }

      if(variableIndex == -1) {
        if(highestExponent == null || exponent.compareTo(highestExponent) > 0) {
          highestExponent = exponent;
        }
        continue;
      }

      int value = exponent.get(variableIndex);
      if (highestExponent == null || value > highest || value == highest && exponent.compareTo(highestExponent) > 0) {
        highest = value;
        highestExponent = exponent;
      }
    }

    if(highestExponent == null) {
      throw new IllegalArgumentException("No leading monomial");
    }

    return highestExponent;
  }

  public boolean isZero() {
    for (Exponent exponent : coefficients.keySet()) {
      if (!coefficients.get(exponent).isZero()) {
        return false;
      }
    }

    return true;
  }

  public boolean isConstant() {
    for (Exponent exponent : coefficients.keySet()) {
      if (!coefficients.get(exponent).isZero() && !exponent.isConstantExponent()) {
        return false;
      }
    }

    return true;
  }

  public MultivariatePolynomial add(MultivariatePolynomial other) {
    Set<String> variablesSet = new HashSet<>(this.variables);
    variablesSet.addAll(other.variables);
    List<String> newVariables = new ArrayList<>(variablesSet);
    Map<Exponent, Number> newCoefficients = new HashMap<>();

    for (Exponent exponent : this.coefficients.keySet()) {
      Exponent thisNewExponent = Exponent.project(exponent, this.variables, newVariables);

      Number c = newCoefficients.getOrDefault(thisNewExponent, Number.ZERO());
      c = c.add(this.coefficients.getOrDefault(exponent, Number.ZERO()));
      newCoefficients.put(thisNewExponent, c);
    }

    for (Exponent exponent : other.coefficients.keySet()) {
      Exponent otherNewExponent = Exponent.project(exponent, other.variables, newVariables);
      Number c = newCoefficients.getOrDefault(otherNewExponent, Number.ZERO());
      c = c.add(other.coefficients.getOrDefault(exponent, Number.ZERO()));
      newCoefficients.put(otherNewExponent, c);
    }

    return multivariatePolynomial(newCoefficients, newVariables);
  }

  public MultivariatePolynomial negate() {
    List<String> variables = new ArrayList<>(this.variables);
    Map<Exponent, Number> coefficients = new HashMap<>();
    for (Exponent exponent : this.coefficients.keySet()) {
      coefficients.put(exponent, this.coefficients.get(exponent).negate());
    }

    return multivariatePolynomial(coefficients, variables);
  }

  public MultivariatePolynomial subtract(MultivariatePolynomial other) {
    return this.add(other.negate());
  }

  public MultivariatePolynomial multiply(MultivariatePolynomial other) {
    Set<String> variablesSet = new HashSet<>(this.variables);
    variablesSet.addAll(other.variables);
    List<String> newVariables = new ArrayList<>(variablesSet);
    Map<Exponent, Number> newCoefficients = new HashMap<>();

    for (Exponent exponent : this.coefficients.keySet()) {
      Exponent projectedExponent = Exponent.project(exponent, this.variables, newVariables);
      for (Exponent otherExponent : other.coefficients.keySet()) {
        Exponent projectedOtherExponent = Exponent.project(otherExponent, other.variables, newVariables);
        Exponent newExponent = projectedExponent.add(projectedOtherExponent);

        Number c = newCoefficients.getOrDefault(newExponent, Number.ZERO());
        c = c.add(this.coefficients.get(exponent).multiply(other.coefficients.get(otherExponent)));

        newCoefficients.put(newExponent, c);
      }
    }

    return multivariatePolynomial(newCoefficients, newVariables);
  }

  // todo: use fast exponentiation
  public MultivariatePolynomial pow(int exponent) {
    if(exponent < 0) {
      if(this.isConstant()) {
        Number c = this.coefficients.getOrDefault(getLeadMonomial(), Number.ZERO());
        return constant(c.pow(-exponent));
      }

      throw new IllegalArgumentException("Exponent must be non-negative");
    }

    if (exponent == 0) {
      return constant(ONE());
    }

    MultivariatePolynomial result = this;
    for (int i = 1; i < exponent; i++) {
      result = result.multiply(this);
    }

    return result;
  }

  public Exponent getLeadMonomial() {
    Exponent leadMonomial = null;

    for (Exponent exponent : coefficients.keySet()) {
      if (coefficients.get(exponent).isZero()) {
        continue;
      }

      if (leadMonomial == null || leadMonomial.compareTo(exponent) < 0) {
        leadMonomial = exponent;
      }
    }

    if (leadMonomial == null) {
      return exponent();
    }

    return leadMonomial;
  }

  public List<MultivariatePolynomial> pseudoDivision(MultivariatePolynomial divisor, String variable) {
    if (divisor.isZero()) {
      throw new IllegalArgumentException("Divisor must not be zero");
    }

    if (this.isZero()) {
      return List.of(ZERO(), ZERO());
    }

    int a = this.degree(variable);
    int b = divisor.degree(variable);

    List<MultivariatePolynomial> bCoefficients = divisor.getCoefficients(variable);
    MultivariatePolynomial bLeadCoefficient = bCoefficients.get(b);

    MultivariatePolynomial dividend = bLeadCoefficient.pow(a - b + 1).multiply(this);

    return dividend.divide(divisor, variable);
  }

  public List<MultivariatePolynomial> divide(MultivariatePolynomial divisor, String variable) {
    if (divisor.isZero()) {
      throw new IllegalArgumentException("Divisor must not be zero");
    }

    if (this.isZero()) {
      return List.of(ZERO(), ZERO());
    }

    if (this.degree(variable) < divisor.degree(variable)) {
      return List.of(ZERO(), this);
    }

    Exponent divisorLM = divisor.leadingMonomial(variable);
    Number leadingCoefficient = divisor.coefficients.get(divisorLM);

    Set<String> variablesSet = new HashSet<>(this.variables);
    variablesSet.addAll(divisor.variables);
    List<String> newVariables = new ArrayList<>(variablesSet);

    MultivariatePolynomial quotient = ZERO();
    MultivariatePolynomial remainder = this;
    while(!remainder.isZero() && remainder.degree(variable) >= divisor.degree(variable)) {
      Exponent remainderLM = remainder.leadingMonomial(variable);

      Exponent projectedDivisorLM = Exponent.project(divisorLM, divisor.variables, newVariables);
      Exponent projectedRemainderLM = Exponent.project(remainderLM, remainder.variables, newVariables);

      if(!projectedDivisorLM.divides(projectedRemainderLM)) {
        break;
      }

      Number highestCoefficient = remainder.coefficients.get(remainderLM);

      MultivariatePolynomial monomial = monomial(projectedRemainderLM.subtract(projectedDivisorLM), new ArrayList<>(this.variables));

      Number highestCoefficientDivided = highestCoefficient.divide(leadingCoefficient);
      if(!highestCoefficientDivided.isInteger()) {
        break;
      }

      MultivariatePolynomial factor = monomial.multiply(constant(highestCoefficientDivided));

      quotient = quotient.add(factor);
      remainder = remainder.subtract(divisor.multiply(factor));
    }

    return List.of(quotient, remainder);
  }

  // todo: clean this up
  public MultivariatePolynomial resultant(MultivariatePolynomial other, String variable) {
    System.out.println("computing resultant of " + this + " and " + other + " with respect to " + variable);
    int n = this.degree(variable);
    int m = other.degree(variable);

    if(n < m) {
      return other.resultant(this, variable);
    }

    if(this.isZero() || other.isZero()) {
      return ZERO();
    }

    int d = n - m;
    MultivariatePolynomial b = (d + 1) % 2 == 0 ? constant(ONE()) : constant(ONE().negate());

    List<MultivariatePolynomial> R = new ArrayList<>();
    R.add(this);
    R.add(other);

    MultivariatePolynomial h = this.pseudoDivision(other, variable).get(1);
    h = h.multiply(b);

    List<MultivariatePolynomial> coefficients = other.getCoefficients(variable);
    MultivariatePolynomial leadCoefficient = coefficients.get(coefficients.size() - 1);

    MultivariatePolynomial c = leadCoefficient.pow(d);

    c = c.negate();

    MultivariatePolynomial f = this;
    MultivariatePolynomial g = other;
    while(!h.isZero()) {
      int k = h.degree(variable);

      R.add(h);

      f = g;
      g = h;
      int temp = m;
      m = k;
      d = temp - k;

      System.out.println("f = " + f);
      System.out.println("g = " + g);
      System.out.println("m = " + m);
      System.out.println("d = " + d);

      System.out.println("c = " + c);
      System.out.println("c ^ d = " + c.pow(d));

      b = leadCoefficient.negate().multiply(c.pow(d));

      System.out.println("b = " + b);

      h = f.pseudoDivision(g, variable).get(1);
      System.out.println("pseudo remainder = " + h);
      h = h.divide(b, variable).get(0);
      System.out.println("h = " + h);

      List<MultivariatePolynomial> hCoefficients = h.getCoefficients(variable);
      leadCoefficient = hCoefficients.get(hCoefficients.size() - 1);

      if(d > 0) {
        MultivariatePolynomial p = leadCoefficient.negate().pow(d);
        MultivariatePolynomial q = c.pow(d - 1);
        c = p.divide(q, variable).get(0);
      } else {
        c = leadCoefficient.negate();
      }
    }

    System.out.println("DONE");

    return R.get(R.size() - 1);
  }

  public List<MultivariatePolynomial> getCoefficients(String variable) {
    List<String> newVariables = new ArrayList<>(variables);
    newVariables.remove(variable);

    int highestExponent = degree(variable);

    MultivariatePolynomial[] coefficientsArray = new MultivariatePolynomial[highestExponent + 1];
    Arrays.fill(coefficientsArray, ZERO());

    int variableIndex = variables.indexOf(variable);
    for (Exponent exponent : this.coefficients.keySet()) {
      if(this.coefficients.get(exponent).isZero()) {
        continue;
      }

      int variableExponent = variableIndex != -1 ? exponent.get(variableIndex) : 0;

      Number c = this.coefficients.get(exponent);
      Exponent newExponent = Exponent.project(exponent, this.variables, newVariables);

      Map<Exponent, Number> monomialCoefficients = new HashMap<>(Map.of(newExponent, c));
      MultivariatePolynomial monomial = multivariatePolynomial(monomialCoefficients, newVariables);

      coefficientsArray[variableExponent] = coefficientsArray[variableExponent].add(monomial);
    }

    return Arrays.stream(coefficientsArray).collect(Collectors.toList());
  }

  public MultivariatePolynomial derivative(String variable) {
    int variableIndex = this.variables.indexOf(variable);

    if (variableIndex == -1) {
      return ZERO();
    }

    List<String> newVariables = new ArrayList<>(this.variables);
    Map<Exponent, Number> coefficients = new HashMap<>();
    for (Exponent exponent : this.coefficients.keySet()) {
      if (exponent.get(variableIndex) == 0) {
        continue;
      }

      List<Integer> exponentValues = new ArrayList<>(exponent.getValues());
      exponentValues.set(variableIndex, exponent.get(variableIndex) - 1);
      Exponent newExponent = exponent(exponentValues);

      coefficients.put(newExponent, this.coefficients.get(exponent));
    }

    return multivariatePolynomial(coefficients, newVariables);
  }

  public MultivariatePolynomial resultantOld(MultivariatePolynomial other, String variable) {
    List<String> newVariables = new ArrayList<>(variables);
    newVariables.remove(variable);

    List<MultivariatePolynomial> thisCoefficients = this.getCoefficients(variable);
    List<MultivariatePolynomial> otherCoefficients = other.getCoefficients(variable);

    Map<List<Integer>, MultivariatePolynomial> entries = new HashMap<>();

    for (int i = 0; i < otherCoefficients.size() - 1; i++) {
      for (int j = 0; j < thisCoefficients.size(); j++) {
        if (thisCoefficients.get(j).isZero()) {
          continue;
        }

        entries.put(List.of(i, i + j), thisCoefficients.get(j));
      }
    }

    for (int i = 0; i < thisCoefficients.size() - 1; i++) {
      for (int j = 0; j < otherCoefficients.size(); j++) {
        if (otherCoefficients.get(j).isZero()) {
          continue;
        }

        entries.put(List.of(otherCoefficients.size() + i - 1, i + j), otherCoefficients.get(j));
      }
    }

    int n = thisCoefficients.size() + otherCoefficients.size() - 2;
    Matrix sylvesterMatrix = matrix(newVariables, n, n, entries);

    return sylvesterMatrix.determinant();
  }

  public MultivariatePolynomial discriminant(String variable) {
    MultivariatePolynomial derivative = this.derivative(variable);

    return this.resultant(derivative, variable);
  }

  public MultivariatePolynomial substitute(Map<String, RealAlgebraicNumber> substitution) {
    MultivariatePolynomial current = this;
    for (String variable : substitution.keySet()) {
      RealAlgebraicNumber value = substitution.get(variable);
      int variableIndex = current.variables.indexOf(variable);

      if (variableIndex == -1) {
        continue;
      }

      if (!value.isNumeric()) {
        Polynomial ranPolynomial = value.getPolynomial();
        current = current.resultant(ranPolynomial.toMultivariatePolynomial(variable), variable); // todo: this introduces incorrect roots
        continue;
      }

      Number rationalValue = value.numericValue();

      List<String> newVariables = new ArrayList<>(current.variables);
      newVariables.remove(variable);

      Map<Exponent, Number> newCoefficients = new HashMap<>();

      for (Exponent exponent : current.coefficients.keySet()) {
        int power = exponent.get(variableIndex);

        Number c = current.coefficients.get(exponent);
        Exponent newExponent = Exponent.project(exponent, current.variables, newVariables);

        if (power != 0) {
          c = c.multiply(rationalValue.pow(power));
        }

        Number prev = newCoefficients.getOrDefault(newExponent, Number.ZERO());
        newCoefficients.put(newExponent, prev.add(c));
      }

      current = multivariatePolynomial(newCoefficients, newVariables);
    }

    return current;
  }

  public Interval evaluate(Map<String, Interval> substitution) {
    Interval interval = null;
    for (Exponent exponent : coefficients.keySet()) {
      if (coefficients.get(exponent).isZero()) {
        continue;
      }

      Number coefficient = coefficients.get(exponent);
      Interval monomialInterval = null;
      for (int i = 0; i < exponent.getValues().size(); i++) {
        String variable = variables.get(i);
        int power = exponent.get(i);
        Interval variableInterval = substitution.get(variable);

        if (variableInterval == null) {
          throw new IllegalArgumentException("No interval for variable " + variable);
        }

        if (monomialInterval == null) {
          monomialInterval = variableInterval.pow(power);
        } else {
          monomialInterval = monomialInterval.multiply(variableInterval.pow(power));
        }
      }

      if (interval == null) {
        interval = monomialInterval.multiply(coefficient);
      } else {
        interval = interval.add(monomialInterval.multiply(coefficient));
      }
    }

    return interval;
  }

  public int evaluateSign(Map<String, RealAlgebraicNumber> substitution) {
    Map<String, RealAlgebraicNumber> numericSubstitutions = new HashMap<>();
    substitution.forEach((variable, ran) -> {
      if (ran.isNumeric()) {
        numericSubstitutions.put(variable, ran);
      }
    });

    MultivariatePolynomial substituted = this.substitute(numericSubstitutions);

    if (substituted.isZero()) {
      return 0;
    } else if (substituted.isConstant()) {
      Exponent constantExponent = substituted.coefficients.keySet().stream().filter(exponent -> !substituted.coefficients.get(exponent).isZero()).findAny().orElseThrow();
      return substituted.coefficients.get(constantExponent).sign();
    }

    Map<String, RealAlgebraicNumber> algebraicSubstitutions = new HashMap<>();
    substitution.forEach((variable, ran) -> {
      if (!ran.isNumeric()) {
        algebraicSubstitutions.put(variable, ran);
      }
    });

    if(algebraicSubstitutions.isEmpty()) {
      throw new IllegalArgumentException("No algebraic substitutions");
    }

    MultivariatePolynomial current = substituted;
    Iterator<String> iterator = algebraicSubstitutions.keySet().iterator();
    String firstVariable = iterator.next();
    String freshVariable = firstVariable + "'"; // todo: make sure this is fresh

    System.out.println("current = " + current);
    RealAlgebraicNumber firstSubstitution = algebraicSubstitutions.get(firstVariable);
    System.out.println("firstSubstitution = " + firstSubstitution);
    System.out.println("Resultant[" + freshVariable + " - (" + current + "), " + firstSubstitution.getPolynomial().toMultivariatePolynomial(firstVariable) + ", " + firstVariable + "] = ");
    current = variable(freshVariable).subtract(current).resultant(firstSubstitution.getPolynomial().toMultivariatePolynomial(firstVariable), firstVariable);
    System.out.println(current);

    while(iterator.hasNext()) {
      String variable = iterator.next();
      RealAlgebraicNumber ran = algebraicSubstitutions.get(variable);
      current = current.resultant(ran.getPolynomial().toMultivariatePolynomial(variable), variable);
    }

    Polynomial univariate = current.toUnivariatePolynomial();
    System.out.println("this = " + this);
    System.out.println("substitution = " + substitution);
    System.out.println("univariate = " + univariate);

    while(true) {
      Map<String, Interval> intervalSubstitution = new HashMap<>();
      for (String variable : substitution.keySet()) {
        RealAlgebraicNumber ran = substitution.get(variable);

        if(ran.isNumeric()) {
          intervalSubstitution.put(variable, pointInterval(ran.numericValue()));
        } else {
          intervalSubstitution.put(variable, interval(ran.getLowerBound(), ran.getUpperBound(), CLOSED, CLOSED));
        }
      }

      Interval res_I = this.evaluate(intervalSubstitution);
      System.out.println("this = " + this);
      System.out.println("intervalSubstitution = " + intervalSubstitution);
      System.out.println("res_I = " + res_I);

      if(!res_I.containsZero()) {
        return res_I.getLowerBound().numericValue().sign();
      }

      Set<RealAlgebraicNumber> testRoots = univariate.isolateRoots(res_I.getLowerBound().numericValue(), res_I.getUpperBound().numericValue());
      System.out.println("testRoots = " + testRoots);
      if(testRoots.size() == 1) {
        RealAlgebraicNumber root = testRoots.iterator().next();
        return root.sign();
      }

      algebraicSubstitutions.values().forEach(RealAlgebraicNumber::refine);
    }
  }

  public void prune() {
    List<String> unusedVariables = new ArrayList<>();
    for (String variable : variables) {
      int degree = degree(variable);
      if (degree == 0) {
        unusedVariables.add(variable);
      }
    }

    List<String> newVariables = new ArrayList<>(variables);
    newVariables.removeAll(unusedVariables);

    Map<Exponent, Number> newCoefficients = new HashMap<>();
    for (Exponent exponent : coefficients.keySet()) {
      if(coefficients.get(exponent).isZero()) {
        continue;
      }

      System.out.println("exponent = " + exponent);
      Exponent newExponent = Exponent.project(exponent, variables, newVariables);
      newCoefficients.put(newExponent, coefficients.get(exponent));
    }

    this.variables = newVariables;
    this.coefficients = newCoefficients;
  }

  public Polynomial toUnivariatePolynomial() {
    prune();

    if (this.variables.size() > 1) {
      throw new IllegalArgumentException("Not a univariate polynomial");
    }

    if (this.variables.isEmpty()) {
      return polynomial(coefficients.getOrDefault(exponent(), Number.ZERO()));
    }

    int degree = 0;
    for (Exponent exponent : coefficients.keySet()) {
      degree = Math.max(degree, exponent.get(0));
    }

    Number[] coefficientsArray = new Number[degree + 1];
    for (int i = 0; i <= degree; i++) {
      Exponent exponent = exponent(i);
      coefficientsArray[i] = coefficients.getOrDefault(exponent, Number.ZERO());
    }

    return polynomial(coefficientsArray);
  }

  public Number getCoefficient(Exponent exponent) {
    return coefficients.getOrDefault(exponent, Number.ZERO());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Iterator<Exponent> iterator = coefficients.keySet().iterator();
         iterator.hasNext(); ) {
      Exponent exponent = iterator.next();
      Number coefficient = coefficients.get(exponent);

      if (coefficient.isZero()) {
        continue;
      }

      sb.append(coefficient);

      if (!exponent.isConstantExponent()) {
        sb.append(" * ");
      }

      for (int i = 0; i < variables.size(); i++) {
        if (exponent.get(i) == 0) {
          continue;
        }

        sb.append(variables.get(i));
        if (exponent.get(i) > 1) {
          sb.append("^").append(exponent.get(i));
        }

        if (i < variables.size() - 1 && exponent.get(i + 1) != 0) {
          sb.append(" * ");
        }
      }

      if (iterator.hasNext()) {
        sb.append(" + ");
      }
    }

    if (sb.isEmpty()) {
      return "0";
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MultivariatePolynomial that = (MultivariatePolynomial) o;
    return Objects.equals(coefficients, that.coefficients)
        && Objects.equals(variables, that.variables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coefficients, variables);
  }
}
