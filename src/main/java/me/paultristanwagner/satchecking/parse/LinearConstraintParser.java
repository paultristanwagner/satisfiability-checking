package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;

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

    if (lexer.canConsume(MIN)) {
      optimization = true;
      lexer.consume(MIN);

      lexer.consume(LPAREN);
      lc = new MinimizingConstraint();
    } else if (lexer.canConsume(MAX)) {
      optimization = true;
      lexer.consume(MAX);

      lexer.consume(LPAREN);
      lc = new MaximizingConstraint();
    } else {
      lc = new LinearConstraint();
    }

    Number coefficient = OPTIONAL_SIGNS(lexer).multiply(OPTIONAL_RATIONAL(lexer));
    Token variableToken = lexer.getLookahead();
    lexer.consume(IDENTIFIER);
    String variable = variableToken.getValue();
    lc.setCoefficient(variable, coefficient);

    while (lexer.canConsumeEither(PLUS, MINUS, FRACTION, DECIMAL)) {
      coefficient = OPTIONAL_SIGNS(lexer).multiply(OPTIONAL_RATIONAL(lexer));
      variableToken = lexer.getLookahead();

      lexer.consume(IDENTIFIER);

      variable = variableToken.getValue();
      lc.setCoefficient(variable, coefficient);
    }

    if (optimization) {
      lexer.consume(RPAREN);
      return lc;
    }

    lexer.requireEither(EQUALS, LOWER_EQUALS, GREATER_EQUALS);
    if (lexer.canConsume(EQUALS)) {
      lexer.consume(EQUALS);
      lc.setBound(EQUAL);
    } else if (lexer.canConsume(LOWER_EQUALS)) {
      lexer.consume(LOWER_EQUALS);
      lc.setBound(UPPER);
    } else {
      lexer.consume(GREATER_EQUALS);
      lc.setBound(LOWER);
    }

    Number value = OPTIONAL_SIGNS(lexer).multiply(RATIONAL(lexer));
    lc.setValue(value);

    return lc;
  }

  private static Number OPTIONAL_SIGNS(Lexer lexer) {
    if (lexer.canConsumeEither(PLUS, MINUS)) {
      return SIGNS(lexer);
    } else {
      return ONE();
    }
  }

  private static Number SIGNS(Lexer lexer) {
    lexer.requireEither(PLUS, MINUS);

    Number sign = ONE();
    do {
      if (lexer.canConsume(PLUS)) {
        lexer.consume(PLUS);
      } else {
        lexer.consume(MINUS);
        sign = sign.negate();
      }
    } while (lexer.canConsumeEither(PLUS, MINUS));

    return sign;
  }

  private static Number OPTIONAL_RATIONAL(Lexer lexer) {
    if (lexer.canConsumeEither(FRACTION, DECIMAL)) {
      return RATIONAL(lexer);
    }

    return ONE();
  }

  private static Number RATIONAL(Lexer lexer) {
    lexer.requireEither(FRACTION, DECIMAL);

    if (lexer.canConsume(FRACTION)) {
      return FRACTION(lexer);
    } else {
      return DECIMAL(lexer);
    }
  }

  private static Number DECIMAL(Lexer lexer) {
    Token token = lexer.getLookahead();
    lexer.consume(DECIMAL);

    return Number.parse(token.getValue());
  }

  private static Number FRACTION(Lexer lexer) {
    Token token = lexer.getLookahead();
    lexer.consume(FRACTION);

    String[] parts = token.getValue().split("/");

    long numerator = Long.parseLong(parts[0]);
    long denominator = Long.parseLong(parts[1]);

    return Number.number(numerator, denominator);
  }
}
