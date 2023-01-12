package me.paultristanwagner.satchecking.sat;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class WatchedLiteralPair {

  private final Clause clause;
  private final Literal[] watched;

  public WatchedLiteralPair(Clause clause, Assignment assignment) {
    this.clause = clause;
    this.watched = new Literal[Math.min(2, clause.getLiterals().size())];

    int correct = 0;
    int incorrect = 0;
    for (int i = 0; i < clause.getLiterals().size(); i++) {
      Literal literal = clause.getLiterals().get(i);
      if (!assignment.assigns(literal) || assignment.evaluate(literal)) {
        watched[correct] = literal;
        correct++;
        if (correct == watched.length) {
          return;
        }
      } else {
        if (correct == 0) {
          watched[(1 - incorrect % 2) % watched.length] = literal;
        } else if (correct == 1) {
          watched[1] = literal;
        }
        incorrect++;
      }
    }
  }

  public boolean isConflicting(Assignment assignment) {
    for (Literal literal : watched) {
      if (!assignment.assigns(literal) || assignment.evaluate(literal)) {
        return false;
      }
    }
    return true;
  }

  // todo: Remove this, just for testing
  private void checkActuallyConflicting(Assignment assignment) {
    System.out.println("Debug code: checkActuallyConflicting");
    for (Literal literal : clause.getLiterals()) {
      if (!assignment.assigns(literal) || assignment.evaluate(literal)) {
        System.out.println("not actually conflicting!");
        return;
      }
    }
  }

  public Literal getUnitLiteral(Assignment assignment) {
    int unassignedCount = 0;
    Literal unassignedLiteral = null;
    for (Literal literal : watched) {
      if (!assignment.assigns(literal)) {
        unassignedCount++;
        unassignedLiteral = literal;
      } else if (assignment.evaluate(literal)) {
        return null;
      }
    }

    if (unassignedCount == 1) {
      return unassignedLiteral;
    }
    return null;
  }

  // todo: Remove this, just for testing
  private void checkActuallyUnit(Assignment assignment) {
    System.out.println("Debug code: checkActuallyUnit");
    int unassignedCount = 0;
    for (Literal literal : clause.getLiterals()) {
      if (!assignment.assigns(literal)) {
        unassignedCount++;
      } else if (assignment.evaluate(literal)) {
        System.out.println(clause + " not actually unit. reason: clause is true");
        return;
      }
    }

    if (unassignedCount != 1) {
      System.out.println(clause + " not actually unit. reason: " + unassignedCount + " unassigned");
    }
  }

  public Literal attemptReplace(Literal literal, Assignment assignment) {
    Literal other = getOther(literal);
    if (other != null && assignment.assigns(other) && assignment.evaluate(other)) {
      return null;
    }

    Literal replacement = null;
    for (Literal lit : clause.getLiterals()) {
      if (!lit.equals(literal)
          && !isWatched(lit)
          && (!assignment.assigns(lit) || assignment.evaluate(lit))) {
        replacement = lit;
        break;
      }
    }

    if (replacement != null) {
      replace(literal, replacement);
    }
    return replacement;
  }

  private void replace(Literal replaced, Literal replacement) {
    for (int i = 0; i < watched.length; i++) {
      Literal lit = watched[i];
      if (lit.equals(replaced)) {
        watched[i] = replacement;
        break;
      }
    }
  }

  public boolean isWatched(Literal literal) {
    for (Literal lit : watched) {
      if (lit.equals(literal)) {
        return true;
      }
    }
    return false;
  }

  public Literal getOther(Literal one) {
    for (Literal literal : watched) {
      if (!literal.equals(one)) {
        return literal;
      }
    }
    return null;
  }

  public Literal[] getWatched() {
    return watched;
  }
}
