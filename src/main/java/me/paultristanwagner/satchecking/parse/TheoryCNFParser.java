package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.TheoryClause;
import me.paultristanwagner.satchecking.theory.LinearConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TheoryCNFParser implements Parser<TheoryCNF<LinearConstraint>> {

    public static void main( String[] args ) {
        TheoryCNFParser parser = new TheoryCNFParser();
        TheoryCNF<LinearConstraint> cnf = parser.parse( "([x>=0]) & ([x<=10])" );
        System.out.println( cnf );
    }

    @Override
    public TheoryCNF<LinearConstraint> parse( String string, AtomicInteger index ) {
        List<TheoryClause<LinearConstraint>> clauses = S( string, index );
        return new TheoryCNF<>( clauses );
    }
    
    /*
        Grammar:
        S -> ( D ) & S
        S -> ( D )
        D -> L | D
        D -> L
        L -> [Constraint]
     */

    private static List<TheoryClause<LinearConstraint>> S( String string, AtomicInteger index ) {
        if ( nextChar( string, index ) != '(' ) {
            throw new RuntimeException( "Cannot parse CNF. Expected '(' at index " + index );
        }
        index.incrementAndGet();

        List<LinearConstraint> literals = D( string, index );
        TheoryClause<LinearConstraint> clause = new TheoryClause<>( literals );

        if ( nextChar( string, index ) != ')' ) {
            throw new RuntimeException( "Cannot parse CNF. Expected ')' at index " + index );
        }
        index.incrementAndGet();

        List<TheoryClause<LinearConstraint>> clauses = new ArrayList<>();
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

    public static List<LinearConstraint> D( String string, AtomicInteger index ) {
        LinearConstraint constraint = L( string, index );
        List<LinearConstraint> constraints = new ArrayList<>();
        constraints.add( constraint );
        if ( nextChar( string, index ) == '|' ) {
            index.incrementAndGet();
            constraints.addAll( D( string, index ) );
        }
        return constraints;
    }

    private static LinearConstraint L( String string, AtomicInteger index ) {
        if ( nextChar( string, index ) != '[' ) {
            throw new RuntimeException( "Expected '[' at index " + index );
        }
        int closingIndex = string.indexOf( ']', index.get() );
        if ( closingIndex == -1 ) {
            throw new RuntimeException( "No closing ']' found after index" + index.get() );
        }
        index.incrementAndGet();
        String subString = string.substring( index.get(), closingIndex );

        LinearConstraintParser linearConstraintParser = new LinearConstraintParser();
        LinearConstraint constraint = linearConstraintParser.parse( subString );

        index.addAndGet( subString.length() );

        if ( nextChar( string, index ) != ']' ) {
            throw new RuntimeException( "Expected ']' at index " + index );
        }
        index.incrementAndGet();

        return constraint;
    }

    private static char nextChar( String string, AtomicInteger index ) {
        char c;
        while ( ( c = string.charAt( index.get() ) ) == ' ' ) {
            index.incrementAndGet();
        }
        return c;
    }
}
