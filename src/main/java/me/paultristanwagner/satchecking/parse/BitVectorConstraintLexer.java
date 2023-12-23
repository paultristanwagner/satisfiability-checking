package me.paultristanwagner.satchecking.parse;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class BitVectorConstraintLexer extends Lexer {

  public BitVectorConstraintLexer(String input) {
    super(input);

    registerTokenTypes();

    registerTokenTypes(
        BITWISE_LEFT_SHIFT,
        BITWISE_RIGHT_SHIFT,
        EQUALS,
        NOT_EQUALS,
        LESS_THAN,
        BINARY_CONSTANT,
        INTEGER,
        HEX_CONSTANT,
        IDENTIFIER,
        BITWISE_NOT,
        TIMES,
        REMAINDER,
        PLUS,
        MINUS,
        BITWISE_AND,
        BITWISE_XOR,
        BITWISE_OR,
        LPAREN,
        RPAREN
    );

    initialize(input);
  }
}
