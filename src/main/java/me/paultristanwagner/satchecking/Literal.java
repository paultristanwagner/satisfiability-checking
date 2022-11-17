package me.paultristanwagner.satchecking;

import java.util.Objects;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Literal {

    private final String name;
    private final boolean negated;

    public Literal( String name ) {
        this( name, false );
    }

    public Literal( String name, boolean negated ) {
        this.name = name;
        this.negated = negated;
    }

    public String getName() {
        return name;
    }

    public boolean isNegated() {
        return negated;
    }

    public Literal not() {
        return new Literal( name, !negated );
    }

    @Override
    public String toString() {
        if ( negated ) {
            return "~" + name;
        } else {
            return name;
        }
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;
        Literal literal = (Literal) o;
        return negated == literal.negated && Objects.equals( name, literal.name );
    }

    @Override
    public int hashCode() {
        return Objects.hash( name, negated );
    }
}
