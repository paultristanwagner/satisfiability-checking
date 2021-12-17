package me.paultristanwagner.satchecking;

import java.util.Scanner;

public class CLI {
    
    public static void main( String[] args ) {
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
            
            CNF cnf;
            try {
                cnf = CNF.parse( input );
            } catch ( RuntimeException e ) {
                System.out.println( AnsiColor.RED + " Error: " + e.getMessage() + AnsiColor.RESET );
                continue;
            }
            
            DPLLSolver solver = new DPLLSolver();
            solver.load( cnf );
            Assignment model = solver.nextModel();
            if ( model == null ) {
                System.out.println( AnsiColor.RED + "UNSAT" + AnsiColor.RESET );
                continue;
            }
    
            System.out.println(AnsiColor.GREEN + "SAT:");
            while ( model != null ) {
                System.out.println( "" + AnsiColor.GREEN + model + AnsiColor.RESET );
                model = solver.nextModel();
            }
        }
    }
}
