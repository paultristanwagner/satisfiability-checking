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
            
            DPLLResult result = DPLL.enumeration( cnf );
            if ( result.isSatisfiable() ) {
                System.out.println( "" + AnsiColor.GREEN + result + AnsiColor.RESET );
            } else {
                System.out.println( "" + AnsiColor.RED + result + AnsiColor.RESET );
            }
        }
    }
}
