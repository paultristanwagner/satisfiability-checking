package me.paultristanwagner.satchecking;

import java.util.List;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

public class DPLLSolver {
    
    public static Result check( CNF cnf ) {
        Assignment assignment = new Assignment();
        return check( cnf, assignment );
    }
    
    public static Result check( CNF cnf, Assignment assignment ) {
        if ( !bcp( cnf, assignment ) ) {
            return UNSAT;
        }
        
        while ( true ) {
            if ( assignment.fits( cnf ) ) {
                return SAT( assignment );
            }
            assignment.decide( cnf );
            while ( !bcp( cnf, assignment ) ) {
                if ( !assignment.backtrack() ) {
                    return UNSAT;
                }
            }
        }
    }
    
    private CNF cnf;
    private Assignment lastAssignment;
    
    public void load( CNF cnf ) {
        this.cnf = cnf;
        this.lastAssignment = new Assignment();
    }
    
    public Assignment nextModel() {
        boolean done = !lastAssignment.backtrack();
        if ( done ) {
            return null;
        }
        
        Result result = check( cnf, lastAssignment );
        if ( !result.isSatisfiable() ) {
            return null;
        }
        
        cnf.learnClause( result.getAssignment().not() );
        
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
