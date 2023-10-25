package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    boolean equal = lexer.getLookahead().getType().equals(EQUALS);
    if(equal) {
      lexer.consume(EQUALS);
    } else {
      lexer.consume(NOT_EQUALS);
    }

    Function right = FUNCTION(lexer);

    return new ParseResult<>(new EqualityFunctionConstraint(left, right, equal), lexer.getRemaining());
  }

  private static Function FUNCTION(Lexer lexer) {
    Token functionNameToken = lexer.getLookahead();
    lexer.consume(IDENTIFIER);

    String functionName = functionNameToken.getValue();

    List<Function> parameters = new ArrayList<>();

    if(!lexer.hasNextToken() || !lexer.getLookahead().getType().equals(LPAREN)) {
      return Function.of(functionName, parameters);
    }

    lexer.consume(LPAREN);

    while(lexer.hasNextToken()) {
      parameters.add(FUNCTION(lexer));
      if(lexer.getLookahead().getType().equals(RPAREN)) {
        break;
      }
      lexer.consume(COMMA);
    }

    lexer.consume(RPAREN);

    return Function.of(functionName, parameters);
  }
}
