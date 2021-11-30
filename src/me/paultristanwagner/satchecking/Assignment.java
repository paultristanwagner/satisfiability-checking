package me.paultristanwagner.satchecking;

import java.util.*;

public class Assignment {
    
    private final Stack<Character> decisions;
    private final Map<Character, Boolean> map;
    
    public Assignment() {
        decisions = new Stack<>();
        map = new HashMap<>();
    }
    
    public boolean fits( CNF cnf ) {
        List<Character> characters = cnf.getCharacters();
        for ( Character character : characters ) {
            if ( !assigns( character ) ) {
                return false;
            }
        }
        return true;
    }
    
    public boolean assigns( char c ) {
        return decisions.contains( c ) && map.containsKey( c );
    }
    
    public boolean get( char c ) {
        if ( !assigns( c ) ) {
            throw new IllegalStateException( "Assignment does not assign any value to '" + c + "'" );
        }
        
        return map.get( c );
    }
    
    public void assign( char c, boolean value ) {
        decisions.add( c );
        map.put( c, value );
    }
    
    public boolean backtrack() {
        while ( !decisions.isEmpty() ) {
            Character c = decisions.peek();
            boolean lastDecision = get( c );
            decisions.pop();
            if ( lastDecision ) { // todo: change this that it also works the other way around if wanted
                map.remove( c );
            } else {
                assign( c, true );
                return true;
            }
        }
        return false;
    }
    
    public boolean evaluate( Literal literal ) {
        return get( literal.getCharacter() ) ^ literal.isNegated();
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
        List<Character> characters = new ArrayList<>( decisions );
        Collections.sort( characters );
        
        for ( Character character : characters ) {
            if ( assigns( character ) ) {
                sb.append( ", " )
                        .append( character )
                        .append( "=" )
                        .append( get( character ) );
            }
        }
        return sb.substring( 2 );
    }
    
    public Character getLastAssigned() {
        if ( decisions.isEmpty() ) {
            throw new IllegalStateException( "Not last assigned literal" );
        }
        return decisions.peek();
    }
    
    public List<Character> getAssignedCharacters() {
        return new ArrayList<>( decisions );
    }
}
