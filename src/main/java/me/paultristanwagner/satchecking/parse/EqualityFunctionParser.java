package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint.Function;

import java.util.ArrayList;
import java.util.List;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class EqualityFunctionParser implements Parser<EqualityFunctionConstraint> {

  /*
   * Grammar for equality function constraints:
   *    <S> ::= <FUN> '=' <FUN>
   *          | <FUN> '!=' <FUN>
   *
   *    <FUN> ::= IDENTIFIER
   *            | IDENTIFIER '(' <FUN> { ',' <FUN> } ')'
   *
   */

  @Override
  public ParseResult<EqualityFunctionConstraint> parseWithRemaining(String string) {
    Lexer lexer = new EqualityFunctionLexer(string);

    lexer.requireNextToken();

    Function left = FUNCTION(lexer);

    lexer.requireEither(EQUALS, NOT_EQUALS);
    boolean equal = lexer.canConsume(EQUALS);
    if(equal) {
      lexer.consume(EQUALS);
    } else {
      lexer.consume(NOT_EQUALS);
    }

    Function right = FUNCTION(lexer);

    return new ParseResult<>(
        new EqualityFunctionConstraint(left, right, equal),
        lexer.getCursor(),
        lexer.getCursor() == string.length()
    );
  }

  private static Function FUNCTION(Lexer lexer) {
    lexer.canConsume(IDENTIFIER);
    Token functionNameToken = lexer.getLookahead();
    lexer.consume(IDENTIFIER);

    String functionName = functionNameToken.getValue();

    List<Function> parameters = new ArrayList<>();

    if(!lexer.canConsume(LPAREN)) {
      return Function.of(functionName, parameters);
    }

    lexer.consume(LPAREN);

    while(true) {
      parameters.add(FUNCTION(lexer));
      if(lexer.canConsume(RPAREN)) {
        break;
      }

      lexer.consume(COMMA);
    }

    lexer.consume(RPAREN);

    return Function.of(functionName, parameters);
  }
}
