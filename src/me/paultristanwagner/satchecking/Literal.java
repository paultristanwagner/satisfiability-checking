package me.paultristanwagner.satchecking;

public class Literal {
    
    private final char c;
    private final boolean negated;
    
    public Literal( char c, boolean negated ) {
        this.c = c;
        this.negated = negated;
    }
    
    public char getCharacter() {
        return c;
    }
    
    public boolean isNegated() {
        return negated;
    }
    
    public static Literal of( char c ) {
        return new Literal( c, false );
    }
    
    public static Literal not( char c ) {
        return new Literal( c, true );
    }
    
    @Override
    public String toString() {
        if ( negated ) {
            return "~" + c;
        } else {
            return String.valueOf( c );
        }
    }
}
