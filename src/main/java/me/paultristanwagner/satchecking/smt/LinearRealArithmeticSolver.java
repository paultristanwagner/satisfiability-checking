package me.paultristanwagner.satchecking.smt;

import me.paultristanwagner.satchecking.*;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import me.paultristanwagner.satchecking.theory.Simplex2;
import me.paultristanwagner.satchecking.theory.Simplex2.SimplexResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LinearRealArithmeticSolver implements SMTSolver<LinearConstraint> {

    public VariableAssignment solve( TheoryCNF<LinearConstraint> cnf ) {
        // todo: proof of concept
        Solver satSolver = new DPLLCDCLSolver();
        satSolver.load( cnf.getBooleanStructure() );

        Assignment assignment;
        while ( ( assignment = satSolver.nextModel() ) != null ) {
            List<Literal> trueLiterals = assignment.getTrueLiterals();

            Simplex2 simplex = new Simplex2();
            List<LinearConstraint> selectedConstraints = new ArrayList<>();
            for ( Literal trueLiteral : trueLiterals ) {
                LinearConstraint constraint = cnf.getConstraintLiteralMap().inverse().get( trueLiteral.getName() );

                if ( constraint instanceof MaximizingConstraint ) {
                    simplex.maximize( constraint );
                } else if ( constraint instanceof MinimizingConstraint ) {
                    simplex.minimize( constraint );
                } else {
                    simplex.addConstraint( constraint );
                }
                selectedConstraints.add( constraint );
            }

            SimplexResult simplexResult = simplex.solve();
            if ( !simplex.hasObjectiveFunction() && simplexResult.isFeasible() ||
                    simplex.hasObjectiveFunction() && simplexResult.isOptimal() ) {
                return simplexResult.getSolution();
            } else {
                Set<LinearConstraint> explanation;

                if ( simplexResult.getExplanation() != null ) {
                    explanation = simplexResult.getExplanation();
                } else {
                    explanation = new HashSet<>( selectedConstraints );
                }

                List<Literal> literals = new ArrayList<>();
                for ( LinearConstraint linearConstraint : explanation ) {
                    String literalName = cnf.getConstraintLiteralMap().get( linearConstraint );
                    literals.add( new Literal( literalName ).not() );
                }

                Clause clause = new Clause( literals );
                cnf.getBooleanStructure().learnClause( clause );
            }
        }

        return null;
    }
}
