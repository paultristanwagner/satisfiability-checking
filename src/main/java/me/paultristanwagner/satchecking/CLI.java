package me.paultristanwagner.satchecking;

import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.smt.*;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import me.paultristanwagner.satchecking.theory.Simplex2;
import me.paultristanwagner.satchecking.theory.Simplex2.SimplexResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import static me.paultristanwagner.satchecking.AnsiColor.*;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class CLI {

    public static void main( String[] args ) throws IOException {
        System.out.println( "SAT-Solver version 1.0-SNAPSHOT © 2021 Paul T. Wagner" );
        System.out.println( "Type '?' for help." );

        Config config = Config.load();

        Scanner scanner = new Scanner( System.in );
        while ( true ) {
            System.out.print( "> " );
            String input;
            try {
                input = scanner.nextLine();
            } catch ( RuntimeException ignored ) {
                // Program was terminated
                return;
            }

            String cnfString;
            String[] split = input.split( " " );
            String command = split[ 0 ];

            if ( command.equals( "read" ) ) {
                if ( split.length != 2 ) {
                    System.out.println( RED + "Syntax: read <file>" + RESET );
                    System.out.println();
                    continue;
                }

                File file = new File( split[ 1 ] );
                if ( !file.exists() ) {
                    System.out.printf( "%sFile '%s' does not exists%s%n", RED, split[ 1 ], RESET );
                    System.out.println();
                    continue;
                }

                BufferedReader bufferedReader = new BufferedReader( new FileReader( file ) );
                StringBuilder builder = new StringBuilder();
                String line;
                while ( ( line = bufferedReader.readLine() ) != null ) {
                    builder.append( line );
                }

                input = builder.toString();
                split = input.split( " " );
                command = split[ 0 ];
            }

            if ( command.equalsIgnoreCase( "?" ) || command.equalsIgnoreCase( "help" ) ) {
                System.out.println( "   ? - View this help page" );
                System.out.println( "   reloadConfig - Reloads the configuration file" );
                System.out.println( "   read <file> - Reads a CNF from the specified file" );
                System.out.println( "   simplex <constraints> ... - Applies the simplex algorithm to the specified constraints" );
                System.out.println( "   smt <cnf of theory constraints> - Solves an SMT problem" );
                System.out.println();
                continue;
            } else if ( command.equals( "reloadConfig" ) ) {
                System.out.println( GREEN + "Reloading config..." + RESET );
                config = Config.reload();
                System.out.println( GREEN + "Done." + RESET );
                System.out.println();
                continue;
            } else if ( command.equals( "simplex" ) ) {
                if ( split.length == 1 ) {
                    System.out.println( RED + "Syntax: simplex <constraints> ..." + RESET );
                    continue;
                }

                Simplex2 simplex = new Simplex2();
                LinearConstraintParser parser = new LinearConstraintParser();
                boolean syntaxError = false;
                for ( int i = 1; i < split.length; i++ ) {
                    try {
                        LinearConstraint lc = parser.parse( split[ i ] );

                        if ( lc instanceof MaximizingConstraint ) {
                            simplex.maximize( lc );
                        } else if ( lc instanceof MinimizingConstraint ) {
                            simplex.minimize( lc );
                        } else {
                            simplex.addConstraint( lc );
                        }
                    } catch ( SyntaxError e ) {
                        System.out.println( RED + "Syntax error: " + e.getMessage() );
                        System.out.println( split[ i ] );
                        Parser.printPointer( e.getIndex() );
                        System.out.print( RESET );

                        syntaxError = true;
                        break;
                    }
                }

                if ( syntaxError ) {
                    continue;
                }

                SimplexResult result = simplex.solve();
                if ( result.isUnbounded() ) {
                    System.out.println( RED + "UNSAT! " + GRAY + "(" + RED + "feasible, but unbounded" + GRAY + ")" );
                    System.out.print( GREEN + "Solution: " );
                    System.out.println( result );
                } else if ( result.isOptimal() ) {
                    System.out.println();
                    System.out.println( GREEN + "SAT! " + GRAY + "(" + GREEN + "optimal" + GRAY + ")" );
                    System.out.print( GREEN + "Solution: " );
                    System.out.print( result );
                    System.out.println( GREEN + "Optimum: " + result.getOptimum() );
                } else if ( result.isFeasible() ) {
                    System.out.println();
                    System.out.println( GREEN + "SAT!" );
                    System.out.print( GREEN + "Solution: " );
                    System.out.print( result );
                } else {
                    System.out.println();
                    System.out.println( RED + "UNSAT!" );
                    System.out.print( "Explanation: " );
                    System.out.print( result );
                }

                System.out.println( RESET );
                continue;
            } else if ( command.equals( "smt" ) ) {
                cnfString = input.substring( 4 );

                TheoryCNFParser<LinearConstraint> parser = new TheoryCNFParser<>();
                TheoryCNF<LinearConstraint> theoryCNF = parser.parse( cnfString );

                SMTSolver<LinearConstraint> smtSolver = new LinearRealArithmeticSolver();
                VariableAssignment variableAssignment = smtSolver.solve( theoryCNF );

                if ( variableAssignment != null ) {
                    System.out.println( GREEN + "SAT:" );
                    System.out.println( "" + variableAssignment );
                } else {
                    System.out.println( RED + "UNSAT" );
                }
                System.out.println( RESET );

                continue;
            } else if ( command.equals( "smteq" ) ) {
                cnfString = input.substring( 6 );

                TheoryCNFParser<EqualityConstraint> parser = new TheoryCNFParser<>();
                TheoryCNF<EqualityConstraint> theoryCNF = parser.parse( cnfString );

                SMTSolver<EqualityConstraint> smtSolver = new EqualityLogicSolver();
                VariableAssignment variableAssignment = smtSolver.solve( theoryCNF );

                if ( variableAssignment != null ) {
                    System.out.println( GREEN + "SAT:" );
                    System.out.println( "" + variableAssignment );
                } else {
                    System.out.println( RED + "UNSAT" );
                }
                System.out.println( RESET );

                continue;
            } else {
                cnfString = input;
            }

            CNF cnf;
            try {
                cnf = CNF.parse( cnfString );
            } catch ( RuntimeException e ) {
                System.out.println( RED + "Syntax Error: " + e.getMessage() + RESET );
                System.out.println();
                continue;
            }

            Solver solver = config.getSolver();
            long beforeMs = System.currentTimeMillis();
            solver.load( cnf );
            Assignment model = solver.nextModel();
            if ( model == null ) {
                System.out.println( RED + "UNSAT" + RESET );
                System.out.println();
                continue;
            }

            long modelCount = 0;
            System.out.println( GREEN + "SAT:" );
            while ( model != null && modelCount < config.getMaxModelCount() ) {
                modelCount++;

                if ( config.printModels() ) {
                    System.out.println( "" + GREEN + model + ";" + RESET );
                }

                model = solver.nextModel();
            }
            long timeMs = System.currentTimeMillis() - beforeMs;

            System.out.println( "" + GREEN + modelCount + " model/s found in " + timeMs + " ms" + RESET );
            if ( modelCount == config.getMaxModelCount() ) {
                System.out.println( GRAY + "(List of models could be incomplete since maximum number of models is restricted)" + RESET );
            }
            System.out.println();
        }
    }
}
