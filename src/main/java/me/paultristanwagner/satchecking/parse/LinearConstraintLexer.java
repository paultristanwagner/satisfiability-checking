package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class LinearConstraintLexer extends Lexer {

  public LinearConstraintLexer(String input) {
    super(input);

    registerTokenTypes(MIN, MAX, FRACTION, DECIMAL, IDENTIFIER, EQUALS, LOWER_EQUALS, GREATER_EQUALS, PLUS, MINUS,  LPAREN, RPAREN);

    initialize(input);
  }
}
