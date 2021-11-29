package me.paultristanwagner.satchecking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Clause {
    
    private final List<Literal> literals;
    
    public Clause(Literal... literals) {
        this.literals = Arrays.asList(literals);
    }
    
    public boolean isUnit(Assignment assignment) {
        return Boolean.parseBoolean( "TODO" );
    }
    
    public Set<Character> getCharacters() {
        Set<Character> characters = new HashSet<>();
        for ( Literal literal : literals ) {
            characters.add( literal.getCharacter() );
        }
        return characters;
    }
    
    public List<Literal> getLiterals() {
        return literals;
    }
}
