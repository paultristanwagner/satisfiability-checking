package me.paultristanwagner.satchecking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class CLI {

    public static void main( String[] args ) throws IOException {
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
            String command = split[0];
            if ( command.equals( "read" ) ) {
                File file = new File( split[1] );
                if ( !file.exists() ) {
                    System.out.printf( "%sFile '%s' does not exists%s%n", AnsiColor.RED, split[1], AnsiColor.RESET );
                    continue;
                }

                BufferedReader bufferedReader = new BufferedReader( new FileReader( file ) );
                StringBuilder builder = new StringBuilder();
                String line;
                while ( ( line = bufferedReader.readLine() ) != null ) {
                    builder.append( line );
                }

                cnfString = builder.toString();
            } else {
                cnfString = input;
            }

            CNF cnf;
            try {
                cnf = CNF.parse( cnfString );
            } catch ( RuntimeException e ) {
                System.out.println( AnsiColor.RED + " Error: " + e.getMessage() + AnsiColor.RESET );
                continue;
            }

            DPLLSolver solver = new DPLLSolver();
            long beforeMs = System.currentTimeMillis();
            solver.load( cnf );
            Assignment model = solver.nextModel();
            if ( model == null ) {
                System.out.println( AnsiColor.RED + "UNSAT" + AnsiColor.RESET );
                continue;
            }

            int modelcount = 0;
            System.out.println( AnsiColor.GREEN + "SAT:" );
            while ( model != null ) {
                modelcount++;
                System.out.println( "" + AnsiColor.GREEN + model + ";" + AnsiColor.RESET );
                model = solver.nextModel();
            }
            long timeMs = System.currentTimeMillis() - beforeMs;
            System.out.println( "" + AnsiColor.GREEN + modelcount + " models found in " + timeMs + " ms" + AnsiColor.RESET );
        }
    }
}
