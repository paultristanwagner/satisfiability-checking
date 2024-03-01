package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class LinearTermLexer extends Lexer {

  public LinearTermLexer(String input) {
    super(input);

    registerTokenTypes(
        FRACTION,
        DECIMAL,
        IDENTIFIER,
        PLUS,
        MINUS
    );

    initialize(input);
  }
}
