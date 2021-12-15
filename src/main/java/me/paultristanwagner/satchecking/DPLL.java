package me.paultristanwagner.satchecking;

import java.util.List;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

public class DPLL {
    
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
                assignment.backtrack();
            }
        }
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
            
            if(!foundUnitClause) {
                break;
            }
        }
        return assignment.couldSatisfy( cnf );
    }
}
