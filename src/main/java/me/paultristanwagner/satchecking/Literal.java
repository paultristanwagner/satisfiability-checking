package me.paultristanwagner.satchecking;

public class Literal {
    
    private final String name;
    private final boolean negated;
    
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
    
    @Override
    public String toString() {
        if ( negated ) {
            return "~" + name;
        } else {
            return name;
        }
    }
}
