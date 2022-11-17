package me.paultristanwagner.satchecking.smt;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.paultristanwagner.satchecking.CNF;
import me.paultristanwagner.satchecking.Clause;
import me.paultristanwagner.satchecking.Literal;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class TheoryCNF<T extends Constraint> {

    private final List<TheoryClause<T>> initialClauses;
    private final List<TheoryClause<T>> clauses;
    private final List<T> constraints;

    private final BiMap<T, String> constraintNameMap;
    private final BiMap<T, Literal> constraintLiteralMap;

    private final CNF booleanStructure;

    public TheoryCNF( List<TheoryClause<T>> clauses ) {
        this.initialClauses = new ArrayList<>( clauses );
        this.clauses = clauses;
        this.constraints = new ArrayList<>();

        this.constraintNameMap = HashBiMap.create();
        this.constraintLiteralMap = HashBiMap.create();

        for ( TheoryClause<T> clause : clauses ) {
            for ( T constraint : clause.getConstraints() ) {
                if ( !constraints.contains( constraint ) ) {
                    constraints.add( constraint );

                    // todo: proof of concept
                    String name = constraintNameMap.getOrDefault( constraint, "c" + constraintNameMap.size() );
                    constraintNameMap.put( constraint, name );
                    constraintLiteralMap.put( constraint, new Literal( name ) );
                }
            }
        }

        // todo: proof of concept
        List<Clause> booleanClauses = new ArrayList<>();
        for ( TheoryClause<T> theoryClause : this.clauses ) {
            List<Literal> literals = new ArrayList<>();
            for ( T constraint : theoryClause.getConstraints() ) {
                literals.add( constraintLiteralMap.get( constraint ) );
            }
            booleanClauses.add( new Clause( literals ) );
        }
        this.booleanStructure = new CNF( booleanClauses );
    }

    public static <T extends Constraint> TheoryCNF<T> parse( String string ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( TheoryClause<T> clause : initialClauses ) {
            sb.append( " & (" ).append( clause ).append( ")" );
        }
        return sb.substring( 3 );
    }

    public BiMap<T, String> getConstraintLiteralMap() {
        return constraintNameMap;
    }

    public CNF getBooleanStructure() {
        return this.booleanStructure;
    }
}
