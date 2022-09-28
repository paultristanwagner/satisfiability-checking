package me.paultristanwagner.satchecking.builder;

import me.paultristanwagner.satchecking.Clause;
import me.paultristanwagner.satchecking.Literal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FunctionCNFBuilder<X, Y> extends CNFBuilder {
    
    private final List<X> domain;
    private final List<Y> codomain;
    
    public FunctionCNFBuilder( List<X> domain, List<Y> codomain ) {
        this.domain = domain;
        this.codomain = codomain;
        
        // Every element in our domain is mapped
        for ( X x : domain ) {
            List<Literal> literals = new ArrayList<>();
            for ( Y y : codomain ) {
                literals.add( mapLiteral( x, y ) );
            }
            Clause clause = new Clause( literals );
            clauses.add( clause );
        }
        
        // Every element of the domain maps to at most one Element
        for ( X x : domain ) {
            for ( int i = 0; i < codomain.size() - 1; i++ ) {
                for ( int j = i + 1; j < codomain.size(); j++ ) {
                    Y y1 = codomain.get( i );
                    Y y2 = codomain.get( j );
                    Literal l1 = mapLiteral( x, y1 ).not();
                    Literal l2 = mapLiteral( x, y2 ).not();
                    Clause clause = new Clause( List.of( l1, l2 ) );
                    clauses.add( clause );
                }
            }
        }
    }
    
    public FunctionCNFBuilder<X, Y> injective() {
        // Every element in the codomain has at most one preimage
        for ( Y y : codomain ) {
            for ( int i = 0; i < domain.size() - 1; i++ ) {
                for ( int j = i + 1; j < domain.size(); j++ ) {
                    // not (a1 and b1)
                    X x1 = domain.get( i );
                    X x2 = domain.get( j );
                    Literal l1 = mapLiteral( x1, y ).not();
                    Literal l2 = mapLiteral( x2, y ).not();
                    Clause clause = new Clause( List.of( l1, l2 ) );
                    clauses.add( clause );
                }
            }
        }
        return this;
    }
    
    public FunctionCNFBuilder<X, Y> surjective() {
        // Every element in the codomain has at least one preimage
        for ( Y y : codomain ) {
            List<Literal> literals = new ArrayList<>();
            for ( X x : domain ) {
                literals.add( mapLiteral( x, y ) );
            }
            Clause clause = new Clause( literals );
            clauses.add( clause );
        }
        return this;
    }
    
    public FunctionCNFBuilder<X, Y> bijective() {
        injective();
        surjective();
        return this;
    }
    
    public FunctionCNFBuilder<X, Y> map( X x, Y y ) {
        Literal literal = mapLiteral( x, y );
        Clause clause = new Clause( List.of( literal ) );
        add( clause );
        return this;
    }
    
    public FunctionCNFBuilder<X, Y> dontMap( X x, Y y ) {
        Literal literal = mapLiteral( x, y ).not();
        Clause clause = new Clause( List.of( literal ) );
        add( clause );
        return this;
    }
    
    public FunctionCNFBuilder<X, Y> mapInto( X x, Set<Y> ys ) {
        List<Literal> literals = new ArrayList<>();
        for ( Y y : ys ) {
            literals.add( mapLiteral( x, y ) );
        }
        add( new Clause( literals ) );
        return this;
    }
    
    public FunctionCNFBuilder<X, Y> mapInto( Set<X> xs, Set<Y> ys ) {
        for ( X x : xs ) {
            mapInto( x, ys );
        }
        
        return this;
    }
    
    private Literal mapLiteral( X x, Y y ) {
        return new Literal( x.toString() + "_" + y.toString() );
    }
}
