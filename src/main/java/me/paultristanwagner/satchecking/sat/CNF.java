package me.paultristanwagner.satchecking.sat;

import me.paultristanwagner.satchecking.parse.SyntaxError;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static me.paultristanwagner.satchecking.parse.Parser.nextProperChar;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class CNF {

  private final List<Clause> initialClauses;
  private final List<Clause> clauses;
  private final List<Literal> orderedLiterals;
  private final Set<Literal> literals;

  public CNF(List<Clause> clauses) {
    this.initialClauses = new ArrayList<>(clauses);
    this.clauses = clauses;
    this.orderedLiterals = new ArrayList<>();
    this.literals = new HashSet<>();

    for (Clause clause : clauses) {
      for (Literal literal : clause.getLiterals()) {
        if(!literals.contains(literal)) {
          literals.add(literal);
          orderedLiterals.add(literal);
        }
      }
    }
  }

  public void learnClause(Clause clause) {
    clauses.add(clause);

    for (Literal literal : clause.getLiterals()) {
      if (!orderedLiterals.contains(literal)) {
        orderedLiterals.add(literal);
      }
    }
  }

  public List<Clause> getClauses() {
    return clauses;
  }

  public List<Literal> getLiterals() {
    return orderedLiterals;
  }

  /*
     Grammar:
     S -> ( D ) & S
     S -> ( D )
     D -> L | D
     D -> L
     L -> ~<literal name>
     L -> <literal name>
  */

  public static CNF parse(String string) {
    AtomicInteger index = new AtomicInteger(0);
    List<Clause> clauses = S(string, index);
    return new CNF(clauses);
  }

  private static List<Clause> S(String string, AtomicInteger index) {
    if (nextProperChar(string, index) != '(') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("Cannot parse CNF. Expected '('", string, lastIndex);
    }

    List<Literal> literals = D(string, index);
    Clause clause = new Clause(literals);

    if (nextProperChar(string, index) != ')') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("Cannot parse CNF. Expected ')'", string, lastIndex);
    }

    List<Clause> clauses = new ArrayList<>();
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

  public static List<Literal> D(String string, AtomicInteger index) {
    Literal literal = L(string, index);
    List<Literal> literals = new ArrayList<>();
    literals.add(literal);
    if (nextProperChar(string, index) == '|') {
      literals.addAll(D(string, index));
    } else {
      index.decrementAndGet();
    }
    return literals;
  }

  private static Literal L(String string, AtomicInteger index) {
    boolean negated = false;
    if (nextProperChar(string, index) == '~') {
      negated = true;
    } else {
      index.decrementAndGet();
    }
    
    StringBuilder sb = new StringBuilder();
    while (true) {
      boolean hasNext = index.get() < string.length();
      char c = hasNext ? string.charAt(index.get()) : ' ';
      if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_') {
        if (sb.isEmpty()) {
          throw new SyntaxError("Literal expected, got '" + c + "' instead", string, index.get());
        }
        break;
      }
      index.incrementAndGet();
      sb.append(c);
    }

    return new Literal(sb.toString(), negated);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Clause clause : initialClauses) {
      sb.append(" & (").append(clause).append(")");
    }
    return sb.substring(3);
  }

  public List<Clause> getInitialClauses() {
    return initialClauses;
  }
}
