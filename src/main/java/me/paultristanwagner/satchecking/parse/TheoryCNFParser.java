package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.TheoryClause;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static me.paultristanwagner.satchecking.parse.Parser.nextProperChar;

public class TheoryCNFParser<T extends Constraint> implements Parser<TheoryCNF<T>> {

  private final Class<T> constraintClass;

  public TheoryCNFParser(Class<T> clazz) {
    this.constraintClass = clazz;
  }

  @Override
  public TheoryCNF<T> parse(String string) {
    return parse(string, new AtomicInteger());
  }

  @Override
  public TheoryCNF<T> parse(String string, AtomicInteger index) {
    List<TheoryClause<T>> clauses = S(string, index);
    return new TheoryCNF<>(clauses);
  }

  /*
     Grammar:
     S -> ( D ) & S
     S -> ( D )
     D -> L | D
     D -> L
     L -> [Constraint]
  */

  private List<TheoryClause<T>> S(String string, AtomicInteger index) {
    if (nextProperChar(string, index) != '(') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("Cannot parse CNF. Expected '('", string, lastIndex);
    }

    List<T> literals = D(string, index);
    TheoryClause<T> clause = new TheoryClause<>(literals);

    if (nextProperChar(string, index) != ')') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("Cannot parse CNF. Expected ')'", string, lastIndex);
    }

    List<TheoryClause<T>> clauses = new ArrayList<>();
    clauses.add(clause);
    if (string.length() == index.get()) {
      return clauses;
    }

    if (nextProperChar(string, index) != '&') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("Expected '&'", string, lastIndex);
    }

    clauses.addAll(S(string, index));
    return clauses;
  }

  private List<T> D(String string, AtomicInteger index) {
    T constraint = L(string, index);
    List<T> constraints = new ArrayList<>();
    constraints.add(constraint);
    if (nextProperChar(string, index) == '|') {
      constraints.addAll(D(string, index));
    } else {
      index.decrementAndGet();
    }
    return constraints;
  }

  private T L(String string, AtomicInteger index) {
    if (nextProperChar(string, index) != '[') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("Expected '['", string, lastIndex);
    }
    int closingIndex = string.indexOf(']', index.get());
    if (closingIndex == -1) {
      throw new SyntaxError("No closing ']' found", string, index.get());
    }
    String subString = string.substring(index.get(), closingIndex);

    T constraint;
    try {
      if (constraintClass == LinearConstraint.class) {
        LinearConstraintParser linearConstraintParser = new LinearConstraintParser();
        constraint = (T) linearConstraintParser.parse(subString);
      } else if (constraintClass == EqualityConstraint.class) {
        EqualityConstraintParser equalityConstraintParser = new EqualityConstraintParser();
        constraint = (T) equalityConstraintParser.parse(subString);
      } else if (constraintClass == EqualityFunctionConstraint.class) {
        EqualityFunctionParser equalityFunctionParser = new EqualityFunctionParser();
        constraint = (T) equalityFunctionParser.parse(subString);
      } else {
        throw new RuntimeException(
            "Cannot parse constraint of type " + constraintClass.getSimpleName());
      }
    } catch (SyntaxError e) {
      throw new SyntaxError(e.getInternalMessage(), string, index.get() + e.getIndex());
    }

    index.addAndGet(subString.length());

    if (nextProperChar(string, index) != ']') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("Expected ']'", string, lastIndex);
    }

    return constraint;
  }

  private static char nextChar(String string, AtomicInteger index) {
    char c;
    while ((c = string.charAt(index.get())) == ' ') {
      index.incrementAndGet();
    }
    return c;
  }
}
