package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.TheoryClause;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint;

import java.util.ArrayList;
import java.util.List;

import static me.paultristanwagner.satchecking.parse.TokenType.*;

public class TheoryCNFParser<T extends Constraint> implements Parser<TheoryCNF<T>> {

  private final Class<T> constraintClass;

  public TheoryCNFParser(Class<T> clazz) {
    this.constraintClass = clazz;
  }

  @Override
  public ParseResult<TheoryCNF<T>> parseWithRemaining(String string) {
    Lexer lexer = new TheoryCNFLexer(string);

    lexer.requireNextToken();

    List<TheoryClause<T>> clauses = S(lexer);

    return new ParseResult<>(new TheoryCNF<>(clauses), lexer.getCursor(), lexer.getCursor() == string.length());
  }

  /*
   * Grammar for theory CNF:
   *    <S> ::= <CLAUSE> { '&' <CLAUSE> }
   *
   *    <CLAUSE> ::= '(' LITERAL { '|' LITERAL } ')'
   *
   */

  private List<TheoryClause<T>> S(Lexer lexer) {
    List<TheoryClause<T>> clauses = new ArrayList<>();

    List<T> literals = CLAUSE(lexer);
    TheoryClause<T> clause = new TheoryClause<>(literals);
    clauses.add(clause);

    while(lexer.canConsume(AND)) {
      lexer.consume(AND);
      literals = CLAUSE(lexer);
      clause = new TheoryClause<>(literals);
      clauses.add(clause);
    }

    return clauses;
  }

  private List<T> CLAUSE(Lexer lexer) {
    lexer.consume(LPAREN);

    List<T> constraints = new ArrayList<>();
    constraints.add(LITERAL(lexer));

    while(lexer.canConsume(OR)) {
      lexer.consume(OR);
      constraints.add(LITERAL(lexer));
    }

    lexer.consume(RPAREN);

    return constraints;
  }

  private T LITERAL(Lexer lexer) {
    String remaining = lexer.getRemaining();

    ParseResult<T> parseResult;
    try {
      if (constraintClass == LinearConstraint.class) {
        LinearConstraintParser linearConstraintParser = new LinearConstraintParser();
        parseResult = (ParseResult<T>) linearConstraintParser.parseWithRemaining(remaining);
      } else if (constraintClass == EqualityConstraint.class) {
        EqualityConstraintParser equalityConstraintParser = new EqualityConstraintParser();
        parseResult = (ParseResult<T>) equalityConstraintParser.parseWithRemaining(remaining);
      } else if (constraintClass == EqualityFunctionConstraint.class) {
        EqualityFunctionParser equalityFunctionParser = new EqualityFunctionParser();
        parseResult = (ParseResult<T>) equalityFunctionParser.parseWithRemaining(remaining);
      } else {
        throw new RuntimeException(
            "Cannot parse constraint of type " + constraintClass.getSimpleName());
      }
    } catch (SyntaxError e) {
      throw new SyntaxError(e.getInternalMessage(), remaining, e.getIndex());
    }

    lexer.skip(parseResult.charsRead());

    return parseResult.result();
  }
}
