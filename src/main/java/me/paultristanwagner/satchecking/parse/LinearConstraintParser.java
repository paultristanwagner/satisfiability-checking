package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.LinearConstraint;

import java.util.concurrent.atomic.AtomicInteger;

public class LinearConstraintParser implements Parser<LinearConstraint> {
    
    /*
     *  Grammar for Linear constraints:
     *  S 			-> TERM "=" OPT_SIGN VAL
     *              -> TERM ">=" OPT_SIGN VAL
     *              -> TERM "<=" OPT_SIGN VAL
     *
     *  TERM 		-> OPT_SIGN OPT_VAL VAR
     *              -> OPT_SIGN OPT_VAL VAR SGN_TERM
     *
     *  SGN_TERM 	-> SIGN OPT_VAL VAR
     *              -> SIGN OPT_VAL VAR SGN_TERM
     *
     *  VAL 		-> NUMBER
     *              -> NUMBER "." NUMBER
     *
     *  OPT_VAL		-> VAL
     *              -> ""
     *
     *  SIGN 		-> "+" SIGN
     *              -> "-" SIGN
     *              -> "+"
     *              -> "-"
     *
     *  VAR			-> VAR_NAME
     *              -> VAR_NAME NUMBER
     *
     *  NUMBER 		-> "0" | ... | "9"
     *              -> "0" | ... | "9" NUMBER
     */
    @Override
    public LinearConstraint parse( String string, AtomicInteger index ) {
        LinearConstraint lc = new LinearConstraint();
    
        lc.setLabel( string );
        TERM( string, index, lc );
        COMPARISON( string, index, lc );
        int sign = OPT_SIGN( string, index );
        lc.setValue( sign * VAL( string, index ) );
    
        if ( index.get() != string.length() ) {
            throw new SyntaxError( "Unexpected character at index " + index.get(), string, index.get() );
        }
        
        return lc;
    }
    
    private static void TERM( String string, AtomicInteger index, LinearConstraint lc ) {
        double firstSign = OPT_SIGN( string, index );
        double firstCoefficient = firstSign * OPT_VAL( string, index );
        String firstVar = VAR( string, index );
        lc.setCoefficient( firstVar, firstCoefficient );
        
        int fallback = index.get();
        while ( index.get() < string.length() ) {
            try {
                int sign = SIGN( string, index );
                double coefficient = sign * OPT_VAL( string, index );
                String var = VAR( string, index );
                
                coefficient += lc.getCoefficients().getOrDefault( var, 0.0 );
                lc.setCoefficient( var, coefficient );
                
                fallback = index.get();
            } catch ( SyntaxError e ) {
                index.set( fallback );
                break;
            }
        }
    }
    
    private static void COMPARISON( String string, AtomicInteger index, LinearConstraint lc ) {
        char character0 = Parser.nextProperChar( string, index );
        if ( character0 == '=' ) {
            lc.setBound( LinearConstraint.Bound.EQUAL );
        } else if ( character0 == '>' ) {
            char character1 = Parser.nextProperChar( string, index );
            if ( character1 == '=' ) {
                lc.setBound( LinearConstraint.Bound.LOWER );
            } else {
                index.decrementAndGet();
                throw new SyntaxError( "Comparison >= expected at index " + index.get(), string, index.get() );
            }
        } else if ( character0 == '<' ) {
            char character1 = Parser.nextProperChar( string, index );
            if ( character1 == '=' ) {
                lc.setBound( LinearConstraint.Bound.UPPER );
            } else {
                index.decrementAndGet();
                throw new SyntaxError( "Comparison <= expected at index " + index.get(), string, index.get() );
            }
        } else {
            index.decrementAndGet();
            throw new SyntaxError( "Comparison (=, >=, <=) expected at index " + index.get(), string, index.get() );
        }
    }
    
    private static double OPT_VAL( String string, AtomicInteger index ) {
        int fallback = index.get();
        try {
            return VAL( string, index );
        } catch ( SyntaxError error ) {
            index.set( fallback );
            return 1;
        }
    }
    
    private static double VAL( String string, AtomicInteger index ) {
        String valString = NUMBER( string, index );
        char character = Parser.nextProperChar( string, index );
        if ( character == '.' ) {
            valString += "." + NUMBER( string, index );
        } else {
            index.decrementAndGet();
        }
        return Double.parseDouble( valString );
    }
    
    private static int OPT_SIGN( String string, AtomicInteger index ) {
        int fallback = index.get();
        int result = 1;
        try {
            result = SIGN( string, index );
        } catch ( SyntaxError e ) {
            index.set( fallback );
        }
        return result;
    }
    
    private static int SIGN( String string, AtomicInteger index ) {
        boolean read = false;
        int result = 1;
        
        while ( index.get() < string.length() ) {
            char character = Parser.nextProperChar( string, index );
            if ( character == '+' ) {
                read = true;
            } else if ( character == '-' ) {
                read = true;
                result *= -1;
            } else {
                index.decrementAndGet();
                break;
            }
        }
        
        if ( !read ) {
            throw new SyntaxError( "Sign expected at index " + index.get(), string, index.get() );
        }
        return result;
    }
    
    private static String VAR( String string, AtomicInteger index ) {
        String result = VAR_NAME( string, index );
        int fallback = index.get();
        try {
            result += NUMBER( string, index );
        } catch ( SyntaxError e ) {
            index.set( fallback );
        }
        return result;
    }
    
    private static String VAR_NAME( String string, AtomicInteger index ) {
        StringBuilder builder = new StringBuilder();
        while ( index.get() < string.length() ) {
            char character = Parser.nextProperChar( string, index );
            if ( ( character < 'a' || character > 'z' ) && ( character < 'A' || character > 'Z' ) ) {
                index.decrementAndGet();
                break;
            }
            
            builder.append( character );
        }
        
        if ( builder.isEmpty() ) {
            throw new SyntaxError( "Variable expected at index " + index.get(), string, index.get() );
        }
        
        return builder.toString();
    }
    
    private static String NUMBER( String string, AtomicInteger index ) {
        StringBuilder builder = new StringBuilder();
        while ( index.get() < string.length() ) {
            char character = Parser.nextProperChar( string, index );
            if ( character < '0' || character > '9' ) {
                index.decrementAndGet();
                break;
            }
            
            builder.append( character );
        }
        
        if ( builder.isEmpty() ) {
            throw new SyntaxError( "Number expected at index " + index.get(), string, index.get() );
        }
        return builder.toString();
    }
}
