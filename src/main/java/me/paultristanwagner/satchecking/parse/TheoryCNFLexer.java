package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class TheoryCNFLexer extends Lexer {

  public TheoryCNFLexer(String input) {
    super(input);

    registerTokenTypes(AND, OR, LPAREN, RPAREN);

    initialize(input);
  }
}
