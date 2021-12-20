package me.paultristanwagner.satchecking;

import java.util.*;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Assignment {

    private final Stack<LiteralAssignment> decisions;
    private final Map<String, LiteralAssignment> literalAssignments;

    public Assignment() {
        decisions = new Stack<>();
        literalAssignments = new HashMap<>();
    }

    public boolean fits( CNF cnf ) {
        List<Literal> literals = cnf.getLiterals();
        for ( Literal literal : literals ) {
            if ( !assigns( literal ) ) {
                return false;
            }
        }
        return true;
    }

    public boolean assigns( Literal literal ) {
        return literalAssignments.containsKey( literal.getName() );
    }

    public boolean getValue( String literalName ) {
        return literalAssignments.get( literalName ).getValue();
    }

    public void assign( Literal literal, boolean value ) {
        LiteralAssignment la = new LiteralAssignment( literal.getName(), value, false );
        decisions.push( la );
        literalAssignments.put( literal.getName(), la );
    }

    public void force( Literal literal ) {
        LiteralAssignment la = new LiteralAssignment( literal.getName(), !literal.isNegated(), true );
        decisions.push( la );
        literalAssignments.put( literal.getName(), la );
    }

    public boolean isEmpty() {
        return decisions.isEmpty();
    }

    public LiteralAssignment getLastDecision() {
        return decisions.peek();
    }

    public void undoLastDecision() {
        LiteralAssignment la = decisions.pop();
        literalAssignments.remove( la.getLiteralName() );
    }

    public boolean evaluate( Literal literal ) {
        return getValue( literal.getName() ) ^ literal.isNegated();
    }

    public boolean evaluate( Clause clause ) {
        for ( Literal literal : clause.getLiterals() ) {
            if ( evaluate( literal ) ) {
                return true;
            }
        }
        return false;
    }

    public boolean evaluate( CNF cnf ) {
        for ( Clause clause : cnf.getClauses() ) {
            if ( !evaluate( clause ) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<LiteralAssignment> las = new ArrayList<>( decisions );
        las.sort( Comparator.comparing( LiteralAssignment::getLiteralName ) );

        boolean anyTrue = false;
        for ( LiteralAssignment la : las ) {
            if ( la.getValue() ) {
                anyTrue = true;
                sb.append( ", " )
                        .append( la.getLiteralName() )
                        .append( "=" )
                        .append( la.getValue() ? "1" : "0" );
            }
        }

        if ( anyTrue ) {
            return sb.substring( 2 );
        } else {
            return "-";
        }
    }

    public Clause not() {
        List<Literal> literals = new ArrayList<>();
        for ( LiteralAssignment decision : decisions ) {
            literals.add( new Literal( decision.getLiteralName(), decision.getValue() ) );
        }
        return new Clause( literals );
    }
}
