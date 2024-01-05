package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import me.paultristanwagner.satchecking.theory.bitvector.BitVector;
import me.paultristanwagner.satchecking.theory.bitvector.BitVectorFlattener;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorConstraint;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static me.paultristanwagner.satchecking.theory.TheoryResult.satisfiable;

public class BitVectorSolver implements TheorySolver<BitVectorConstraint> {

  private final List<BitVectorConstraint> constraints = new ArrayList<>();

  @Override
  public void clear() {
    constraints.clear();
  }

  @Override
  public void addConstraint(BitVectorConstraint constraint) {
    constraints.add(constraint);
  }

  @Override
  public TheoryResult<BitVectorConstraint> solve() {
    BitVectorFlattener flattener = new BitVectorFlattener();
    CNF cnf = flattener.flatten(constraints);
    SATSolver satSolver = new DPLLCDCLSolver();
    satSolver.load(cnf);

    Assignment assignment = satSolver.nextModel();
    if (assignment == null) {
      return TheoryResult.unsatisfiable(new HashSet<>(constraints));
    }

    List<BitVectorVariable> variables = new ArrayList<>();
    for (BitVectorConstraint constraint : constraints) {
      variables.addAll(constraint.getVariables());
    }

    VariableAssignment variableAssignment = new VariableAssignment();
    for (BitVectorVariable variable : variables) {
      BitVector value = flattener.reconstruct(variable, assignment);
      variableAssignment.assign(variable.getName(), value);
      BigInteger decimalValue = variable.isSigned() ? value.asSignedBigInteger() : value.asUnsignedBigInteger();
      variableAssignment.assign(variable.getName(), value + " (" + decimalValue + ")");
    }

    return satisfiable(variableAssignment);
  }
}
