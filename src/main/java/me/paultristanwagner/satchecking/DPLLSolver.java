package me.paultristanwagner.satchecking;

import java.util.*;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class DPLLSolver {

    private CNF cnf;
    private Assignment assignment;

    private Map<Literal, List<Clause>> watchedInMap;
    private Map<Clause, WatchedLiteralPair> watchedLiteralsMap;

    public void load( CNF cnf ) {
        this.cnf = cnf;
        this.assignment = new Assignment();
        this.watchedInMap = new HashMap<>();
        this.watchedLiteralsMap = new HashMap<>();

        for ( Clause clause : cnf.getClauses() ) {
            WatchedLiteralPair wlp = new WatchedLiteralPair( clause, assignment );
            watchedLiteralsMap.put( clause, wlp );

            for ( Literal literal : wlp.getWatched() ) {
                List<Clause> watchedIn = watchedInMap.getOrDefault( literal, new ArrayList<>() );
                watchedIn.add( clause );
                watchedInMap.put( literal, watchedIn );
            }
        }
    }

    public Assignment nextModel() {
        boolean done = !backtrack( assignment );
        if ( done ) {
            return null;
        }

        Result result = check( cnf, assignment );
        if ( !result.isSatisfiable() ) {
            return null;
        }

        return result.getAssignment();
    }

    public static Result check( CNF cnf ) {
        DPLLSolver solver = new DPLLSolver();
        solver.load( cnf );
        Assignment assignment = new Assignment();
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
            decide( assignment );
            while ( !bcp( cnf, assignment ) ) {
                if ( !backtrack( assignment ) ) {
                    return UNSAT;
                }
            }
        }
    }

    private void decide( Assignment assignment ) {
        List<Literal> literals = cnf.getLiterals();
        Optional<Literal> unassignedOptional = literals.stream().filter( lit -> !assignment.assigns( lit ) ).findAny();
        if ( unassignedOptional.isEmpty() ) {
            throw new IllegalStateException( "Cannot decide because all literals are assigned" );
        }
        Literal literal = unassignedOptional.get();
        assignment.assign( literal, true );
        updateWatchedLiterals( literal.not(), assignment );
    }

    private boolean bcp( CNF cnf, Assignment assignment ) {
        List<Clause> clauseList = cnf.getClauses();

        // While we can find unit clauses
        while ( true ) {
            boolean foundUnitClause = false;

            for ( Clause clause : clauseList ) {
                WatchedLiteralPair wlp = watchedLiteralsMap.get( clause );
                if ( wlp.isConflicting( assignment ) ) {
                    return false;
                }

                Literal unitLiteral = wlp.getUnitLiteral( assignment );
                if ( unitLiteral != null ) {
                    foundUnitClause = true;
                    assignment.force( unitLiteral );
                    updateWatchedLiterals( unitLiteral.not(), assignment );
                    break;
                }
            }

            if ( !foundUnitClause ) {
                return true;
            }
        }
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

                Literal literal = new Literal( la.getLiteralName(), newValue );
                updateWatchedLiterals( literal, assignment );

                return true;
            }
        }
        return false;
    }

    private void updateWatchedLiterals( Literal literal, Assignment assignment ) {
        List<Clause> watchedInCopy = new ArrayList<>( watchedInMap.getOrDefault( literal, new ArrayList<>() ) );
        for ( Clause clause : watchedInCopy ) {
            WatchedLiteralPair wlp = watchedLiteralsMap.get( clause );
            Literal replacement = wlp.attemptReplace( literal, assignment );
            if ( replacement != null ) {
                watchedInMap.get( literal ).remove( clause );

                List<Clause> watchedIn = watchedInMap.getOrDefault( replacement, new ArrayList<>() );
                watchedIn.add( clause );
                watchedInMap.put( replacement, watchedIn );
            }
        }
    }
}
