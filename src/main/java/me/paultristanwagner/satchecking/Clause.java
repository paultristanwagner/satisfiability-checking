package me.paultristanwagner.satchecking;

import java.util.List;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Clause {

    private final List<Literal> literals;

    public Clause( List<Literal> literals ) {
        this.literals = literals;
    }

    public static Clause parse( String string ) {
        CNF cnf = CNF.parse( "(" + string + ")" );
        return cnf.getClauses().stream().findFirst().get();
    }

    public List<Literal> getLiterals() {
        return literals;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( Literal literal : literals ) {
            sb.append( " | " ).append( literal );
        }
        return sb.substring( 3 );
    }
}
