package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class PolynomialLexer extends Lexer {

  public PolynomialLexer(String input) {
    super(input);

    registerTokenTypes(
        PLUS,
        MINUS,
        DECIMAL,
        FRACTION,
        IDENTIFIER,
        TIMES,
        POWER);

    initialize(input);
  }
}
