package me.paultristanwagner.satchecking;

import java.util.Arrays;
import java.util.List;

public class Clause {
    
    private final List<Literal> literals;
    
    public Clause( Literal... literals ) {
        this.literals = Arrays.asList( literals );
    }
    
    public boolean isUnit( Assignment assignment ) {
        // todo
        throw new UnsupportedOperationException();
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
