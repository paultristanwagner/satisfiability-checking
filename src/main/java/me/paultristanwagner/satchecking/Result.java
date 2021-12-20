package me.paultristanwagner.satchecking;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Result {
    
    private final boolean satisfiable;
    
    private final Assignment assignment;
    
    private Result( boolean satisfiable, Assignment assignment ) {
        this.satisfiable = satisfiable;
        this.assignment = assignment;
    }
    
    public static final Result UNSAT = new Result( false, null );
    
    public static Result SAT( Assignment assignment ) {
        return new Result( true, assignment );
    }
    
    public boolean isSatisfiable() {
        return satisfiable;
    }
    
    public Assignment getAssignment() {
        if(!satisfiable) {
            throw new IllegalStateException("No assignment exists because the result is unsatisfiable");
        }
        return assignment;
    }
    
    @Override
    public String toString() {
        if ( satisfiable ) {
            return "SAT: " + assignment;
        } else {
            return "UNSAT";
        }
    }
}
