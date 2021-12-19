package me.paultristanwagner.satchecking;

import java.util.List;
import java.util.Map;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

public class DPLLSolver {
    
    private CNF cnf;
    private Assignment lastAssignment;
    
    private Map<Literal, List<Clause>> watchedInMap;
    private Map<Clause, List<Literal>> watchedLiteralsMap;
    
    public void load( CNF cnf ) {
        this.cnf = cnf;
        this.lastAssignment = new Assignment( cnf );
    }
    
    public Assignment nextModel() {
        boolean done = !backtrack( lastAssignment );
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
    
    public static Result check( CNF cnf ) {
        DPLLSolver solver = new DPLLSolver();
        Assignment assignment = new Assignment( cnf );
        return solver.check( cnf, assignment );
    }
    
    public Result check( CNF cnf, Assignment assignment ) {
        if ( !bcp( cnf, assignment ) ) {
            return UNSAT;
        }
        
        while ( true ) {
            if ( assignment.fits( cnf ) ) {
                return SAT( assignment );
            }
            assignment.decide( cnf );
            while ( !bcp( cnf, assignment ) ) {
                if ( !backtrack( assignment ) ) {
                    return UNSAT;
                }
            }
        }
    }
    
    private boolean bcp( CNF cnf, Assignment assignment ) {
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
    
    private boolean backtrack( Assignment assignment ) {
        if ( assignment.isEmpty() ) {
            return true;
        }
        
        while ( !assignment.isEmpty() ) {
            LiteralAssignment la = assignment.getLastDecision();
            if ( la.wasPreviouslyAssigned() ) {
                assignment.undoLastDecision();
            } else {
                boolean newValue = la.toggleValue();
                la.setPreviouslyAssigned();
                if ( !newValue ) {
                    // updateWatchedLiterals( la.getLiteralName() );
                    // todo: update watched literals
                }
                return true;
            }
        }
        return false;
    }
    
    private void updateWatchedLiterals( Literal literal ) {
        List<Clause> watchedIn = watchedInMap.get( literal ); //todo: what if this is empty?
        for ( Clause clause : watchedIn ) {
            List<Literal> watchedLiterals = watchedLiteralsMap.get( clause );
            Literal other = getOtherWatchedLiteral( watchedLiterals, literal );
            // todo: WIP
        }
    }
    
    private Literal getOtherWatchedLiteral( List<Literal> watched, Literal one ) {
        for ( Literal literal : watched ) {
            if ( !literal.equals( one ) ) {
                return literal;
            }
        }
        return null;
    }
}
