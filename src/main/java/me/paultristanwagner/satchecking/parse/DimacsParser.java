package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;

import java.util.ArrayList;
import java.util.List;

import static me.paultristanwagner.satchecking.parse.TokenType.IDENTIFIER;
import static me.paultristanwagner.satchecking.parse.TokenType.INTEGER;

public class DimacsParser implements Parser<CNF> {

  @Override
  public ParseResult<CNF> parseWithRemaining(String string) {
    Lexer lexer = new DimacsLexer(string);

    lexer.consume(IDENTIFIER);

    lexer.consume(IDENTIFIER);

    lexer.require(INTEGER);
    int variableCount = Integer.parseInt(lexer.getLookahead().getValue());
    lexer.consume(INTEGER);

    lexer.require(INTEGER);
    int clauseCount = Integer.parseInt(lexer.getLookahead().getValue());
    lexer.consume(INTEGER);

    List<Clause> clauses = new ArrayList<>();
    for (int i = 0; i < clauseCount; ++i) {
      Clause clause = parseClause(lexer);
      clauses.add(clause);
    }

    return new ParseResult<>(new CNF(clauses), lexer.getCursor(), lexer.getRemaining().isEmpty());
  }

  private Clause parseClause(Lexer lexer) {
    List<Literal> literals = new ArrayList<>();

    while(lexer.hasNextToken() && !lexer.getLookahead().getValue().equals("0")) {
      Literal literal = parseLiteral(lexer);
      literals.add(literal);
    }

    lexer.require(INTEGER);
    int zero = Integer.parseInt(lexer.getLookahead().getValue());
    lexer.consume(INTEGER);

    if (zero != 0) {
      throw new SyntaxError("Expected 0, got " + zero, lexer.getCursor());
    }

    return new Clause(literals);
  }

  private Literal parseLiteral(Lexer lexer) {
    lexer.require(INTEGER);
    int literalId = Integer.parseInt(lexer.getLookahead().getValue());
    lexer.consume(INTEGER);

    if (literalId >= 0) {
      return new Literal("l" + literalId);
    } else {
      return new Literal("l" + -literalId, true);
    }
  }
}
