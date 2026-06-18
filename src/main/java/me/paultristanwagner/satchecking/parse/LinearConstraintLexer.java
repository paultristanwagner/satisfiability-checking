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
        LESS_THAN,
        GREATER_THAN,
        LPAREN,
        RPAREN
    );

    initialize(input);
  }
}
