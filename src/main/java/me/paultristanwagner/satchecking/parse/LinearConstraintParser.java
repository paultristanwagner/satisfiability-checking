package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.Bound;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearTerm;

import java.util.Scanner;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.parse.TokenType.GREATER_EQUALS;
import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;

public class LinearConstraintParser implements Parser<LinearConstraint> {

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

    lexer.skip(result.charsRead());

    return result.result();
  }

  private static Bound BOUND(Lexer lexer) {
    lexer.requireEither(EQUALS, LOWER_EQUALS, GREATER_EQUALS);
    if (lexer.canConsume(EQUALS)) {
      lexer.consume(EQUALS);
      return EQUAL;
    } else if (lexer.canConsume(LOWER_EQUALS)) {
      lexer.consume(LOWER_EQUALS);
      return LESS_EQUALS;
    } else {
      lexer.consume(GREATER_EQUALS);
      return Bound.GREATER_EQUALS;
    }
  }
}
