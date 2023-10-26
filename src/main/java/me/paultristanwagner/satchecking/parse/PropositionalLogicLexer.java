package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class PropositionalLogicLexer extends Lexer {

  public PropositionalLogicLexer(String input) {
    super(input);

    registerTokenTypes(AND, OR, NOT, EQUIVALENCE, IMPLIES, LPAREN, RPAREN, IDENTIFIER);

    initialize(input);
  }
}
