package me.paultristanwagner.satchecking.theory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LinearConstraint {
    
    private String label;
    private final Set<String> variables = new HashSet<>();
    private final Map<String, Double> coefficients = new HashMap<>();
    private Bound bound;
    private double value;
    
    public void setCoefficient( String variable, double coefficient ) {
        variables.add( variable );
        coefficients.put( variable, coefficient );
    }
    
    public Set<String> getVariables() {
        return variables;
    }
    
    public Map<String, Double> getCoefficients() {
        return coefficients;
    }
    
    public Bound getBound() {
        return bound;
    }
    
    public double getValue() {
        return value;
    }
    
    public void setLabel( String label ) {
        this.label = label;
    }
    
    public void setBound( Bound bound ) {
        this.bound = bound;
    }
    
    public void setValue( double value ) {
        this.value = value;
    }
    
    public enum Bound {
        
        LOWER,
        UPPER,
        EQUAL
    }
    
    @Override
    public String toString() {
        return label;
    }
}
