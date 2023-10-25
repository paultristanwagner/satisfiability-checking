package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.EqualityConstraint;

import java.util.concurrent.atomic.AtomicInteger;

import static me.paultristanwagner.satchecking.parse.EqualityConstraintLexer.*;
import static me.paultristanwagner.satchecking.parse.TokenType.*;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class EqualityConstraintParser implements Parser<EqualityConstraint> {

  /*
   * Grammar for equality constraints:
   *    <S> ::= IDENTIFIER '=' IDENTIFIER
   *          | IDENTIFIER '!=' IDENTIFIER
   */

  @Override
  public ParseResult<EqualityConstraint> parseWithRemaining(String string) {
    EqualityConstraintLexer lexer = new EqualityConstraintLexer(string);

    lexer.requireNextToken();

    Token a = lexer.getLookahead();
    lexer.consume(IDENTIFIER);

    boolean equal = lexer.getLookahead().getType().equals(EQUALS);
    if(equal) {
      lexer.consume(EQUALS);
    } else {
      lexer.consume(NOT_EQUALS);
    }

    Token b = lexer.getLookahead();
    lexer.consume(IDENTIFIER);

    return new ParseResult<>(new EqualityConstraint(a.getValue(), b.getValue(), equal), lexer.getRemaining());
  }
}
