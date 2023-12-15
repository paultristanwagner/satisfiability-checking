package me.paultristanwagner.satchecking.parse;

public class TokenType {

  static final TokenType IDENTIFIER = TokenType.of("identifier", "^([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+)");
  static final TokenType NOT_EQUALS = TokenType.of("!=", "^(!=|≠)");
  static final TokenType EQUALS = TokenType.of("=", "^(=|==)");
  static final TokenType LOWER_EQUALS = TokenType.of("<=", "^(<=|≤)");
  static final TokenType GREATER_EQUALS = TokenType.of(">=", "^(>=|≥)");
  static final TokenType LESS_THAN = TokenType.of("<", "^<");
  // static final TokenType GREATER_THAN = TokenType.of(">", "^>");
  static final TokenType LPAREN = TokenType.of("(", "^\\(");
  static final TokenType RPAREN = TokenType.of(")", "^\\)");
  static final TokenType COMMA = TokenType.of(",", "^,");
  static final TokenType INTEGER = TokenType.of("integer", "^\\d+");
  static final TokenType DECIMAL = TokenType.of("decimal", "^[+-]?(?:\\d+\\.\\d*|\\.\\d+|\\d+)");
  static final TokenType FRACTION = TokenType.of("fraction", "^[+-]?\\d+/\\d+");
  static final TokenType MIN = TokenType.of("min", "^(min|MIN)");
  static final TokenType MAX = TokenType.of("max", "^(max|MAX)");
  static final TokenType PLUS = TokenType.of("+", "^\\+");
  static final TokenType MINUS = TokenType.of("-", "^-");
  static final TokenType TIMES = TokenType.of("*", "^\\*");
  static final TokenType AND = TokenType.of("and", "^(&|&&|and|AND|∧)");
  static final TokenType OR = TokenType.of("or", "^(\\|\\||\\||or|OR|∨)");
  static final TokenType NOT = TokenType.of("not", "^(~|!|¬|not|NOT)");
  static final TokenType EQUIVALENCE = TokenType.of("equivalence", "^(<->|<=>|iff|IFF)");
  static final TokenType IMPLIES = TokenType.of("implies", "^(->|=>|implies|IMPLIES)");
  static final TokenType BINARY_CONSTANT = TokenType.of("binary constant", "^0b[01]+");
  static final TokenType HEX_CONSTANT = TokenType.of("hex constant", "^0x[0-9a-fA-F]+");
  static final TokenType BITWISE_AND = TokenType.of("bitwise and", "^&");
  static final TokenType BITWISE_OR = TokenType.of("bitwise or", "^\\|");
  static final TokenType BITWISE_XOR = TokenType.of("bitwise xor", "^\\^");
  static final TokenType BITWISE_NOT = TokenType.of("bitwise not", "^~");
  static final TokenType BITWISE_LEFT_SHIFT = TokenType.of("bitwise left shift", "^<<");
  static final TokenType BITWISE_RIGHT_SHIFT = TokenType.of("bitwise right shift", "^>>");
  // static final TokenType BITWISE_UNSIGNED_RIGHT_SHIFT = TokenType.of("bitwise unsigned right shift", "^>>>");

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
