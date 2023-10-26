package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class PropositionalLogicParser
    implements Parser<PropositionalLogicParser.PropositionalLogicExpression> {

  /*
   * Grammar for propositional logic:
   *    <S> ::= <B>
   *          | <B> '<->' <S>
   *
   *    <B> ::= <I>
   *          | <I> '->' <B>
   *
   *    <I> ::= <C>
   *          | <I> '|' <C>
   *
   *    <C> ::= <N>
   *          | <C> '&' <N>
   *
   *    <N> ::= <P>
   *          | '~' <P>
   *
   *   <P> ::= IDENTIFIER
   *         | '(' <S> ')'
   */

  @Override
  public ParseResult<PropositionalLogicExpression> parseWithRemaining(String string) {
    Lexer lexer = new PropositionalLogicLexer(string);

    lexer.requireNextToken();

    PropositionalLogicExpression expression = S(lexer);

    return new ParseResult<>(
        expression,
        lexer.getCursor(),
        lexer.getCursor() == string.length()
    );
  }

  private static PropositionalLogicExpression S(Lexer lexer) {
    PropositionalLogicExpression first = B(lexer);

    if (lexer.canConsume(EQUIVALENCE)) {
      lexer.consume(EQUIVALENCE);
      PropositionalLogicExpression last = S(lexer);
      return new PropositionalLogicBiConditional(first, last);
    }

    return first;
  }

  private static PropositionalLogicExpression B(Lexer lexer) {
    PropositionalLogicExpression first = I(lexer);

    if (lexer.canConsume(IMPLIES)) {
      lexer.consume(IMPLIES);
      PropositionalLogicExpression last = B(lexer);
      return new PropositionalLogicImplication(first, last);
    }

    return first;
  }

  private static PropositionalLogicExpression I(Lexer lexer) {
    PropositionalLogicExpression res = C(lexer);

    while (lexer.canConsume(OR)) {
      lexer.consume(OR);
      PropositionalLogicExpression next = I(lexer);
      res = new PropositionalLogicOr(res, next);
    }

    return res;
  }

  private static PropositionalLogicExpression C(Lexer lexer) {
    PropositionalLogicExpression res = N(lexer);

    while (lexer.canConsume(AND)) {
      lexer.consume(AND);
      PropositionalLogicExpression next = C(lexer);
      res = new PropositionalLogicAnd(res, next);
    }

    return res;
  }

  private static PropositionalLogicExpression N(Lexer lexer) {
    if (lexer.canConsume(NOT)) {
      lexer.consume(NOT);
      PropositionalLogicExpression next = P(lexer);
      return new PropositionalLogicNegation(next);
    }

    return P(lexer);
  }

  private static PropositionalLogicExpression P(Lexer lexer) {
    lexer.requireEither(LPAREN, IDENTIFIER);

    if (lexer.canConsume(LPAREN)) {
      lexer.consume(LPAREN);

      PropositionalLogicExpression res = S(lexer);

      lexer.require(RPAREN);
      lexer.consume(RPAREN);

      return new PropositionalLogicParenthesis(res);
    }

    return VARIABLE(lexer);
  }

  private static PropositionalLogicVariable VARIABLE(Lexer lexer) {
    lexer.require(IDENTIFIER);

    Token lookahead = lexer.getLookahead();
    lexer.consume(IDENTIFIER);
    return new PropositionalLogicVariable(lookahead.getValue());
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
    public PropositionalLogicImplication(PropositionalLogicExpression left, PropositionalLogicExpression right) {
      super(left, right);
    }

    @Override
    public String toString() {
      return left + " -> " + right;
    }
  }

  static class PropositionalLogicBiConditional extends PropositionalLogicBinary {
    public PropositionalLogicBiConditional(PropositionalLogicExpression left, PropositionalLogicExpression right) {
      super(left, right);
    }

    @Override
    public String toString() {
      return left + " <-> " + right;
    }
  }

  static class PropositionalLogicAnd extends PropositionalLogicBinary {
    public PropositionalLogicAnd(PropositionalLogicExpression left, PropositionalLogicExpression right) {
      super(left, right);
    }

    @Override
    public String toString() {
      return left + " & " + right;
    }
  }

  static class PropositionalLogicOr extends PropositionalLogicBinary {
    public PropositionalLogicOr(PropositionalLogicExpression left, PropositionalLogicExpression right) {
      super(left, right);
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
