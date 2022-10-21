package me.paultristanwagner.satchecking.smt;

import me.paultristanwagner.satchecking.*;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.Simplex;
import me.paultristanwagner.satchecking.theory.Simplex.SimplexResult;

import java.util.ArrayList;
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
            
            Simplex simplex = new Simplex();
            for ( Literal trueLiteral : trueLiterals ) {
                LinearConstraint constraint = cnf.getConstraintLiteralMap().inverse().get( trueLiteral.getName() );
                simplex.addConstraint( constraint );
            }
            
            SimplexResult simplexResult = simplex.solve();
            if ( simplexResult.isFeasible() ) {
                return simplexResult.getSolution();
            } else {
                Set<LinearConstraint> explanation = simplexResult.getExplanation();
                
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
