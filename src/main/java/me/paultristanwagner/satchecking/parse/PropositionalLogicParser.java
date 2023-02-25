package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PropositionalLogicParser
    implements Parser<PropositionalLogicParser.PropositionalLogicExpression> {

  /*
   Grammar for not, and, or, parenthesis, and variables:
     S -> B
       -> S <-> B
     B -> I
       -> B -> I
     I -> C
       -> I | C
     C -> N
       -> C & N
     N -> P
       -> ~P
     P -> VARIABLE
       -> (S)
  */

  @Override
  public PropositionalLogicExpression parse(String string) {
    return parse(string, new AtomicInteger(string.length() - 1));
  }
  
  @Override
  public PropositionalLogicExpression parse(String string, AtomicInteger index) {
    PropositionalLogicExpression expression = S(string, index);
    if (index.get() != -1) {
      char c = string.charAt(index.get());
      throw new SyntaxError("Unexpected character '" + c + "'", string, index.get());
    }

    return expression;
  }

  private static PropositionalLogicExpression S(String string, AtomicInteger index) {
    PropositionalLogicExpression last = B(string, index);
    if (Parser.previousProperChar(string, index) == '>') {
      if (Parser.previousProperChar(string, index) != '-') {
        throw new IllegalArgumentException("Expected '-' before '>'");
      }
      if (Parser.previousProperChar(string, index) == '<') {
        PropositionalLogicExpression first = S(string, index);
        return new PropositionalLogicBiConditional(first, last);
      }
      index.addAndGet(2);
    }
  
    index.incrementAndGet();
    return last;
  }

  private static PropositionalLogicExpression B(String string, AtomicInteger index) {
    PropositionalLogicExpression last = I(string, index);
    if (Parser.previousProperChar(string, index) == '>') {
      if (Parser.previousProperChar(string, index) != '-') {
        throw new IllegalArgumentException("Expected '-' before '>'");
      }
      if (Parser.previousProperChar(string, index) != '<') {
        index.incrementAndGet();

        PropositionalLogicExpression first = B(string, index);
        return new PropositionalLogicImplication(first, last);
      } else { // '->' belongs to '<->', ignore it
        index.addAndGet(2);
      }
    }

    index.incrementAndGet();
    return last;
  }
  
  private static PropositionalLogicExpression I(String string, AtomicInteger index) {
    PropositionalLogicExpression last = C(string, index);
    if (Parser.previousProperChar(string, index) == '|') {
      PropositionalLogicExpression first = I(string, index);
      return new PropositionalLogicOr(first, last);
    }
  
    index.incrementAndGet();
    return last;
  }

  private static PropositionalLogicExpression C(String string, AtomicInteger index) {
    PropositionalLogicExpression last = N(string, index);
    if (Parser.previousProperChar(string, index) == '&') {
      PropositionalLogicExpression first = C(string, index);
      return new PropositionalLogicAnd(first, last);
    }

    index.incrementAndGet();
    return last;
  }

  private static PropositionalLogicExpression N(String string, AtomicInteger index) {
    PropositionalLogicExpression last = P(string, index);
    if (Parser.previousProperChar(string, index) == '~') {
      return new PropositionalLogicNegation(last);
    }

    index.incrementAndGet();
    return last;
  }

  private static PropositionalLogicExpression P( String string, AtomicInteger index) {
    if (Parser.previousProperChar(string, index) == ')') {
      PropositionalLogicExpression last = S(string, index);
      if (Parser.previousProperChar(string, index) != '(') {
        int lastIndex = index.get() + 1;
        throw new SyntaxError("Expected '('", string, lastIndex);
      }
      return new PropositionalLogicParenthesis(last);
    } else {
      index.incrementAndGet();
      return VARIABLE(string, index);
    }
  }

  private static PropositionalLogicVariable VARIABLE(String string, AtomicInteger index) {
    StringBuilder builder = new StringBuilder();

    while (index.get() >= 0) {
      char c = Parser.previousProperChar(string, index);
      if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
        builder.append(c);
      } else {
        break;
      }
    }
    index.incrementAndGet();

    if (builder.isEmpty()) {
      int lastIndex = index.get() + 1;
      throw new SyntaxError("Expected variable", string, lastIndex);
    }

    return new PropositionalLogicVariable(builder.reverse().toString());
  }

  public static class PropositionalLogicExpression {
    @Override
    public String toString() {
      return "";
    }
  }

  static class PropositionalLogicNegation extends PropositionalLogicExpression {
    private final PropositionalLogicExpression expression;

    public PropositionalLogicNegation(PropositionalLogicExpression expression) {
      this.expression = expression;
    }

    @Override
    public String toString() {
      return "~" + expression;
    }
  }
  
  static abstract class PropositionalLogicBinary extends PropositionalLogicExpression {
    protected PropositionalLogicExpression left;
    protected PropositionalLogicExpression right;
    
    public PropositionalLogicBinary(
        PropositionalLogicExpression left, PropositionalLogicExpression right) {
      this.left = left;
      this.right = right;
    }
  }

  static class PropositionalLogicImplication extends PropositionalLogicBinary {
    public PropositionalLogicImplication( PropositionalLogicExpression left, PropositionalLogicExpression right ) {
      super( left, right );
    }
  
    @Override
    public String toString() {
      return left + " -> " + right;
    }
  }
  
  static class PropositionalLogicBiConditional extends PropositionalLogicBinary {
    public PropositionalLogicBiConditional( PropositionalLogicExpression left, PropositionalLogicExpression right ) {
      super( left, right );
    }
  
    @Override
    public String toString() {
      return left + " <-> " + right;
    }
  }

  static class PropositionalLogicAnd extends PropositionalLogicBinary {
    public PropositionalLogicAnd( PropositionalLogicExpression left, PropositionalLogicExpression right ) {
      super( left, right );
    }
  
    @Override
    public String toString() {
      return left + " & " + right;
    }
  }

  static class PropositionalLogicOr extends PropositionalLogicBinary {
    public PropositionalLogicOr( PropositionalLogicExpression left, PropositionalLogicExpression right ) {
      super( left, right );
    }
  
    @Override
    public String toString() {
      return left + " | " + right;
    }
  }

  static class PropositionalLogicParenthesis extends PropositionalLogicExpression {
    private final PropositionalLogicExpression expression;

    public PropositionalLogicParenthesis(PropositionalLogicExpression expression) {
      this.expression = expression;
    }

    @Override
    public String toString() {
      return "(" + expression + ")";
    }
  }

  static class PropositionalLogicVariable extends PropositionalLogicExpression {
    private final String name;

    public PropositionalLogicVariable(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  // todo: just as a prototype
  public static CNF tseitin(PropositionalLogicExpression expression) {
    TseitinNode result = tseitin(expression, new AtomicInteger(0));
    List<Clause> clauses = result.clauses;
    clauses.add(new Clause(new ArrayList<>(List.of(new Literal(result.nodeName)))));

    return new CNF(clauses);
  }

  private static TseitinNode tseitin(PropositionalLogicExpression expression, AtomicInteger index) {
    if (expression instanceof PropositionalLogicVariable variable) {
      return TseitinNode.leaf(variable.name);
    }

    // todo: make sure that the helper variable is unique
    String helperVariable = "h" + index.get();
    index.incrementAndGet();

    Literal h = new Literal(helperVariable);
    Literal notH = h.not();
    if (expression instanceof PropositionalLogicNegation negation) {
      TseitinNode node = tseitin(negation.expression, index);
      List<Clause> clauses = node.clauses;

      Literal nodeLiteral = new Literal(node.nodeName);
      Literal notNodeLiteral = nodeLiteral.not();

      Clause c0 = new Clause(new ArrayList<>(List.of(notH, notNodeLiteral)));
      Clause c1 = new Clause(new ArrayList<>(List.of(nodeLiteral, h)));

      clauses.add(c0);
      clauses.add(c1);

      return TseitinNode.inner(clauses, helperVariable);
    } else if (expression instanceof PropositionalLogicBinary binaryExpression) {
      TseitinNode left = tseitin(binaryExpression.left, index);
      TseitinNode right = tseitin(binaryExpression.right, index);

      List<Clause> clauses = new ArrayList<>(left.clauses);
      clauses.addAll(right.clauses);
  
      Literal leftLiteral = new Literal(left.nodeName);
      Literal notLeftLiteral = leftLiteral.not();
      Literal rightLiteral = new Literal(right.nodeName);
      Literal notRightLiteral = rightLiteral.not();

      if (expression instanceof PropositionalLogicOr) {
        Clause c0 = new Clause(new ArrayList<>(List.of(notH, leftLiteral, rightLiteral)));
        Clause c1 = new Clause(new ArrayList<>(List.of(notLeftLiteral, h)));
        Clause c2 = new Clause(new ArrayList<>(List.of(notRightLiteral, h)));

        clauses.add(c0);
        clauses.add(c1);
        clauses.add(c2);

        return TseitinNode.inner(clauses, helperVariable);
      } else if (expression instanceof PropositionalLogicAnd) {
        Clause c0 = new Clause(new ArrayList<>(List.of(notH, leftLiteral)));
        Clause c1 = new Clause(new ArrayList<>(List.of(notH, rightLiteral)));
        Clause c2 = new Clause(new ArrayList<>(List.of(notLeftLiteral, notRightLiteral, h)));

        clauses.add(c0);
        clauses.add(c1);
        clauses.add(c2);

        return TseitinNode.inner(clauses, helperVariable);
      } else if (expression instanceof PropositionalLogicImplication) {
        Clause c0 = new Clause(new ArrayList<>(List.of(notH, notLeftLiteral, rightLiteral)));
        Clause c1 = new Clause(new ArrayList<>(List.of(leftLiteral, h)));
        Clause c2 = new Clause(new ArrayList<>(List.of(notRightLiteral, h)));

        clauses.add(c0);
        clauses.add(c1);
        clauses.add(c2);

        return TseitinNode.inner(clauses, helperVariable);
      } else if (expression instanceof PropositionalLogicBiConditional) {
        Clause clause0 = new Clause(new ArrayList<>(List.of(notH, notLeftLiteral, rightLiteral)));
        Clause clause1 = new Clause(new ArrayList<>(List.of(notH, notRightLiteral, leftLiteral)));
        Clause clause2 = new Clause(new ArrayList<>(List.of(leftLiteral, rightLiteral, h)));
        Clause clause3 = new Clause(new ArrayList<>(List.of(notLeftLiteral, notRightLiteral, h)));

        clauses.add(clause0);
        clauses.add(clause1);
        clauses.add(clause2);
        clauses.add(clause3);

        return TseitinNode.inner(clauses, helperVariable);
      }
    } else if (expression instanceof PropositionalLogicParenthesis parenthesis) {
      index.decrementAndGet(); // parenthesis don't need to be counted as helper variables
      return tseitin(parenthesis.expression, index);
    }

    throw new UnsupportedOperationException("Not implemented yet");
  }

  static class TseitinNode {
    private final List<Clause> clauses;
    private final String nodeName;

    private TseitinNode(List<Clause> clauses, String nodeName) {
      this.clauses = clauses;
      this.nodeName = nodeName;
    }

    public static TseitinNode leaf(String nodeName) {
      return new TseitinNode(new ArrayList<>(), nodeName);
    }

    public static TseitinNode inner(List<Clause> clauses, String nodeName) {
      return new TseitinNode(clauses, nodeName);
    }
  }
}
