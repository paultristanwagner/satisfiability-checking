package me.paultristanwagner.satchecking.builder;

import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Clause;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class CNFBuilder {

  protected final List<Clause> clauses;

  protected CNFBuilder() {
    this.clauses = new ArrayList<>();
  }

  public static <X, Y> FunctionCNFBuilder<X, Y> function(List<X> domain, List<Y> codomain) {
    return new FunctionCNFBuilder<>(domain, codomain);
  }

  public CNFBuilder add(Clause clause) {
    clauses.add(clause);
    return this;
  }

  public CNF build() {
    return new CNF(clauses);
  }
}
