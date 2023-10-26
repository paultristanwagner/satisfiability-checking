package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;

import java.util.concurrent.atomic.AtomicInteger;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;

public class LinearConstraintParser implements Parser<LinearConstraint> {

  /*
   *  Grammar for Linear constraints:
   *    <S> ::= <TERM> '=' <RATIONAL>
   *          | <TERM> '<=' <RATIONAL>
   *          | <TERM> '>=' <RATIONAL>
   *          | MIN '(' <TERM> ')'
   *          | MAX '(' <TERM> ')'
   *
   *    <TERM> ::= [ <SIGNS> ] [ <RATIONAL> ] IDENTIFIER
   *             | [ <SIGNS> ] [ <RATIONAL> ] IDENTIFIER [ <SIGNS> <TERM> ]
   *
   *    <SIGNS> ::= '+' [ <SIGNS> ]
   *              | '-' [ <SIGNS> ]
   *
   *    <RATIONAL> ::= FRACTION | DECIMAL
   *
   */
  @Override
  public ParseResult<LinearConstraint> parseWithRemaining(String string) {
    Lexer lexer = new LinearConstraintLexer(string);

    lexer.requireNextToken();

    LinearConstraint lc = TERM(lexer);

    return new ParseResult<>(lc, lexer.getCursor(), lexer.getCursor() == string.length());
  }

  private static LinearConstraint TERM(Lexer lexer) {
    LinearConstraint lc;
    boolean optimization = false;

    if(lexer.canConsume(MIN)) {
      optimization = true;
      lexer.consume(MIN);

      lexer.require(LPAREN);
      lexer.consume(LPAREN);
      lc = new MinimizingConstraint();
    } else if(lexer.canConsume(MAX)) {
      optimization = true;
      lexer.consume(MAX);

      lexer.require(LPAREN);
      lexer.consume(LPAREN);
      lc = new MaximizingConstraint();
    } else {
      lc = new LinearConstraint();
    }

    double coefficient = OPTIONAL_SIGNS(lexer) * OPTIONAL_RATIONAL(lexer);
    Token variableToken = lexer.getLookahead();
    lexer.require(IDENTIFIER);
    lexer.consume(IDENTIFIER);
    String variable = variableToken.getValue();
    lc.setCoefficient(variable, coefficient);

    while(lexer.canConsumeEither(PLUS, MINUS, FRACTION, DECIMAL)) {
      coefficient = OPTIONAL_SIGNS(lexer) * OPTIONAL_RATIONAL(lexer);
      variableToken = lexer.getLookahead();

      lexer.require(IDENTIFIER);
      lexer.consume(IDENTIFIER);

      variable = variableToken.getValue();
      lc.setCoefficient(variable, coefficient);
    }

    if(optimization) {
      lexer.require(RPAREN);
      lexer.consume(RPAREN);
      return lc;
    }

    lexer.requireEither(EQUALS, LOWER_EQUALS, GREATER_EQUALS);
    if(lexer.canConsume(EQUALS)) {
      lexer.consume(EQUALS);
      lc.setBound(EQUAL);
    } else if(lexer.canConsume(LOWER_EQUALS)) {
      lexer.consume(LOWER_EQUALS);
      lc.setBound(UPPER);
    } else {
      lexer.consume(GREATER_EQUALS);
      lc.setBound(LOWER);
    }

    double value = OPTIONAL_SIGNS(lexer) * RATIONAL(lexer);
    lc.setValue(value);

    return lc;
  }

  private static int OPTIONAL_SIGNS(Lexer lexer) {
    if(lexer.canConsumeEither(PLUS, MINUS)) {
      return SIGNS(lexer);
    } else {
      return 1;
    }
  }

  private static int SIGNS(Lexer lexer) {
    lexer.requireEither(PLUS, MINUS);

    int sign = 1;
    do {
      if(lexer.canConsume(PLUS)) {
        lexer.consume(PLUS);
      } else {
        lexer.consume(MINUS);
        sign *= -1;
      }
    } while(lexer.canConsumeEither(PLUS, MINUS));

    return sign;
  }

  private static double OPTIONAL_RATIONAL(Lexer lexer) {
    if(lexer.canConsumeEither(FRACTION, DECIMAL)) {
      return RATIONAL(lexer);
    }

    return 1;
  }

  private static double RATIONAL(Lexer lexer) {
    lexer.requireEither(FRACTION, DECIMAL);

    if(lexer.canConsume(FRACTION)) {
      return FRACTION(lexer);
    } else {
      return DECIMAL(lexer);
    }
  }

  private static double DECIMAL(Lexer lexer) {
    lexer.require(DECIMAL);

    Token token = lexer.getLookahead();
    lexer.consume(DECIMAL);
    return Double.parseDouble(token.getValue());
  }

  private static double FRACTION(Lexer lexer) {
    lexer.require(FRACTION);

    Token token = lexer.getLookahead();
    lexer.consume(FRACTION);

    String[] parts = token.getValue().split("/");
    return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
  }
}
