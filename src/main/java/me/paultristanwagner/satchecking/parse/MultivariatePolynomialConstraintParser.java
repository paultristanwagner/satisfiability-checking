package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.sat.Result;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.Comparison.GREATER_THAN;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.Comparison.GREATER_THAN_OR_EQUALS;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.Comparison.LESS_THAN;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.Comparison.LESS_THAN_OR_EQUALS;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.Comparison.NOT_EQUALS;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.multivariatePolynomialConstraint;

public class MultivariatePolynomialConstraintParser implements Parser<MultivariatePolynomialConstraint> {

  @Override
  public MultivariatePolynomialConstraint parse(String string) {
    ParseResult<MultivariatePolynomialConstraint> result = parseWithRemaining(string);
    if(!result.complete()) {
      throw new SyntaxError("Expected end of input", string, result.charsRead());
    }

    return result.result();
  }

  @Override
  public ParseResult<MultivariatePolynomialConstraint> parseWithRemaining(String string) {
    Parser<MultivariatePolynomial> parser = new PolynomialParser();

    ParseResult<MultivariatePolynomial> pResult = parser.parseWithRemaining(string);
    MultivariatePolynomial p = pResult.result();

    Lexer lexer = new ComparisonLexer(string.substring(pResult.charsRead()));
    MultivariatePolynomialConstraint.Comparison comparison = parseComparison(lexer);

    ParseResult<MultivariatePolynomial> qResult = parser.parseWithRemaining(lexer.getRemaining());
    MultivariatePolynomial q = qResult.result();

    MultivariatePolynomial d = p.subtract(q);
    MultivariatePolynomialConstraint constraint = multivariatePolynomialConstraint(d, comparison);

    int charsRead = pResult.charsRead() + lexer.getCursor() + qResult.charsRead();
    return new ParseResult<>(constraint, charsRead, qResult.complete());
  }

  private MultivariatePolynomialConstraint.Comparison parseComparison(Lexer lexer) {
    if(lexer.canConsume(TokenType.EQUALS)) {
      lexer.consume(TokenType.EQUALS);
      return MultivariatePolynomialConstraint.Comparison.EQUALS;
    } else if(lexer.canConsume(TokenType.NOT_EQUALS)) {
      lexer.consume(TokenType.NOT_EQUALS);
      return NOT_EQUALS;
    } else if(lexer.canConsume(TokenType.LESS_THAN)) {
      lexer.consume(TokenType.LESS_THAN);
      return LESS_THAN;
    } else if(lexer.canConsume(TokenType.GREATER_THAN)) {
      lexer.consume(TokenType.GREATER_THAN);
      return GREATER_THAN;
    } else if(lexer.canConsume(LOWER_EQUALS)) {
      lexer.consume(LOWER_EQUALS);
      return LESS_THAN_OR_EQUALS;
    } else if(lexer.canConsume(GREATER_EQUALS)) {
      lexer.consume(GREATER_EQUALS);
      return GREATER_THAN_OR_EQUALS;
    } else {
      throw new SyntaxError("Expected comparison operator", lexer.getInput(), lexer.getCursor());
    }
  }
}
