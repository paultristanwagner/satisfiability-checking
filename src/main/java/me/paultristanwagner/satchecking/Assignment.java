package me.paultristanwagner.satchecking;

import java.util.*;

public class Assignment {
    
    private final CNF cnf;
    private final Stack<LiteralAssignment> decisions;
    // todo: switch to map
    private final List<LiteralAssignment> literalAssignments;
    
    public Assignment( CNF cnf ) {
        this.cnf = cnf;
        
        // todo: use literal watcher
        /* literalWatcher = new ArrayList<>();
        for ( Clause clause : cnf.getClauses() ) {
            List<Literal> literals = clause.getLiterals();
            List<Literal> watched = new ArrayList<>();
            watched.add( literals.get( 0 ) );
            if ( literals.size() >= 2 ) {
                watched.add( literals.get( 1 ) );
            }
            literalWatcher.add( watched );
        } */
        
        decisions = new Stack<>();
        literalAssignments = new ArrayList<>();
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
        return literalAssignments.stream().anyMatch( la -> la.getLiteralName().equals( literal.getName() ) );
    }
    
    public boolean getValue( String literalName ) {
        Optional<LiteralAssignment> assignedLiteral = literalAssignments.stream().filter( la -> la.getLiteralName().equals( literalName ) ).findFirst();
        if ( assignedLiteral.isEmpty() ) {
            throw new IllegalStateException( "Assignment does not assign any value to '" + literalName + "'" );
        }
        return assignedLiteral.get().getValue();
    }
    
    public void assign( Literal literal, boolean value ) {
        LiteralAssignment la = new LiteralAssignment( literal.getName(), value, false );
        decisions.add( la );
        literalAssignments.add( la );
    }
    
    public void propagate( Literal literal ) {
        LiteralAssignment la = new LiteralAssignment( literal.getName(), !literal.isNegated(), true );
        decisions.add( la );
        literalAssignments.add( la );
    }
    
    public void decide( CNF cnf ) {
        List<Literal> literals = cnf.getLiterals();
        Optional<Literal> unassignedOptional = literals.stream().filter( lit -> !assigns( lit ) ).findAny();
        if ( unassignedOptional.isEmpty() ) {
            throw new IllegalStateException( "Cannot decide because all literals are assigned" );
        }
        Literal literal = unassignedOptional.get();
        this.assign( literal, false );
    }
    
    public boolean isEmpty() {
        return decisions.isEmpty();
    }
    
    public LiteralAssignment getLastDecision() {
        return decisions.peek();
    }
    
    public void undoLastDecision() {
        LiteralAssignment la = decisions.pop();
        literalAssignments.remove( la );
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
    
    public boolean couldSatisfy( Clause clause ) {
        for ( Literal literal : clause.getLiterals() ) {
            if ( !assigns( literal ) || evaluate( literal ) ) {
                return true;
            }
        }
        return false;
    }
    
    public boolean couldSatisfy( CNF cnf ) {
        for ( Clause clause : cnf.getClauses() ) {
            if ( !couldSatisfy( clause ) ) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<LiteralAssignment> las = new ArrayList<>( literalAssignments );
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
    
    public List<LiteralAssignment> getLiteralAssignments() {
        return literalAssignments;
    }
    
    public Clause not() {
        List<LiteralAssignment> assignments = new ArrayList<>( literalAssignments );
        assignments.sort( Comparator.comparing( LiteralAssignment::getLiteralName ) );
        Literal[] literals = new Literal[ assignments.size() ];
        for ( int i = 0; i < assignments.size(); i++ ) {
            LiteralAssignment la = assignments.get( i );
            literals[ i ] = new Literal( la.getLiteralName(), la.getValue() );
        }
        return new Clause( literals );
    }
}
