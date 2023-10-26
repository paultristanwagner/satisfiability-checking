package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class EqualityConstraintLexer extends Lexer {

  public EqualityConstraintLexer(String input) {
    super(input);

    registerTokenTypes(NOT_EQUALS, EQUALS, IDENTIFIER);

    this.initialize(input);
  }
}
