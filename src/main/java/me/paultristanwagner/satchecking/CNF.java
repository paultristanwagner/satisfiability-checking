package me.paultristanwagner.satchecking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class CNF {

    private final List<Clause> initialClauses;
    private final List<Clause> clauses;
    private final List<Literal> literals;

    public CNF( List<Clause> clauses ) {
        this.initialClauses = new ArrayList<>( clauses );
        this.clauses = clauses;
        this.literals = new ArrayList<>();

        for ( Clause clause : clauses ) {
            for ( Literal literal : clause.getLiterals() ) {
                if ( !literals.contains( literal ) ) {
                    literals.add( literal );
                }
            }
        }
    }

    public void learnClause( Clause clause ) {
        clauses.add( clause );
        
        for ( Literal literal : clause.getLiterals() ) {
            if ( !literals.contains( literal ) ) {
                literals.add( literal );
            }
        }
    }

    public List<Clause> getClauses() {
        return clauses;
    }

    public List<Literal> getLiterals() {
        return literals;
    }
    
    /*
        Grammar:
        S -> ( D ) & S
        S -> ( D )
        D -> L | D
        D -> L
        L -> ~<literal name>
        L -> <literal name>
     */

    public static CNF parse( String string ) {
        AtomicInteger index = new AtomicInteger( 0 );
        List<Clause> clauses = S( string, index );
        return new CNF( clauses );
    }

    private static List<Clause> S( String string, AtomicInteger index ) {
        if ( nextChar( string, index ) != '(' ) {
            throw new RuntimeException( "Cannot parse CNF. Expected '(' at index " + index );
        }
        index.incrementAndGet();

        List<Literal> literals = D( string, index );
        Clause clause = new Clause( literals );

        if ( nextChar( string, index ) != ')' ) {
            throw new RuntimeException( "Cannot parse CNF. Expected ')' at index " + index );
        }
        index.incrementAndGet();

        List<Clause> clauses = new ArrayList<>();
        clauses.add( clause );
        if ( string.length() == index.get() ) {
            return clauses;
        }

        if ( nextChar( string, index ) != '&' ) {
            throw new RuntimeException( "Expected '&' at index " + index );
        }
        index.incrementAndGet();

        clauses.addAll( S( string, index ) );
        return clauses;
    }

    public static List<Literal> D( String string, AtomicInteger index ) {
        Literal literal = L( string, index );
        List<Literal> literals = new ArrayList<>();
        literals.add( literal );
        if ( nextChar( string, index ) == '|' ) {
            index.incrementAndGet();
            literals.addAll( D( string, index ) );
        }
        return literals;
    }

    private static Literal L( String string, AtomicInteger index ) {
        boolean negated = false;
        if ( nextChar( string, index ) == '~' ) {
            index.incrementAndGet();
            negated = true;
        }
        StringBuilder sb = new StringBuilder();
        while ( true ) {
            char c = string.charAt( index.get() );
            if ( ( c < 'A' || c > 'Z' ) && ( c < 'a' || c > 'z' ) && ( c < '0' || c > '9' ) && c != '_' ) {
                if ( sb.length() == 0 ) {
                    throw new RuntimeException( "Literal expected, got '" + c + "' instead" );
                }
                break;
            }
            index.incrementAndGet();
            sb.append( c );
        }

        return new Literal( sb.toString(), negated );
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
        for ( Clause clause : initialClauses ) {
            sb.append( " & (" ).append( clause ).append( ")" );
        }
        return sb.substring( 3 );
    }
    
    public List<Clause> getInitialClauses() {
        return initialClauses;
    }
}
