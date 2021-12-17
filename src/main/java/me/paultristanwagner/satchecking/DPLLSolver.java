package me.paultristanwagner.satchecking;

import java.util.ArrayList;
import java.util.List;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

public class DPLLSolver {
    
    public static Result check( CNF cnf ) {
        Assignment assignment = new Assignment();
        if ( !bcp( cnf, assignment ) ) {
            return UNSAT;
        }
        
        while ( true ) {
            if ( assignment.fits( cnf ) ) {
                return SAT( assignment );
            }
            assignment.decide( cnf );
            while ( !bcp( cnf, assignment ) ) {
                if(!assignment.backtrack()) {
                    return UNSAT;
                }
            }
        }
    }
    
    private CNF cnf;
    
    public void load( CNF cnf ) {
        this.cnf = cnf;
    }
    
    public Assignment nextModel() {
        Result result = check( cnf );
        if ( !result.isSatisfiable() ) {
            return null;
        }
        
        List<Clause> clauses = new ArrayList<>( cnf.getClauses() );
        clauses.add( result.getAssignment().not() );
        this.cnf = new CNF( clauses.toArray( new Clause[ 0 ] ) );
        
        return result.getAssignment();
    }
    
    private static boolean bcp( CNF cnf, Assignment assignment ) {
        List<Clause> clauseList = cnf.getClauses();
        
        // While we can find unit clauses
        while ( true ) {
            boolean foundUnitClause = false;
            
            for ( Clause clause : clauseList ) {
                if ( clause.isUnit( assignment ) ) {
                    Literal literal = clause.getUnassignedLiterals( assignment ).get( 0 );
                    assignment.propagate( literal );
                    foundUnitClause = true;
                    break;
                }
            }
            
            if ( !foundUnitClause ) {
                break;
            }
        }
        return assignment.couldSatisfy( cnf );
    }
}
