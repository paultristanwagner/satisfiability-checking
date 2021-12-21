package me.paultristanwagner.satchecking;

import java.util.*;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Assignment {
    
    private int decisionLevel;
    private final Stack<List<LiteralAssignment>> decisionLevels;
    private final Map<String, LiteralAssignment> literalAssignments;
    
    public Assignment() {
        decisionLevel = 0;
        decisionLevels = new Stack<>();
        decisionLevels.add( new ArrayList<>() );
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
        List<LiteralAssignment> dl = new ArrayList<>();
        LiteralAssignment la = new LiteralAssignment( literal.getName(), value, false, null );
        dl.add( la );
        decisionLevels.push( dl );
        decisionLevel++;
        literalAssignments.put( literal.getName(), la );
    }
    
    public void force( Literal literal, Clause antecedent ) {
        List<LiteralAssignment> dl = decisionLevels.peek();
        LiteralAssignment la = new LiteralAssignment( literal.getName(), !literal.isNegated(), true, antecedent );
        dl.add( la );
        literalAssignments.put( literal.getName(), la );
    }
    
    public boolean isEmpty() {
        return literalAssignments.isEmpty();
    }
    
    public LiteralAssignment getLastDecision() {
        return decisionLevels.peek().get( 0 );
    }
    
    public LiteralAssignment getLastAssignmentOnCurrentLevel( Clause clause ) {
        List<LiteralAssignment> assignmentsOnCurrentLevel = getAssignmentsOnCurrentLevel();
        for ( int i = assignmentsOnCurrentLevel.size() - 1; i >= 0; i-- ) {
            LiteralAssignment literalAssignment = assignmentsOnCurrentLevel.get( i );
            for ( Literal literal : clause.getLiterals() ) {
                if ( literal.getName().equals( literalAssignment.getLiteralName() ) ) {
                    return literalAssignment;
                }
            }
        }
        throw new RuntimeException( "This should not happen :)" );
    }
    
    public LiteralAssignment getLastAssigned() {
        List<LiteralAssignment> dl = decisionLevels.peek();
        return dl.get( dl.size() - 1 );
    }
    
    public void undoLastDecision() {
        List<LiteralAssignment> dl = decisionLevels.peek();
        while ( !dl.isEmpty() ) {
            LiteralAssignment la = dl.get( dl.size() - 1 );
            dl.remove( dl.size() - 1 );
            literalAssignments.remove( la.getLiteralName() );
        }
        decisionLevels.pop();
        decisionLevel--;
    }
    
    public void undoPropagations() {
        List<LiteralAssignment> dl = decisionLevels.peek();
        while ( dl.size() > 1 ) {
            LiteralAssignment la = dl.get( dl.size() - 1 );
            dl.remove( dl.size() - 1 );
            literalAssignments.remove( la.getLiteralName() );
        }
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
    
    public List<LiteralAssignment> getAssignmentsOnCurrentLevel() {
        return decisionLevels.peek();
    }
    
    @Override
    public String toString() {
        if ( isEmpty() ) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        List<LiteralAssignment> las = new ArrayList<>( literalAssignments.values() );
        las.sort( Comparator.comparing( LiteralAssignment::getLiteralName ) );
        
        boolean anyTrue = false;
        for ( LiteralAssignment la : las ) {
            if ( Config.load().reducedAssignments() ) {
                if ( la.getValue() ) {
                    anyTrue = true;
                    sb.append( ", " )
                            .append( la.getLiteralName() )
                            .append( "=1" );
                }
            } else {
                sb.append( ", " )
                        .append( la.getLiteralName() )
                        .append( "=" )
                        .append( la.getValue() ? "1" : "0" );
            }
        }
        
        if ( !Config.load().reducedAssignments() || anyTrue ) {
            return sb.substring( 2 );
        } else {
            return "-";
        }
    }
    
    public int getDecisionLevel() {
        return decisionLevel;
    }
}
