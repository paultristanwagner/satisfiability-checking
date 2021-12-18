package me.paultristanwagner.satchecking;

import java.util.ArrayList;
import java.util.List;

public class CNFBuilder<X, Y> {
    
    public static void main( String[] args ) {
        List<Character> domain = List.of('a', 'b', 'c', 'd', 'e');
        List<Character> codomain = List.of('1', '2', '3', '4', '5');
        CNF cnf = CNFBuilder.function( domain, codomain )
                .build();
        System.out.println( cnf );
    }
    
    private final List<X> domain;
    private final List<Y> codomain;
    private final List<Clause> clauses;
    
    private CNFBuilder( List<X> domain, List<Y> codomain ) {
        this.clauses = new ArrayList<>();
        this.domain = domain;
        this.codomain = codomain;
    }
    
    public static <X, Y> CNFBuilder<X, Y> function( List<X> domain, List<Y> codomain ) {
        CNFBuilder<X, Y> builder = new CNFBuilder<>( domain, codomain );
        
        // Every element in our domain is mapped
        for ( X x : domain ) {
            List<Literal> literals = new ArrayList<>();
            for ( Y y : codomain ) {
                literals.add( new Literal( x.toString() + y.toString(), false ) );
            }
            Clause clause = new Clause( literals.toArray( Literal[]::new ) );
            builder.clauses.add( clause );
        }
        // not (x1 and x2)
        
        // Every elements of the domain maps to at most one Element
        for ( X x : domain ) {
            for ( int i = 0; i < codomain.size() - 1; i++ ) {
                for ( int j = i + 1; j < codomain.size(); j++ ) {
                    Y y1 = codomain.get( i );
                    Y y2 = codomain.get( j );
                    Literal l1 = new Literal( x.toString() + y1.toString(), true );
                    Literal l2 = new Literal( x.toString() + y2.toString(), true );
                    Clause clause = new Clause( l1, l2 );
                    builder.clauses.add( clause );
                }
            }
        }
        
        return builder;
    }
    
    public CNFBuilder<X, Y> injective() {
        // Every element in the codomain has at most one preimage
        for ( Y y : codomain ) {
            for ( int i = 0; i < domain.size() - 1; i++ ) {
                for ( int j = i + 1; j < domain.size(); j++ ) {
                    // not (a1 and b1)
                    X x1 = domain.get( i );
                    X x2 = domain.get( j );
                    Literal l1 = new Literal( x1.toString() + y.toString(), true );
                    Literal l2 = new Literal( x2 + y.toString(), true );
                    Clause clause = new Clause( l1, l2 );
                    clauses.add( clause );
                }
            }
        }
        return this;
    }
    
    public CNFBuilder<X, Y> surjective() {
        // Every elements in the codomain has at least one preimage
        for ( Y y : codomain ) {
            List<Literal> literals = new ArrayList<>();
            for ( X x : domain ) {
                literals.add( new Literal( x.toString() + y.toString(), false ) );
            }
            Clause clause = new Clause( literals.toArray( Literal[]::new ) );
            clauses.add( clause );
        }
        return this;
    }
    
    public CNFBuilder bijective() {
        injective();
        surjective();
        return this;
    }
    
    public CNFBuilder add( Clause clause ) {
        clauses.add( clause );
        return this;
    }
    
    public CNF build() {
        return new CNF( clauses.toArray( Clause[]::new ) );
    }
}
