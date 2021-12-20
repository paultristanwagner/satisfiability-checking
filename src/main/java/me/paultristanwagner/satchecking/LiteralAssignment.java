package me.paultristanwagner.satchecking;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class LiteralAssignment {
    
    private final String literalName;
    private boolean value;
    private boolean previouslyAssigned;
    
    public LiteralAssignment( String literalName, boolean value, boolean previouslyAssigned ) {
        this.literalName = literalName;
        this.value = value;
        this.previouslyAssigned = previouslyAssigned;
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
}
