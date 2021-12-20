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
        System.out.println( "SAT-Solver version 1.0-SNAPSHOT \u24B8 2021 Paul T. Wagner" );
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
            String command = split[0];
            if ( command.equalsIgnoreCase( "?" ) || command.equalsIgnoreCase( "help" ) ) {
                System.out.println( "? - View this help page" );
                System.out.println( "reloadConfig - Reloads the configuration file" );
                System.out.println( "read <file> - Reads a CNF from the specified file" );
                System.out.println();
                continue;
            } else if ( command.equals( "reloadConfig" ) ) {
                System.out.println( AnsiColor.GREEN + "Reloading config..." + AnsiColor.RESET );
                config = Config.reload();
                System.out.println( AnsiColor.GREEN + "Done." + AnsiColor.RESET );
                System.out.println();
                continue;
            } else if ( command.equals( "read" ) ) {
                File file = new File( split[1] );
                if ( !file.exists() ) {
                    System.out.printf( "%sFile '%s' does not exists%s%n", AnsiColor.RED, split[1], AnsiColor.RESET );
                    System.out.println();
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
                System.out.println( AnsiColor.RED + "Syntax Error: " + e.getMessage() + AnsiColor.RESET );
                System.out.println();
                continue;
            }

            Solver solver = config.getSolver();
            long beforeMs = System.currentTimeMillis();
            solver.load( cnf );
            Assignment model = solver.nextModel();
            if ( model == null ) {
                System.out.println( AnsiColor.RED + "UNSAT" + AnsiColor.RESET );
                System.out.println();
                continue;
            }

            long modelCount = 0;
            System.out.println( AnsiColor.GREEN + "SAT:" );
            while ( model != null && modelCount < config.getMaxModelCount() ) {
                modelCount++;

                if ( config.printModels() ) {
                    System.out.println( "" + AnsiColor.GREEN + model + ";" + AnsiColor.RESET );
                }

                model = solver.nextModel();
            }
            long timeMs = System.currentTimeMillis() - beforeMs;

            System.out.println( "" + AnsiColor.GREEN + modelCount + " model/s found in " + timeMs + " ms" + AnsiColor.RESET );
            if ( modelCount == config.getMaxModelCount() ) {
                System.out.println( AnsiColor.GRAY + "(List of models could be incomplete since maximum number of models is restricted)" + AnsiColor.RESET );
            }
            System.out.println();
        }
    }
}
