package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.Bound;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearTerm;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.Scanner;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;

public class LinearConstraintParser implements Parser<LinearConstraint> {

  public static void main(String[] args) {
    LinearConstraintParser parser = new LinearConstraintParser();

    Scanner scanner = new Scanner(System.in);
    String line;
    while ((line = scanner.nextLine()) != null) {
      try {
        LinearConstraint constraint = parser.parse(line);
        System.out.println(constraint);
      } catch (SyntaxError e) {
        e.printWithContext();
        e.printStackTrace();
      }
    }
  }

  /*
   *  Grammar for Linear constraints:
   *    <S> ::= <TERM> '=' <TERM>
   *          | <TERM> '<=' <TERM
   *          | <TERM> '>=' <TERM>
   *          | MIN '(' <TERM> ')'
   *          | MAX '(' <TERM> ')'
   *
   */
  @Override
  public ParseResult<LinearConstraint> parseWithRemaining(String string) {
    Lexer lexer = new LinearConstraintLexer(string);

    lexer.requireNextToken();

    boolean optimization = false;
    boolean minimization = false;

    if (lexer.canConsume(MIN)) {
      lexer.consume(MIN);
      lexer.consume(LPAREN);

      optimization = true;
      minimization = true;
    } else if (lexer.canConsume(MAX)) {
      lexer.consume(MAX);
      lexer.consume(LPAREN);

      optimization = true;
    }

    LinearTerm lhs = TERM(lexer);

    LinearConstraint lc;
    if(optimization) {
      if(minimization) {
        lc = new MinimizingConstraint(lhs);
      } else {
        lc = new MaximizingConstraint(lhs);
      }

      lexer.consume(RPAREN);
      return new ParseResult<>(lc, lexer.getCursor(), lexer.getCursor() == string.length());
    }

    Bound bound = BOUND(lexer);

    LinearTerm rhs = TERM(lexer);

    lc = new LinearConstraint(lhs, rhs, bound);

    return new ParseResult<>(lc, lexer.getCursor(), lexer.getCursor() == string.length());
  }

  private static LinearTerm TERM(Lexer lexer) {
    LinearTermParser parser = new LinearTermParser();
    ParseResult<LinearTerm> result = parser.parseWithRemaining(lexer.getRemaining());

    return result.result();
  }

  private static Bound BOUND(Lexer lexer) {
    lexer.requireEither(EQUALS, LOWER_EQUALS, GREATER_EQUALS);
    if (lexer.canConsume(EQUALS)) {
      lexer.consume(EQUALS);
      return EQUAL;
    } else if (lexer.canConsume(LOWER_EQUALS)) {
      lexer.consume(LOWER_EQUALS);
      return UPPER;
    } else {
      lexer.consume(GREATER_EQUALS);
      return LOWER;
    }
  }
}
