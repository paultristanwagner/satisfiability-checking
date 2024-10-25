package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial.constant;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial.variable;

public class PolynomialParser implements Parser<MultivariatePolynomial> {

  /*
   * Grammar for polynomial constraints:
   *   <S> ::= <TERM> EQUALS 0
   *
   *   <TERM> ::= <MONOMIAL> PLUS <MONOMIAL>
   *            | <MONOMIAL> MINUS <MONOMIAL>
   *            | <MONOMIAL>
   *
   *  <MONOMIAL> ::= <FACTOR> TIMES <FACTOR>
   *               | <FACTOR>
   *
   *  <FACTOR> ::= FRACTION
   *             | DECIMAL
   *             | IDENTIFIER
   *             | IDENTIFIER POWER INTEGER
   */

  public MultivariatePolynomial parse(String string) {
    ParseResult<MultivariatePolynomial> result = parseWithRemaining(string);

    if (!result.complete()) {
      throw new SyntaxError("Expected end of input", string, result.charsRead());
    }

    return result.result();
  }

  @Override
  public ParseResult<MultivariatePolynomial> parseWithRemaining(String string) {
    Lexer lexer = new PolynomialLexer(string);

    lexer.requireNextToken();

    MultivariatePolynomial polynomial = parseTerm(lexer);

    return new ParseResult<>(polynomial, lexer.getCursor(), lexer.getCursor() == string.length());
  }

  private MultivariatePolynomial parseTerm(Lexer lexer) {
    MultivariatePolynomial term1 = parseMonomial(lexer);

    while(lexer.canConsume(PLUS) || lexer.canConsume(MINUS)){
      if (lexer.canConsume(PLUS)) {
        lexer.consume(PLUS);
        MultivariatePolynomial term2 = parseMonomial(lexer);
        term1 = term1.add(term2);
      } else if (lexer.canConsume(MINUS)) {
        lexer.consume(MINUS);
        MultivariatePolynomial term2 = parseMonomial(lexer);
        term1 = term1.subtract(term2);
      }
    }

    return term1;
  }

  private MultivariatePolynomial parseMonomial(Lexer lexer) {
    MultivariatePolynomial monomial1 = parseFactor(lexer);

    while (lexer.canConsumeEither(TIMES, IDENTIFIER)) {
      if(lexer.canConsume(TIMES)) {
        lexer.consume(TIMES);
      }
      MultivariatePolynomial monomial2 = parseFactor(lexer);
      monomial1 = monomial1.multiply(monomial2);
    }

    return monomial1;
  }

  private int parseSign(Lexer lexer) {
    int sign = 1;
    while(lexer.canConsumeEither(PLUS, MINUS)){
      if (lexer.canConsume(PLUS)) {
        lexer.consume(PLUS);
      } else if (lexer.canConsume(MINUS)) {
        lexer.consume(MINUS);
        sign *= -1;
      }
    }

    return sign;
  }

  private MultivariatePolynomial parseFactor(Lexer lexer) {
    int sign = parseSign(lexer);

    if (lexer.canConsumeEither(DECIMAL, FRACTION)) {
      String value = lexer.getLookahead().getValue();
      lexer.consumeEither(DECIMAL, FRACTION);
      Number number = Number.parse(value);

      if(sign == -1) {
        number = number.negate();
      }

      return constant(number);
    }

    if (lexer.canConsume(IDENTIFIER)) {
      String variable = lexer.getLookahead().getValue();
      lexer.consume(IDENTIFIER);

      if (lexer.canConsume(POWER)) {
        lexer.consume(POWER);
        lexer.require(DECIMAL);
        String exponent = lexer.getLookahead().getValue();
        lexer.consume(DECIMAL);

        MultivariatePolynomial monomial = variable(variable).pow(Integer.parseInt(exponent));

        return monomial.multiply(constant(number(sign)));
      }

      return variable(variable).multiply(constant(number(sign)));
    }

    throw new SyntaxError("Expected either a decimal or an identifier", lexer.getInput(), lexer.getCursor());
  }
}
