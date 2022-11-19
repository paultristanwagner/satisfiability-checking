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
        while ( true ) {
            String line = scanner.nextLine();
            if ( line.equals( "solve" ) ) {
                break;
            }

            EqualityConstraint c = parser.parse( line );
            solver.addConstraint( c );
        }

        EqualityLogicResult result = solver.checkConsistency();
        if ( result.isSatisfiable() ) {
            System.out.println( "SAT!" );
            System.out.println( result.getAssignment() );
        } else {
            System.out.println( "UNSAT!" );
            System.out.println( result.getExplanation() );
        }
    }

    private final List<EqualityConstraint> constraints;
    private final List<EqualityConstraint> equalities;
    private final List<EqualityConstraint> inequalities;
    private final Map<String, String> pointers;
    private final Map<String, Integer> ranks;

    public EqualityLogic() {
        this.constraints = new LinkedList<>();
        this.equalities = new LinkedList<>();
        this.inequalities = new LinkedList<>();
        this.pointers = new HashMap<>();
        this.ranks = new HashMap<>();
    }

    public void addConstraint( EqualityConstraint constraint ) {
        this.constraints.add( constraint );

        String lhs = constraint.getLeft();
        String rhs = constraint.getRight();
        this.pointers.putIfAbsent( lhs, lhs );
        this.pointers.putIfAbsent( rhs, rhs );

        this.ranks.putIfAbsent( lhs, 1 );
        this.ranks.putIfAbsent( rhs, 1 );

        if ( constraint.areEqual() ) {
            addEquality( constraint );
        } else {
            addInequality( constraint );
        }
    }

    private void addEquality( EqualityConstraint equality ) {
        this.equalities.add( equality );
        union( equality.getLeft(), equality.getRight() );
    }

    private void addInequality( EqualityConstraint inequality ) {
        this.inequalities.add( inequality );
    }

    public EqualityLogicResult checkConsistency() {
        for ( EqualityConstraint inequality : inequalities ) {
            String aRoot = find( inequality.getLeft() );
            String bRoot = find( inequality.getRight() );
            if ( aRoot.equals( bRoot ) ) {
                Set<EqualityConstraint> explanation = constructEqualityPath( inequality.getLeft(), inequality.getRight() );
                explanation.add( inequality );
                return EqualityLogicResult.unsatisfiable( explanation );
            }
        }

        // todo: Put this code into an extra method
        VariableAssignment assignment = new VariableAssignment();
        Map<String, Integer> rootMapping = new HashMap<>();
        int i = 0;
        for ( String variable : pointers.keySet() ) {
            String root = find( variable );
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

    private Set<EqualityConstraint> constructEqualityPath( String a, String b ) {
        Map<String, List<Pair<String, EqualityConstraint>>> neighbors = new HashMap<>();
        Map<String, Boolean> visited = new HashMap<>();
        Map<String, Pair<String, EqualityConstraint>> previous = new HashMap<>();
        for ( EqualityConstraint equality : equalities ) {
            if ( !neighbors.containsKey( equality.getLeft() ) ) {
                neighbors.put( equality.getLeft(), new ArrayList<>() );
                visited.put( equality.getLeft(), false );
                previous.put( equality.getLeft(), null );
            }
            if ( !neighbors.containsKey( equality.getRight() ) ) {
                neighbors.put( equality.getRight(), new ArrayList<>() );
                visited.put( equality.getRight(), false );
                previous.put( equality.getRight(), null );
            }

            neighbors.get( equality.getLeft() ).add( Pair.of( equality.getRight(), equality ) );
            neighbors.get( equality.getRight() ).add( Pair.of( equality.getLeft(), equality ) );
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

    private String find( String variable ) {
        String current = variable;
        while ( !pointers.get( current ).equals( current ) ) {
            current = pointers.get( current );
        }
        return current;
    }

    private void union( String a, String b ) {
        String aRoot = find( a );
        String bRoot = find( b );
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
