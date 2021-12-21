package me.paultristanwagner.satchecking;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class LiteralAssignment {
    
    private final String literalName;
    private boolean value;
    private boolean previouslyAssigned;
    private Clause antecedent;
    
    public LiteralAssignment( String literalName, boolean value, boolean previouslyAssigned, Clause antecedent ) {
        this.literalName = literalName;
        this.value = value;
        this.previouslyAssigned = previouslyAssigned;
        this.antecedent = antecedent;
    }
    
    public String getLiteralName() {
        return literalName;
    }
    
    public boolean getValue() {
        return value;
    }
    
    public boolean wasPreviouslyAssigned() {
        return previouslyAssigned;
    }
    
    public boolean toggleValue() {
        this.value = !value;
        return value;
    }
    
    public void setPreviouslyAssigned() {
        this.previouslyAssigned = true;
    }
    
    public void setAntecedent( Clause antecedent ) {
        this.antecedent = antecedent;
    }
    
    public Clause getAntecedent() {
        return antecedent;
    }
}
