package me.paultristanwagner.satchecking.sat.solver;

import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.PartialAssignment;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public interface SATSolver {

  void load(CNF cnf);

  Assignment nextModel();

  default PartialAssignment nextPartialAssignment() {
    Assignment model = nextModel();
    if (model == null) {
      return null;
    }
    return PartialAssignment.complete(model);
  }
}
