package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class LinearConstraintLexer extends Lexer {

  public LinearConstraintLexer(String input) {
    super(input);

    registerTokenTypes(
        MIN,
        MAX,
        EQUALS,
        LOWER_EQUALS,
        GREATER_EQUALS,
        LPAREN,
        RPAREN
    );

    initialize(input);
  }
}
