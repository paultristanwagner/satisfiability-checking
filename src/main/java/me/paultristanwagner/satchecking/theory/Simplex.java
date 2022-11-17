package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.VariableAssignment;

import java.util.*;

public class Simplex {

    private int rows;
    private int columns;
    private double[][] tableau;

    private List<String> basicVariables;
    private List<String> nonBasicVariables;
    private List<Double> values;

    private Map<String, Double> lowerBounds;
    private Map<String, Double> upperBounds;

    private final List<LinearConstraint> constraints = new ArrayList<>();

    // todo: return something meaningful here, maybe a variableassignment or a simplex solution class
    public SimplexResult solve() {
        this.lowerBounds = new HashMap<>();
        this.upperBounds = new HashMap<>();

        // Collect variables
        Set<String> variableSet = new HashSet<>();
        for ( LinearConstraint constraint : constraints ) {
            for ( String variable : constraint.getVariables() ) {
                variableSet.add( "" + variable );
            }
        }
        List<String> variables = new ArrayList<>( variableSet );
        variables.sort( String::compareTo );

        // Initialize non-basic variables
        nonBasicVariables = new ArrayList<>( variables );
        values = new ArrayList<>();
        for ( int i = 0; i < variables.size(); i++ ) {
            values.add( 0.0 );
        }

        // Initialize slack variables, tableau and constraints
        basicVariables = new ArrayList<>();
        tableau = new double[constraints.size()][variableSet.size()];
        for ( int i = 0; i < constraints.size(); i++ ) {
            String slackName = "s" + i;
            LinearConstraint constraint = constraints.get( i );
            basicVariables.add( slackName );

            for ( int j = 0; j < variableSet.size(); j++ ) {
                String variable = variables.get( j );
                tableau[i][j] = constraint.getCoefficients().getOrDefault( variable, 0.0 );
            }

            if ( constraint.getBound() == LinearConstraint.Bound.EQUAL ) {
                lowerBounds.put( slackName, constraint.getValue() );
                upperBounds.put( slackName, constraint.getValue() );
            } else if ( constraint.getBound() == LinearConstraint.Bound.UPPER ) {
                upperBounds.put( slackName, constraint.getValue() );
            } else if ( constraint.getBound() == LinearConstraint.Bound.LOWER ) {
                lowerBounds.put( slackName, constraint.getValue() );
            }
        }

        this.rows = tableau.length;
        this.columns = tableau[0].length;

        while ( true ) {
            // todo: printTableau();

            Violation violation = getViolation();
            if ( violation == null ) {
                break;
            }

            String pivotVariable = getPivotVariable( violation.variable(), violation.increase() );
            if ( pivotVariable == null ) {
                Set<LinearConstraint> explanation = calculateExplanation( violation );
                return SimplexResult.infeasible( explanation );
            }
            pivot( violation.variable(), pivotVariable, violation.increase() );
        }

        VariableAssignment variableAssignment = new VariableAssignment();

        for ( String variable : variables ) {
            double value;
            if ( basicVariables.contains( variable ) ) {
                value = getBasicValue( basicVariables.indexOf( variable ) );
            } else {
                value = values.get( nonBasicVariables.indexOf( variable ) );
            }
            variableAssignment.assign( variable, value );
        }
        return SimplexResult.feasible( variableAssignment );
    }

    public void addConstraint( LinearConstraint constraint ) {
        constraints.add( constraint );
    }

    private Set<LinearConstraint> calculateExplanation( Violation violation ) {
        Set<LinearConstraint> explanation = new HashSet<>();

        int constraintIndex = Integer.parseInt( violation.variable().split( "s" )[1] );
        LinearConstraint constraint = constraints.get( constraintIndex );

        explanation.add( constraint );

        int basicIndex = basicVariables.indexOf( violation.variable() );
        for ( int j = 0; j < columns; j++ ) {
            double a = tableau[basicIndex][j];
            if ( a != 0 ) {
                String variable = nonBasicVariables.get( j );
                constraintIndex = Integer.parseInt( variable.split( "s" )[1] );
                constraint = constraints.get( constraintIndex );

                explanation.add( constraint );
            }
        }

        return explanation;
    }

    private Violation getViolation() {
        Violation result = null;
        for ( int i = 0; i < rows; i++ ) {
            String variable = basicVariables.get( i );
            double value = getBasicValue( i );
            if ( result != null && result.variable().compareTo( variable ) < 0 ) {
                continue;
            }

            if ( upperBounds.containsKey( variable ) ) {
                double u = upperBounds.get( variable );
                if ( value > u ) {
                    // todo: System.out.println( variable + " = " + value + " but " + variable + " <= " + u );
                    result = new Violation( variable, false );
                }
            }

            if ( lowerBounds.containsKey( variable ) ) {
                double l = lowerBounds.get( variable );
                if ( value < l ) {
                    // todo: System.out.println( variable + " = " + value + " but " + variable + " >= " + l );
                    result = new Violation( variable, true );
                }
            }
        }
        return result;
    }

