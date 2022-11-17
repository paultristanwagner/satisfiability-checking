package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.parse.EqualityConstraintParser;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

// todo: proof of concept
public class EqualityLogic {

    // todo: remove later
    public static void main( String[] args ) {
        Scanner scanner = new Scanner( System.in );
        EqualityConstraintParser parser = new EqualityConstraintParser();
        EqualityLogic solver = new EqualityLogic();
        Set<EqualityConstraint> constraints = new HashSet<>();
        while ( true ) {
            String line = scanner.nextLine();
            if ( line.equals( "solve" ) ) {
                break;
            }

            EqualityConstraint c = parser.parse( line );
            constraints.add( c );
        }

        EqualityLogicResult result = solver.solve( constraints );
        if ( result.isSatisfiable() ) {
            System.out.println( "SAT!" );
            System.out.println( result.getAssignment() );
        } else {
            System.out.println( "UNSAT!" );
            System.out.println( result.getExplanation() );
        }
    }

    public EqualityLogicResult solve( Set<EqualityConstraint> constraints ) {
        Map<String, String> pointers = new HashMap<>();
        Map<String, Integer> ranks = new HashMap<>();

        for ( EqualityConstraint constraint : constraints ) {
            pointers.put( constraint.getLeft(), constraint.getLeft() );
            pointers.put( constraint.getRight(), constraint.getRight() );

            ranks.put( constraint.getLeft(), 1 );
            ranks.put( constraint.getRight(), 1 );
        }

        for ( EqualityConstraint constraint : constraints ) {
            if ( constraint.areEqual() ) {
                union( constraint.getLeft(), constraint.getRight(), pointers, ranks );
            }
        }

        for ( EqualityConstraint constraint : constraints ) {
            if ( !constraint.areEqual() ) {
                String aRoot = find( constraint.getLeft(), pointers );
                String bRoot = find( constraint.getRight(), pointers );
                if ( aRoot.equals( bRoot ) ) {
                    Set<EqualityConstraint> explanation = constructPath( constraints, constraint.getLeft(), constraint.getRight() );
                    explanation.add( constraint );
                    return EqualityLogicResult.unsatisfiable( explanation );
                }
            }
        }

        VariableAssignment assignment = new VariableAssignment();
        Map<String, Integer> rootMapping = new HashMap<>();
        int i = 0;
        for ( String variable : pointers.keySet() ) {
            String root = find( variable, pointers );
            if ( rootMapping.containsKey( root ) ) {
                int v = rootMapping.get( root );
                assignment.assign( variable, v );
            } else {
                assignment.assign( variable, i );
                rootMapping.put( root, i );
                i++;
            }
        }
        return EqualityLogicResult.satisfiable( assignment );
    }

    // todo: bad code, improve later
    // We cannot directly use the pointers map, because it does not represent an actual path
    private Set<EqualityConstraint> constructPath( Set<EqualityConstraint> constraints, String a, String b ) {
        Map<String, List<Pair<String, EqualityConstraint>>> neighbors = new HashMap<>();
        Map<String, Boolean> visited = new HashMap<>();
        Map<String, Pair<String, EqualityConstraint>> previous = new HashMap<>();
        for ( EqualityConstraint constraint : constraints ) {
            if ( !neighbors.containsKey( constraint.getLeft() ) ) {
                neighbors.put( constraint.getLeft(), new ArrayList<>() );
                visited.put( constraint.getLeft(), false );
                previous.put( constraint.getLeft(), null );
            }
            if ( !neighbors.containsKey( constraint.getRight() ) ) {
                neighbors.put( constraint.getRight(), new ArrayList<>() );
                visited.put( constraint.getRight(), false );
                previous.put( constraint.getRight(), null );
            }

            if ( constraint.areEqual() ) {
                neighbors.get( constraint.getLeft() ).add( Pair.of( constraint.getRight(), constraint ) );
                neighbors.get( constraint.getRight() ).add( Pair.of( constraint.getLeft(), constraint ) );
            }
        }

        Queue<String> queue = new LinkedList<>();
        queue.add( a );
        visited.put( a, true );
        while ( !queue.isEmpty() ) {
            String current = queue.poll();
            if ( current.equals( b ) ) {
                break;
            }

            for ( Pair<String, EqualityConstraint> neighbor : neighbors.get( current ) ) {
                if ( !visited.get( neighbor.getLeft() ) ) {
                    queue.add( neighbor.getLeft() );
                    visited.put( neighbor.getLeft(), true );
                    previous.put( neighbor.getLeft(), Pair.of( current, neighbor.getRight() ) );
                }
            }
        }

        Set<EqualityConstraint> path = new HashSet<>();
        String current = b;
        while ( !current.equals( a ) ) {
            Pair<String, EqualityConstraint> p = previous.get( current );
            path.add( p.getRight() );
            current = p.getLeft();
        }

        return path;
    }

    private String find( String variable, Map<String, String> pointers ) {
        String current = variable;
        while ( !pointers.get( current ).equals( current ) ) {
            current = pointers.get( current );
        }
        return current;
    }

    private void union( String a, String b, Map<String, String> pointers, Map<String, Integer> ranks ) {
        String aRoot = find( a, pointers );
        String bRoot = find( b, pointers );
        if ( aRoot.equals( bRoot ) ) {
            return;
        }

        int aRank = ranks.get( aRoot );
        int bRank = ranks.get( bRoot );
        if ( aRank > bRank ) {
            pointers.put( bRoot, aRoot );
            ranks.put( aRoot, aRank + bRank );
        } else {
            pointers.put( aRoot, bRoot );
            ranks.put( bRoot, aRank + bRank );
        }
    }

    public static class EqualityLogicResult {

        private final boolean satisfiable;
        private final VariableAssignment assignment;
        private final Set<EqualityConstraint> explanation;

        private EqualityLogicResult( boolean satisfiable, VariableAssignment assignment, Set<EqualityConstraint> explanation ) {
            this.satisfiable = satisfiable;
            this.assignment = assignment;
            this.explanation = explanation;
        }

        public static EqualityLogicResult unsatisfiable( Set<EqualityConstraint> explanation ) {
            return new EqualityLogicResult( false, null, explanation );
        }

        public static EqualityLogicResult satisfiable( VariableAssignment assignment ) {
            return new EqualityLogicResult( true, assignment, null );
        }

        public boolean isSatisfiable() {
            return satisfiable;
        }

        public VariableAssignment getAssignment() {
            return assignment;
        }

        public Set<EqualityConstraint> getExplanation() {
            return explanation;
        }
    }
}
