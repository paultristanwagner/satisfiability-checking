package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class EqualityFunctionLexer extends Lexer {

  public EqualityFunctionLexer(String input) {
    super(input);

    registerTokenTypes(IDENTIFIER, EQUALS, NOT_EQUALS, LPAREN, RPAREN, COMMA);

    initialize(input);
  }
}
