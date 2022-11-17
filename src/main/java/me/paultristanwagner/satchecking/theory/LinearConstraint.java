package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.Constraint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LinearConstraint implements Constraint {

    protected final Set<String> variables;
    protected final Map<String, Double> coefficients;
    private Bound bound;
    private double value;

    private LinearConstraint derivedFrom;

    public LinearConstraint() {
        this.variables = new HashSet<>();
        this.coefficients = new HashMap<>();
    }

    public LinearConstraint( LinearConstraint constraint ) {
        this.variables = new HashSet<>( constraint.variables );
        this.coefficients = new HashMap<>( constraint.coefficients );
        this.bound = constraint.bound;
        this.value = constraint.value;
        this.derivedFrom = constraint;
    }

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

    public void setBound( Bound bound ) {
        this.bound = bound;
    }

    public void setValue( double value ) {
        this.value = value;
    }

    public void setDerivedFrom( LinearConstraint derivedFrom ) {
        this.derivedFrom = derivedFrom;
    }

    public double getBoundOn( String variable ) {
        if ( variables.size() != 1 ) {
            throw new IllegalStateException( "Constraint does not have exactly one variable" );
        }

        if ( !variables.contains( variable ) ) {
            throw new IllegalArgumentException( "Variable is not in constraint" );
        }

        double coefficient = coefficients.get( variable );

        return value / coefficient;
    }

    public LinearConstraint getRoot() {
        if ( derivedFrom == null ) {
            return this;
        }
        return derivedFrom.getRoot();
    }

    public LinearConstraint offset( String variable, String substitute, double offset ) {
        LinearConstraint constraint = new LinearConstraint( this );
        if ( !coefficients.containsKey( variable ) ) {
            return this;
        }

        double coeff = coefficients.get( variable );
        constraint.variables.remove( variable );
        constraint.coefficients.remove( variable );
        constraint.setCoefficient( substitute, coeff );

        constraint.value = value - coeff * offset;

        return constraint;
    }

    public LinearConstraint positiveNegativeSubstitute( String variable, String positive, String negative ) {
        double coeff = coefficients.get( variable );

        LinearConstraint constraint = new LinearConstraint( this );
        constraint.variables.remove( variable );
        constraint.coefficients.remove( variable );
        constraint.setCoefficient( positive, coeff );
        constraint.setCoefficient( negative, -coeff );

        return constraint;
    }

    public enum Bound {
        LOWER,
        UPPER,
        EQUAL
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append( serializeTerm( coefficients ) );

        if ( bound == Bound.EQUAL ) {
            sb.append( "=" );
        } else if ( bound == Bound.LOWER ) {
            sb.append( ">=" );
        } else {
            sb.append( "<=" );
        }
        sb.append( value );

        return sb.toString();
    }

    public static class MaximizingConstraint extends LinearConstraint {

        @Override
        public String toString() {
            return "max(" + serializeTerm( coefficients ) + ")";
        }
    }

    public static class MinimizingConstraint extends LinearConstraint {

        @Override
        public String toString() {
            return "min(" + serializeTerm( coefficients ) + ")";
        }
    }

    private static String serializeTerm( Map<String, Double> coefficients ) {
        StringBuilder sb = new StringBuilder();
        coefficients.forEach( ( variable, coefficent ) -> {
            if ( coefficent >= 0 ) {
                if ( !sb.isEmpty() ) {
                    sb.append( "+" );
                }
            } else {
                sb.append( "-" );
            }

            double abs = Math.abs( coefficent );
            if ( abs != 1 ) {
                sb.append( abs );
            }

            sb.append( variable );
        } );

        return sb.toString();
    }
}
