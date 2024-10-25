package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.IDENTIFIER;
import static me.paultristanwagner.satchecking.parse.TokenType.INTEGER;

public class DimacsLexer extends Lexer {

  public DimacsLexer(String input) {
    super(input);

    registerTokenTypes(IDENTIFIER, INTEGER);

    initialize(input);
  }
}
