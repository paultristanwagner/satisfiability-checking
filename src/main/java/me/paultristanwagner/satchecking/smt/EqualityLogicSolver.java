package me.paultristanwagner.satchecking.smt;

import me.paultristanwagner.satchecking.*;
import me.paultristanwagner.satchecking.builder.SudokuEqualityBuilder;
import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import me.paultristanwagner.satchecking.theory.EqualityLogic;
import me.paultristanwagner.satchecking.theory.EqualityLogic.EqualityLogicResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// todo: proof of concept
public class EqualityLogicSolver implements SMTSolver<EqualityConstraint> {

    public static void main( String[] args ) {
        int blockSize = 3;

        System.out.printf( "Trying to solve %dx%d sudoku puzzle %n", blockSize, blockSize );
        SudokuEqualityBuilder builder = new SudokuEqualityBuilder( blockSize );
        TheoryCNFParser<EqualityConstraint> parser = new TheoryCNFParser<>();
        TheoryCNF<EqualityConstraint> cnf = parser.parse( builder.build() ); // Ideally the builder would return a CNF directly

        EqualityLogicSolver solver = new EqualityLogicSolver();
        long start = System.currentTimeMillis();
        VariableAssignment assignment = solver.solve( cnf );
        long end = System.currentTimeMillis();

        if ( assignment == null ) {
            System.out.println( "No solution found." );
        } else {
            StringBuilder sb = new StringBuilder();
            for ( int i = 1; i <= 9; i++ ) {
                for ( int j = 1; j <= 9; j++ ) {
                    String variable = "a" + i + "_" + j;
                    double value = assignment.getAssignment( variable );
                    String constant = null;
                    for ( int k = 1; k <= 9; k++ ) {
                        String c = "c" + k;
                        if ( assignment.getAssignment( c ) == value ) {
                            constant = c;
                            break;
                        }
                    }

                    if ( constant == null ) {
                        throw new IllegalStateException();
                    }

                    sb.append( "a" ).append( i ).append( "_" ).append( j )
                            .append( "=" )
                            .append( constant )
                            .append( "; " );
                }
            }
            System.out.println( sb );
        }

        float seconds = ( end - start ) / 1000f;
        System.out.printf( "Solved in %.3f seconds. %n", seconds );
    }

    @Override
    public VariableAssignment solve( TheoryCNF<EqualityConstraint> cnf ) {
        Solver satSolver = new DPLLCDCLSolver();
        satSolver.load( cnf.getBooleanStructure() );

        PartialAssignment assignment;
        while ( ( assignment = ( (DPLLCDCLSolver) satSolver ).nextPartialAssignment() ) != null ) {
            List<Literal> trueLiterals = assignment.getTrueLiterals();

            EqualityLogic equalityLogic = new EqualityLogic();
            Set<EqualityConstraint> selectedConstraints = new HashSet<>();
            for ( Literal trueLiteral : trueLiterals ) {
                EqualityConstraint constraint = cnf.getConstraintLiteralMap().inverse().get( trueLiteral.getName() );

                selectedConstraints.add( constraint );
            }

            EqualityLogicResult equalityLogicResult = equalityLogic.solve( selectedConstraints );
            if ( equalityLogicResult.isSatisfiable() ) {
                if ( assignment.isComplete() ) {
                    return equalityLogicResult.getAssignment();
                }
            } else {
                Set<EqualityConstraint> explanation = equalityLogicResult.getExplanation();

                List<Literal> literals = new ArrayList<>();
                for ( EqualityConstraint equalityConstraint : explanation ) {
                    String literalName = cnf.getConstraintLiteralMap().get( equalityConstraint );
                    literals.add( new Literal( literalName ).not() );
                }

                Clause clause = new Clause( literals );
                ( (DPLLCDCLSolver) satSolver ).excludeClause( clause );

                // todo: We need to exclude the clause from the SAT solver
                // How can we do this? Conflict resolution?
                // We need to be careful to not exclude the remaining assignment from the SAT solver!
            }
        }

        return null;
    }
}
