package me.paultristanwagner.satchecking.parse;

public class TokenType {

  static final TokenType IDENTIFIER = TokenType.of("identifier", "^([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+)");
  static final TokenType NOT_EQUALS = TokenType.of("!=", "^(!=|≠)");
  static final TokenType EQUALS = TokenType.of("=", "^(=|==)");
  static final TokenType LOWER_EQUALS = TokenType.of("<=", "^(<=|≤)");
  static final TokenType GREATER_EQUALS = TokenType.of(">=", "^(>=|≥)");
  static final TokenType LPAREN = TokenType.of("(", "^\\(");
  static final TokenType RPAREN = TokenType.of(")", "^\\)");
  static final TokenType COMMA = TokenType.of(",", "^,");
  static final TokenType DECIMAL = TokenType.of("decimal", "^[+-]?(?:\\d+\\.\\d*|\\.\\d+|\\d+)");
  static final TokenType FRACTION = TokenType.of("fraction", "^[+-]?\\d+/\\d+");
  static final TokenType MIN = TokenType.of("min", "^(min|MIN)");
  static final TokenType MAX = TokenType.of("max", "^(max|MAX)");
  static final TokenType PLUS = TokenType.of("+", "^\\+");
  static final TokenType MINUS = TokenType.of("-", "^-");
  static final TokenType AND = TokenType.of("and", "^(&|&&|and|AND|∧)");
  static final TokenType OR = TokenType.of("or", "^(\\|\\||\\||or|OR|∨)");
  static final TokenType NOT = TokenType.of("not", "^(~|!|¬|not|NOT)");
  static final TokenType EQUIVALENCE = TokenType.of("equivalence", "^(<->|<=>|iff|IFF)");
  static final TokenType IMPLIES = TokenType.of("implies", "^(->|=>|implies|IMPLIES)");


  private final String name;
  private final String regex;

  private TokenType(String name, String regex) {
    this.name = name;
    this.regex = regex;
  }

  public static TokenType of(String name, String regex) {
    return new TokenType(name, regex);
  }

  public String getName() {
    return name;
  }

  public String getRegex() {
    return regex;
  }
}
