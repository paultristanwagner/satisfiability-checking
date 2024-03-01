package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.LinearTerm;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;

public class LinearTermParser implements Parser<LinearTerm> {

  /*
   * Grammar for linear terms:
   *   <TERM> ::= [ <SIGNS> ] [ <RATIONAL> ] IDENTIFIER [ <SIGNS> <TERM> ]
   *            | [ <SIGNS> ] <RATIONAL> [ <SIGNS> <TERM> ]
   *
   */

  @Override
  public ParseResult<LinearTerm> parseWithRemaining(String string) {
    LinearTermLexer lexer = new LinearTermLexer(string);

    lexer.requireNextToken();

    LinearTerm term = new LinearTerm();

    Number value = OPTIONAL_SIGNS(lexer);
    boolean explicitValue = false;

    if(lexer.canConsumeEither(DECIMAL, FRACTION)) {
      explicitValue = true;
      value = value.multiply(OPTIONAL_RATIONAL(lexer));
    }

    if(!explicitValue) {
      lexer.require(IDENTIFIER);
      String identifier = lexer.getLookahead().getValue();
      lexer.consume(IDENTIFIER);
      term.addCoefficient(identifier, value);
    } else {
      term.addConstant(value);
    }

    while(lexer.canConsumeEither(PLUS, MINUS)) {
      Number sign = SIGNS(lexer);
      value = OPTIONAL_SIGNS(lexer);
      explicitValue = false;

      if(lexer.canConsumeEither(DECIMAL, FRACTION)) {
        explicitValue = true;
        value = value.multiply(OPTIONAL_RATIONAL(lexer));
      }

      if(!explicitValue) {
        lexer.require(IDENTIFIER);
        String identifier = lexer.getLookahead().getValue();
        lexer.consume(IDENTIFIER);
        term.addCoefficient(identifier, sign.multiply(value));
      } else {
        term.addConstant(sign.multiply(value));
      }
    }

    return new ParseResult<>(term, lexer.getCursor(), lexer.getRemaining().isEmpty());
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
