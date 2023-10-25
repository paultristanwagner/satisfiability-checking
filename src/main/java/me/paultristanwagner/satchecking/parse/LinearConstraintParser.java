package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;

import java.util.concurrent.atomic.AtomicInteger;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;

public class LinearConstraintParser implements Parser<LinearConstraint> {

  /*
   *  Grammar for Linear constraints:
   *    <S> ::= <TERM> '=' <RATIONAL>
   *          | <TERM> '<=' <RATIONAL>
   *          | <TERM> '>=' <RATIONAL>
   *          | MIN '(' <TERM> ')'
   *          | MAX '(' <TERM> ')'
   *
   *    <TERM> ::= [ <SIGNS> ] [ <RATIONAL> ] IDENTIFIER
   *             | [ <SIGNS> ] [ <RATIONAL> ] IDENTIFIER [ <SIGNS> <TERM> ]
   *
   *    <SIGNS> ::= '+' [ <SIGNS> ]
   *              | '-' [ <SIGNS> ]
   *
   *    <RATIONAL> ::= DECIMAL | FRACTION
   *
   */
  @Override
  public ParseResult<LinearConstraint> parseWithRemaining(String string) {
    Lexer lexer = new LinearConstraintLexer(string);

    lexer.requireNextToken();

    LinearConstraint lc = TERM(lexer);

    return new ParseResult<>(lc, lexer.getRemaining());
  }

  private static LinearConstraint TERM(Lexer lexer) {
    LinearConstraint lc;
    boolean optimization = false;

    if(lexer.getLookahead().getType().equals(MIN)) {
      optimization = true;
      lexer.consume(MIN);
      lexer.consume(LPAREN);
      lc = new MinimizingConstraint();
    } else if(lexer.getLookahead().getType().equals(MAX)) {
      optimization = true;
      lexer.consume(MAX);
      lexer.consume(LPAREN);
      lc = new MaximizingConstraint();
    } else {
      lc = new LinearConstraint();
    }

    double coefficient = OPTIONAL_SIGNS(lexer) * OPTIONAL_RATIONAL(lexer);
    Token variableToken = lexer.getLookahead();
    lexer.consume(IDENTIFIER);
    String variable = variableToken.getValue();
    lc.setCoefficient(variable, coefficient);

    while(lexer.getLookahead().getType().equals(PLUS)
        || lexer.getLookahead().getType().equals(MINUS)
        || lexer.getLookahead().getType().equals(FRACTION)
        || lexer.getLookahead().getType().equals(DECIMAL)
    ) {
      coefficient = OPTIONAL_SIGNS(lexer) * OPTIONAL_RATIONAL(lexer);
      variableToken = lexer.getLookahead();
      lexer.consume(IDENTIFIER);
      variable = variableToken.getValue();
      lc.setCoefficient(variable, coefficient);
    }

    if(optimization) {
      lexer.consume(RPAREN);
      return lc;
    }

    if(lexer.getLookahead().getType().equals(EQUALS)) {
      lexer.consume(EQUALS);
      lc.setBound(EQUAL);
    } else if(lexer.getLookahead().getType().equals(LOWER_EQUALS)) {
      lexer.consume(LOWER_EQUALS);
      lc.setBound(UPPER);
    } else if(lexer.getLookahead().getType().equals(GREATER_EQUALS)) {
      lexer.consume(GREATER_EQUALS);
      lc.setBound(LOWER);
    } else {
      throw new SyntaxError("Relation expected", lexer.getInput(), lexer.getCursor());
    }

    double value = OPTIONAL_SIGNS(lexer) * RATIONAL(lexer);
    lc.setValue(value);

    return lc;
  }

  private static int OPTIONAL_SIGNS(Lexer lexer) {
    if(lexer.getLookahead().getType().equals(PLUS) || lexer.getLookahead().getType().equals(MINUS)) {
      return SIGNS(lexer);
    } else {
      return 1;
    }
  }

  private static int SIGNS(Lexer lexer) {
    int sign = 1;
    do {
      if(lexer.getLookahead().getType().equals(PLUS)) {
        lexer.consume(PLUS);
      } else if(lexer.getLookahead().getType().equals(MINUS)) {
        lexer.consume(MINUS);
        sign *= -1;
      } else {
        throw new SyntaxError("Sign expected", lexer.getInput(), lexer.getCursor());
      }
    } while(lexer.getLookahead().getType().equals(PLUS) || lexer.getLookahead().getType().equals(MINUS));

    return sign;
  }

  private static double OPTIONAL_RATIONAL(Lexer lexer) {
    if(lexer.getLookahead().getType().equals(DECIMAL)) {
      return DECIMAL(lexer);
    } else if(lexer.getLookahead().getType().equals(FRACTION)) {
      return FRACTION(lexer);
    } else {
      return 1;
    }
  }

  private static double RATIONAL(Lexer lexer) {
    if(lexer.getLookahead().getType().equals(DECIMAL)) {
      return DECIMAL(lexer);
    } else {
      return FRACTION(lexer);
    }
  }

  private static double DECIMAL(Lexer lexer) {
    Token token = lexer.getLookahead();
    lexer.consume(DECIMAL);
    return Double.parseDouble(token.getValue());
  }

  private static double FRACTION(Lexer lexer) {
    Token token = lexer.getLookahead();
    lexer.consume(FRACTION);

    String[] parts = token.getValue().split("/");
    return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
  }
}
