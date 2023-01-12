package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.TheoryResult;

import java.util.Set;

public interface TheorySolver<T extends Constraint> {

  void clear();

  default void load(Set<T> constraints) {
    for (T constraint : constraints) {
      addConstraint(constraint);
    }
  }

  void addConstraint(T constraint);

  TheoryResult<T> solve();
}
