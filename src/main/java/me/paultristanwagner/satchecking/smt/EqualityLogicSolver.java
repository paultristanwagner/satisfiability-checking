package me.paultristanwagner.satchecking.smt;

import me.paultristanwagner.satchecking.*;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import me.paultristanwagner.satchecking.theory.EqualityLogic;
import me.paultristanwagner.satchecking.theory.EqualityLogic.EqualityLogicResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// todo: proof of concept
public class EqualityLogicSolver implements SMTSolver<EqualityConstraint> {

    @Override
    public VariableAssignment solve( TheoryCNF<EqualityConstraint> cnf ) {
        Solver satSolver = new DPLLCDCLSolver();
        satSolver.load( cnf.getBooleanStructure() );

        Assignment assignment;
        while ( ( assignment = satSolver.nextModel() ) != null ) {
            List<Literal> trueLiterals = assignment.getTrueLiterals();

            EqualityLogic equalityLogic = new EqualityLogic();
            Set<EqualityConstraint> selectedConstraints = new HashSet<>();
            for ( Literal trueLiteral : trueLiterals ) {
                EqualityConstraint constraint = cnf.getConstraintLiteralMap().inverse().get( trueLiteral.getName() );

                selectedConstraints.add( constraint );
            }

            EqualityLogicResult equalityLogicResult = equalityLogic.solve( selectedConstraints );
            if ( equalityLogicResult.isSatisfiable() ) {
                return equalityLogicResult.getAssignment();
            } else {
                Set<EqualityConstraint> explanation = equalityLogicResult.getExplanation();
                List<Literal> literals = new ArrayList<>();
                for ( EqualityConstraint equalityConstraint : explanation ) {
                    String literalName = cnf.getConstraintLiteralMap().get( equalityConstraint );
                    literals.add( new Literal( literalName ).not() );
                }

                Clause clause = new Clause( literals );

                // todo: We need to exclude the clause from the SAT solver
                // How can we do this? Conflict resolution?
                // We need to be careful to not exclude the remaining assignment from the SAT solver!
            }
        }

        return null;
    }
}
