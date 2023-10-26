package me.paultristanwagner.satchecking.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Lexer {

  public static TokenType WHITESPACE = TokenType.of("whitespace", "^\\s+");

  private String input;
  private final List<TokenType> tokenTypes = new ArrayList<>();

  private int cursor;
  private Token lookahead;

  public Lexer(String input) {
    registerTokenType(WHITESPACE);
  }

  public void initialize(String input) {
    this.input = input;
    this.cursor = 0;

    this.nextToken();
  }

  public void skip(int positions) {
    this.cursor += positions;

    this.nextToken();
  }

  public void registerTokenType(TokenType tokenType) {
    this.tokenTypes.add(tokenType);
  }

  public void registerTokenTypes(TokenType... tokenTypes) {
    for (TokenType tokenType : tokenTypes) {
      registerTokenType(tokenType);
    }
  }

  public Token nextToken() {
    if (cursor >= input.length()) {
      lookahead = null;
      return null;
    }

    for (TokenType tokenType : tokenTypes) {
      String regex = tokenType.getRegex();
      String remaining = input.substring(cursor);

      Matcher matcher = Pattern.compile(regex).matcher(remaining);

      if (!matcher.find()) {
        continue;
      }

      String group = matcher.group();

      if (tokenType == WHITESPACE) {
        cursor += group.length();
        return nextToken();
      }

      lookahead = Token.of(tokenType, group);
      return lookahead;
    }

    lookahead = null;
    return null;
  }

  public void require(TokenType tokenType) {
    requireEither(tokenType);
  }

  public void requireEither(TokenType... tokenTypes) {
    for (TokenType tokenType : tokenTypes) {
      if (canConsume(tokenType)) {
        return;
      }
    }

    if (tokenTypes.length == 1) {
      throw new SyntaxError("Expected token '" + tokenTypes[0].getName() + "'", input, cursor);
    }

    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < tokenTypes.length; i++) {
      TokenType tokenType = tokenTypes[i];
      if(i != tokenTypes.length - 1) {
        builder.append("'").append(tokenType.getName()).append("', ");
      } else {
        builder.append(" or '").append(tokenType.getName()).append("'");
      }
    }

    throw new SyntaxError("Expected either token " + builder, input, cursor);
  }

  public boolean canConsume(TokenType tokenType) {
    return canConsumeEither(tokenType);
  }

  public boolean canConsumeEither(TokenType... tokenTypes) {
    if(lookahead == null) {
      return false;
    }

    for (TokenType type : tokenTypes) {
      if(lookahead.getType() == type) {
        return true;
      }
    }

    return false;
  }

  public void consume(TokenType token) {
    if (lookahead == null) {
      throw new SyntaxError("Expected token '" + token.getName() + "'", input, cursor);
    }

    if (lookahead.getType() != token) {
      throw new SyntaxError(
          "Expected token '"
              + token.getName()
              + "' but got token '"
              + lookahead.getType().getName()
              + "'",
          input,
          cursor);
    }

    cursor += lookahead.getValue().length();
    nextToken();
  }

  public String getInput() {
    return input;
  }

  public int getCursor() {
    return cursor;
  }

  public Token getLookahead() {
    return lookahead;
  }

  public String getRemaining() {
    return input.substring(cursor);
  }

  public boolean hasNextToken() {
    return lookahead != null;
  }

  public void requireNextToken() {
    if (!hasNextToken()) {
      throw new SyntaxError("Unexpected end of input", input, cursor);
    }
  }

  public void requireNoToken() {
    if (hasNextToken()) {
      throw new SyntaxError(
          "Unexpected token " + lookahead.getType().getName(),
          input,
          cursor - lookahead.getValue().length());
    }
  }
}
