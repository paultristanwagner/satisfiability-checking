package me.paultristanwagner.satchecking;

public class DPLLResult {
    
    private final boolean satisfiable;
    
    private final Assignment assignment;
    
    private DPLLResult( boolean satisfiable, Assignment assignment ) {
        this.satisfiable = satisfiable;
        this.assignment = assignment;
    }
    
    public static final DPLLResult UNSAT = new DPLLResult( false, null );
    
    public static DPLLResult SAT( Assignment assignment ) {
        return new DPLLResult( true, assignment );
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
