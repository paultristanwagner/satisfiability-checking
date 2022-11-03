package me.paultristanwagner.satchecking.smt;

import me.paultristanwagner.satchecking.parse.EqualityConstraintParser;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;

import java.util.*;

// todo: proof of concept
public class EqualityLogicSolver implements TheorySolver<EqualityConstraint> {

    // todo: remove later
    public static void main( String[] args ) {
        Scanner scanner = new Scanner( System.in );
        EqualityConstraintParser parser = new EqualityConstraintParser();
        EqualityLogicSolver solver = new EqualityLogicSolver();
        Set<EqualityConstraint> constraints = new HashSet<>();
        while ( true ) {
            String line = scanner.nextLine();
            if ( line.equals( "solve" ) ) {
                break;
            }

            EqualityConstraint c = parser.parse( line );
            constraints.add( c );
        }

        VariableAssignment assignment = solver.solve( constraints );
        if ( assignment == null ) {
            System.out.println( "UNSAT!" );
        } else {
            System.out.println( "SAT!" );
            System.out.println( assignment );
        }
    }

    @Override
    public VariableAssignment solve( Set<EqualityConstraint> constraints ) {
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
                    return null;
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
        return assignment;
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

        int aRank = ranks.get( a );
        int bRank = ranks.get( b );
        if ( aRank > bRank ) {
            pointers.put( b, a );
            ranks.put( a, aRank + bRank );
        } else {
            pointers.put( a, b );
            ranks.put( b, aRank + bRank );
        }
    }
}
