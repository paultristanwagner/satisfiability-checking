package me.paultristanwagner.satchecking.parse;

/**
 * Lexer for a subset of SMT-LIB 2.6 scripts.
 *
 * <p>Tokenizes parentheses, numerals, decimals, keywords (e.g. {@code :status}), string literals
 * and SMT-LIB symbols. Line comments starting with {@code ;} are treated as whitespace and skipped.
 *
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 */
public class SmtLibLexer extends Lexer {

  // ; comment until end of line is skipped like whitespace.
  public static final TokenType COMMENT = TokenType.of("comment", "^;[^\\n]*");

  public static final TokenType LPAREN = TokenType.of("(", "^\\(");
  public static final TokenType RPAREN = TokenType.of(")", "^\\)");

  // A decimal numeral, e.g. 3.14 (must be tried before NUMERAL).
  public static final TokenType DECIMAL = TokenType.of("decimal", "^\\d+\\.\\d+");

  // A non-negative integer numeral, e.g. 0, 42.
  public static final TokenType NUMERAL = TokenType.of("numeral", "^\\d+");

  // A keyword, e.g. :status, :smt-lib-version.
  public static final TokenType KEYWORD = TokenType.of("keyword", "^:[a-zA-Z0-9_+\\-*/=%?!.$_~&^<>@]+");

  // A string literal "...". Two consecutive double quotes are an escaped quote.
  public static final TokenType STRING = TokenType.of("string", "^\"(\"\"|[^\"])*\"");

  // An SMT-LIB symbol. Covers simple symbols and the special operator characters.
  // Note: a leading digit is excluded so numerals win.
  public static final TokenType SYMBOL =
      TokenType.of("symbol", "^[a-zA-Z+\\-*/=%?!.$_~&^<>@][a-zA-Z0-9+\\-*/=%?!.$_~&^<>@]*");

  // A quoted symbol |...|.
  public static final TokenType QUOTED_SYMBOL = TokenType.of("quoted symbol", "^\\|[^|]*\\|");

  public SmtLibLexer(String input) {
    super(input);

    // The COMMENT type must be skipped like whitespace. We register it first so it is matched
    // eagerly, then handle skipping below.
    registerTokenTypes(
        COMMENT,
        LPAREN,
        RPAREN,
        DECIMAL,
        NUMERAL,
        KEYWORD,
        STRING,
        QUOTED_SYMBOL,
        SYMBOL);

    initialize(input);
  }
}
