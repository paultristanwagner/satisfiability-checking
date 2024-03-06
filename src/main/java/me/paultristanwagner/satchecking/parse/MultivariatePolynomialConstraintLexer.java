package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class MultivariatePolynomialConstraintLexer extends Lexer {

  public MultivariatePolynomialConstraintLexer(String input) {
    super(input);

    registerTokenTypes(
        MIN,
        MAX,
        LPAREN,
        RPAREN
    );

    this.initialize(input);
  }
}
