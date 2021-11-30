package me.paultristanwagner.satchecking;

import java.util.Scanner;

public class CLI {
    
    public static void main( String[] args ) {
        Scanner scanner = new Scanner( System.in );
        while ( true ) {
            System.out.print( "> " );
            String input = scanner.nextLine();
            CNF cnf;
            try {
                cnf = CNF.parse( input );
            } catch ( RuntimeException e ) {
                System.out.println( "\u001b[31m Error: " + e.getMessage() + "\u001b[0m");
                continue;
            }
            
            DPLLResult result = DPLL.enumeration( cnf );
            System.out.println( "CNF: " + cnf );
            System.out.println( result );
        }
    }
}
