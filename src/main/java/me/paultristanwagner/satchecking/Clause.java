package me.paultristanwagner.satchecking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Clause {
    
    private final List<Literal> literals;
    
    public Clause( Literal... literals ) {
        this.literals = Arrays.asList( literals );
    }
    
    public boolean isUnit( Assignment assignment ) {
        // todo
        return Boolean.parseBoolean( "TODO" );
    }
    
    public List<Character> getCharacters() {
        List<Character> characters = new ArrayList<>();
        for ( Literal literal : literals ) {
            characters.add( literal.getCharacter() );
        }
        return characters;
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
