package me.paultristanwagner.satchecking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CNF {
    
    private final List<Clause> clauses;
    
    public CNF( Clause... clauses ) {
        this.clauses = Arrays.asList( clauses );
    }
    
    public List<Clause> getClauses() {
        return clauses;
    }
    
    public Set<Character> getCharacters() {
        Set<Character> characters = new HashSet<>();
        for ( Clause clause : clauses ) {
            characters.addAll( clause.getCharacters() );
        }
        return characters;
    }
    
    /*
        Grammar:
        S -> ( D ) & S
        S -> ( D )
        D -> L | D
        D -> L
        L -> ~<char>
        L -> <char>
     */
    
    public static void main( String[] args ) {
        CNF cnf = CNF.parse( "(a | ~c | c) & (c)" );
        System.out.println( cnf );
    }
    
    public static CNF parse( String string ) {
        AtomicInteger index = new AtomicInteger( 0 );
        Set<Clause> clauses = S( string, index );
        return new CNF( clauses.toArray( new Clause[]{} ) );
    }
    
    private static Set<Clause> S( String string, AtomicInteger index ) {
        if ( nextChar( string, index ) != '(' ) {
            throw new RuntimeException( "Cannot parse CNF. Expected '(' at index " + index );
        }
        index.incrementAndGet();
        
        Set<Literal> literals = D( string, index );
        Clause clause = new Clause( literals.toArray( new Literal[]{} ) );
        
        if ( nextChar( string, index ) != ')' ) {
            throw new RuntimeException( "Cannot parse CNF. Expected ')' at index " + index );
        }
        index.incrementAndGet();
        
        if ( string.length() == index.get() ) { // todo: code is funky
            Set<Clause> clauses = new HashSet<>();
            clauses.add( clause );
            return clauses;
        }
        if ( nextChar( string, index ) != '&' ) {
            throw new RuntimeException( "Expected '&' at index " + index );
        }
        index.incrementAndGet();
        Set<Clause> clauses = S( string, index );
        clauses.add( clause );
        return clauses;
    }
    
    private static Set<Literal> D( String string, AtomicInteger index ) {
        Literal literal = L( string, index );
        Set<Literal> literals = new HashSet<>();
        if ( nextChar( string, index ) == '|' ) {
            index.incrementAndGet();
            literals.addAll( D( string, index ) );
        }
        literals.add( literal );
        return literals;
    }
    
    private static Literal L( String string, AtomicInteger index ) {
        boolean negated = false;
        if ( nextChar( string, index ) == '~' ) {
            index.incrementAndGet();
            negated = true;
        }
        char c = nextChar( string, index );
        index.incrementAndGet();
        if ( ( c < 65 || c > 90 ) && ( c < 97 || c > 122 ) ) {
            throw new IllegalStateException( "Literal expected instead of '" + c + "' at index " + index );
        }
        return new Literal( c, negated );
    }
    
    private static char nextChar( String string, AtomicInteger index ) {
        char c;
        while ( ( c = string.charAt( index.get() ) ) == ' ' ) {
            index.incrementAndGet();
        }
        return c;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( Clause clause : clauses ) {
            sb.append( " & (" ).append( clause ).append( ")" );
        }
        return sb.substring( 3 );
    }
}
