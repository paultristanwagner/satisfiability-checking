package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class ComparisonLexer extends Lexer {

  public ComparisonLexer(String input) {
    super(input);

    registerTokenTypes(
        EQUALS,
        NOT_EQUALS,
        GREATER_EQUALS,
        LOWER_EQUALS,
        LESS_THAN,
        GREATER_THAN
    );

    initialize(input);
  }
}