    private String getPivotVariable( String violatingVariable, boolean increase ) {
        String result = null;
        boolean found = false;
        for ( int i = 0; i < columns; i++ ) {
            String nonBasic = this.nonBasicVariables.get( i );
            if ( ( !found || nonBasic.compareTo( result ) < 0 ) && canPivot( violatingVariable, nonBasic, increase ) ) {
                result = nonBasic;
                found = true;
            }
        }
        return result;
    }

    private boolean canPivot( String basic, String nonBasic, boolean increase ) {
        int basicIndex = this.basicVariables.indexOf( basic );
        int nonBasicIndex = this.nonBasicVariables.indexOf( nonBasic );
        double a = tableau[basicIndex][nonBasicIndex];
        if ( a == 0 ) {
            return false;
        }

        if ( increase == a > 0 ) {
            return canBeIncreased( nonBasic );
        } else {
            return canBeDecreased( nonBasic );
        }
    }

    private void pivot( String basic, String nonBasic, boolean increase ) {
        int basicIndex = this.basicVariables.indexOf( basic );
        int nonBasicIndex = this.nonBasicVariables.indexOf( nonBasic );

        if ( increase ) {
            double l = lowerBounds.get( basic );
            values.set( nonBasicIndex, l );
        } else {
            double u = upperBounds.get( basic );
            values.set( nonBasicIndex, u );
        }

        // Swap
        this.basicVariables.set( basicIndex, nonBasic );
        this.nonBasicVariables.set( nonBasicIndex, basic );
        double coefficient = tableau[basicIndex][nonBasicIndex];
        tableau[basicIndex][nonBasicIndex] = 1.0 / coefficient;
        for ( int j = 0; j < columns; j++ ) {
            if ( j != nonBasicIndex ) {
                tableau[basicIndex][j] /= -coefficient;
            }
        }

        // Replace in other rows
        for ( int i = 0; i < rows; i++ ) {
            if ( i == basicIndex ) {
                continue;
            }

            double m = tableau[i][nonBasicIndex];
            tableau[i][nonBasicIndex] = m * tableau[basicIndex][nonBasicIndex];
            for ( int j = 0; j < columns; j++ ) {
                if ( j != nonBasicIndex ) {
                    tableau[i][j] += m * tableau[basicIndex][j];
                }
            }
        }
    }

    private boolean canBeIncreased( String variable ) {
        if ( upperBounds.containsKey( variable ) ) {
            double u = upperBounds.get( variable );
            double value = values.get( nonBasicVariables.indexOf( variable ) );

            return value < u;
        }
        return true;
    }

    private boolean canBeDecreased( String variable ) {
        if ( lowerBounds.containsKey( variable ) ) {
            double l = lowerBounds.get( variable );
            double value = values.get( nonBasicVariables.indexOf( variable ) );

            return value > l;
        }
        return true;
    }

    public double getBasicValue( int row ) {
        double result = 0;
        for ( int i = 0; i < columns; i++ ) {
            result += tableau[row][i] * values.get( i );
        }
        return result;
    }

    public void printTableau() {
        System.out.println( "---------------------" );
        System.out.print( "       " );
        for ( int i = 0; i < nonBasicVariables.size(); i++ ) {
            String variable = nonBasicVariables.get( i );
            System.out.printf( " %s[%.2f]", variable, values.get( i ) );
        }
        System.out.println();

        for ( int i = 0; i < rows; i++ ) {
            System.out.printf( "%s[%.2f] ", basicVariables.get( i ), getBasicValue( i ) );
            for ( int j = 0; j < columns; j++ ) {
                System.out.printf( "  %.2f  ", tableau[i][j] );
            }
            System.out.println();
        }

        System.out.println();
        upperBounds.forEach( ( v, u ) -> System.out.printf( "%s <= %.2f%n", v, u ) );
        lowerBounds.forEach( ( v, l ) -> System.out.printf( "%s >= %.2f%n", v, l ) );

        System.out.println( "---------------------" );
    }

    record Violation(String variable, boolean increase) {

    }

    public static class SimplexResult {

        boolean feasible;
        VariableAssignment solution;
        Set<LinearConstraint> explanation;

        private SimplexResult( boolean feasible, VariableAssignment solution, Set<LinearConstraint> explanation ) {
            this.feasible = feasible;
            this.solution = solution;
            this.explanation = explanation;
        }

        public static SimplexResult infeasible( Set<LinearConstraint> explanation ) {
            return new SimplexResult(
                    false,
                    null,
                    explanation
            );
        }

        public static SimplexResult feasible( VariableAssignment solution ) {
            return new SimplexResult(
                    true,
                    solution,
                    null
            );
        }

        public boolean isFeasible() {
            return feasible;
        }

        public Set<LinearConstraint> getExplanation() {
            return explanation;
        }

        public VariableAssignment getSolution() {
            return solution;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if ( feasible ) {
                builder.append( solution );
            } else {
                for ( LinearConstraint linearConstraint : this.explanation ) {
                    builder.append( linearConstraint ).append( "; " );
                }
            }

            return builder.toString();
        }
    }
}
