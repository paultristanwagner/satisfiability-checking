package me.paultristanwagner.satchecking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CNF {
    
    private final List<Clause> clauses;
    
    public CNF(Clause... clauses) {
        this.clauses = Arrays.asList(clauses);
    }
    
    public List<Clause> getClauses() {
        return clauses;
    }
    
    public Set<Character> getCharacters() {
        Set<Character> characters = new HashSet<>();
        for ( Clause clause : clauses ) {
            characters.addAll( clause.getCharacters() );
        }
        return characters;
    }
}
