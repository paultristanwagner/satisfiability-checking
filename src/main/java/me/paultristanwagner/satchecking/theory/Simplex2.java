package me.paultristanwagner.satchecking.theory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class Simplex2 {

    private final List<String> allVariables;
    private final List<String> basicVariables;

    private final Set<String> originalVariables;
    private final Set<String> slackVariables;
    private final Set<String> nonBasicVariables;
    
    private final BiMap<String, String> substitutions;
    private final Map<String, Double> offsets;
    
    private final Map<String, Pair<String, String>> unbounded;
    
    private int rows;
    private int columns;
    private double[][] tableau;
    
    private final List<LinearConstraint> originalConstraints;
    private List<LinearConstraint> constraints;
    private LinearConstraint originalObjective;
    private LinearConstraint objective;
    
    public static void main( String[] args ) {
        LinearConstraintParser parser = new LinearConstraintParser();
        
        Simplex2 simplex = new Simplex2();
        simplex.addConstraint( parser.parse( "2x+y<=3" ) );
        simplex.addConstraint( parser.parse( "2x+y>=10" ) );
        
        SimplexResult result = simplex.solve();
        if ( result.isUnbounded() ) {
            System.out.println( "Feasible, but unbounded" );
            System.out.println( result.getSolution() );
        } else if ( result.isOptimal() ) {
            System.out.println( "Optimal" );
            System.out.println( result.getSolution() );
            System.out.println( "Optimum: " + result.getOptimum() );
        } else if ( result.isFeasible() ) {
            System.out.println( "Feasible" );
            System.out.println( result.getSolution() );
        } else {
            System.out.println( "Infeasible" );
        }
    }
    
    public Simplex2() {
        this.allVariables = new ArrayList<>();
        
        this.originalVariables = new HashSet<>();
        this.slackVariables = new HashSet<>();
        
        this.basicVariables = new ArrayList<>();
        this.nonBasicVariables = new HashSet<>();
        
        this.originalConstraints = new ArrayList<>();
        this.constraints = new ArrayList<>();
        
        this.substitutions = HashBiMap.create();
        this.offsets = new HashMap<>();
        
        this.unbounded = new HashMap<>();
    }
    
    public void maximize( LinearConstraint f ) {
        if ( !( f instanceof MaximizingConstraint ) ) {
            throw new IllegalArgumentException( "The objective function must be a maximizing constraint." );
        }
    
        if ( originalObjective != null ) {
            throw new IllegalStateException( "The objective function has already been set." );
        }
    
        this.originalObjective = f;
        this.objective = f;
    }
    
    public void minimize( LinearConstraint f ) {
        if ( !( f instanceof MinimizingConstraint ) ) {
            throw new IllegalArgumentException( "The objective function must be a minimizing constraint." );
        }
    
        if ( originalObjective != null ) {
            throw new IllegalStateException( "The objective function has already been set." );
        }
    
        this.originalObjective = f;
        this.objective = new LinearConstraint( f );
    
        for ( String variable : objective.getVariables() ) {
            objective.setCoefficient( variable, -objective.getCoefficients().get( variable ) );
        }
    }
    
    public SimplexResult solve() {
        this.originalConstraints.addAll( constraints );
    
        // Collect variables
        Set<String> tempSet = new HashSet<>();
        for ( LinearConstraint constraint : constraints ) {
            originalVariables.addAll( constraint.getVariables() );

            tempSet.addAll( constraint.getVariables() );
            nonBasicVariables.addAll( constraint.getVariables() );
        }
        allVariables.addAll( tempSet );

        // Infer bounds
        Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferedBounds = inferBounds();
    
        SimplexResult result = checkBoundsConsistency( inferedBounds );
        if ( result != null && !result.isFeasible() ) {
            return result;
        }

        List<String> withoutLowerBounds = new ArrayList<>( allVariables );
        withoutLowerBounds.removeAll( inferedBounds.getLeft().keySet() );
    
        // Transform constraints, where a single variable has a bound other than zero
        transformOffsetVariables( inferedBounds );
    
        // Replace unbounded variables
        replaceUnboundedVariables( withoutLowerBounds );
    
        // Add slack variables
        for ( LinearConstraint constraint : constraints ) {
            String slackVariable = freshVariable( "slack" );
            slackVariables.add( slackVariable );
            allVariables.add( slackVariable );
            basicVariables.add( slackVariable );
        }
    
        // Sort variables
        allVariables.sort( ( o1, o2 ) -> {
            if ( slackVariables.contains( o1 ) && slackVariables.contains( o2 ) ) {
                return o1.compareTo( o2 );
            } else if ( slackVariables.contains( o1 ) ) {
                return 1;
            } else if ( slackVariables.contains( o2 ) ) {
                return -1;
            }
            return o1.compareTo( o2 );
        } );
        
        // Create tableau
        createTableau();
        
        // First phase, find feasible solution
        while ( true ) {
            int violatingRow = -1;
            for ( int i = 0; i < basicVariables.size(); i++ ) {
                double value = tableau[ i + 1 ][ allVariables.size() ];
                
                if ( value < 0 ) {
                    violatingRow = i + 1;
                    break;
                }
            }
            
            if ( violatingRow == -1 ) {
                VariableAssignment solution = calculateSolution();
                result = SimplexResult.feasible( solution );
                break;
            }
            
            // check for negative coefficient
            int pivotColumn = -1;
            for ( int j = 0; j < allVariables.size(); j++ ) {
                if ( tableau[ violatingRow ][ j ] < 0 ) {
                    pivotColumn = j;
                    break;
                }
            }
            
            if ( pivotColumn == -1 ) {
                Set<LinearConstraint> explanation = calculateExplanation( violatingRow );
                result = SimplexResult.infeasible( explanation );
                
                return result;
            }
            
            pivot( violatingRow, pivotColumn );
        }
        
        // Second phase, maximize
        if ( objective == null ) {
            return result;
        }
        
        while ( true ) {
            // Find negative entry in objective row
            int pivotColumn = -1;
            double smallestValue = Double.MAX_VALUE;
            for ( int j = 0; j < allVariables.size(); j++ ) {
                double f_j = tableau[ 0 ][ j ];
                if ( f_j < 0 && f_j < smallestValue ) {
                    pivotColumn = j;
                    smallestValue = f_j;
                }
            }
            
            if ( pivotColumn == -1 ) {
                VariableAssignment solution = calculateSolution();
                
                double optimum = calculateObjectiveValue();
                
                result = SimplexResult.optimal( solution, optimum );
                
                return result;
            }
            
            // Find row with the smallest ratio
            int pivotRow = -1;
            double minRatio = Double.MAX_VALUE;
            for ( int i = 1; i < rows; i++ ) {
                if ( tableau[ i ][ pivotColumn ] <= 0 ) {
                    continue;
                }
                
                double ratio = tableau[ i ][ columns - 1 ] / tableau[ i ][ pivotColumn ];
                if ( ratio >= 0 && ratio < minRatio ) {
                    pivotRow = i;
                    minRatio = ratio;
                }
            }
            
            if ( pivotRow == -1 ) {
                VariableAssignment solution = calculateSolution();
                result = SimplexResult.unbounded( solution );
                return result;
            }
            
            pivot( pivotRow, pivotColumn );
        }
    }
    
    private void pivot( int pivotRow, int pivotColumn ) {
        String leaving = basicVariables.get( pivotRow - 1 );
        String entering = allVariables.get( pivotColumn );
        
        nonBasicVariables.remove( entering );
        nonBasicVariables.add( leaving );
        basicVariables.set( pivotRow - 1, entering );
        
        double temp = tableau[ pivotRow ][ pivotColumn ];
        
        for ( int j = 0; j < allVariables.size() + 1; j++ ) {
            tableau[ pivotRow ][ j ] /= temp;
        }
    
        for ( int i = 0; i < constraints.size() + 1; i++ ) {
            if ( i == pivotRow ) continue;
        
            double factor = tableau[ i ][ pivotColumn ] / tableau[ pivotRow ][ pivotColumn ];
            for ( int j = 0; j < allVariables.size() + 1; j++ ) {
                tableau[ i ][ j ] += -factor * tableau[ pivotRow ][ j ];
            }
        }
    }
    
    private Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferBounds() {
        Map<String, LinearConstraint> lowerBounds = new HashMap<>();
        Map<String, LinearConstraint> upperBounds = new HashMap<>();
        
        List<LinearConstraint> keptConstraints = new ArrayList<>();
        for ( String variable : allVariables ) {
            Iterator<LinearConstraint> iterator = constraints.iterator();
            while ( iterator.hasNext() ) {
                LinearConstraint constraint = iterator.next();
                if ( constraint.getCoefficients().size() != 1 ) {
                    iterator.remove();
                    keptConstraints.add( constraint );
                    continue;
                }
                
                String constraintVariable = constraint.getVariables().iterator().next();
                if ( !variable.equals( constraintVariable ) )
                    continue;
                
                double bound = constraint.getBoundOn( constraintVariable );
                
                if ( constraint.getBound() != LinearConstraint.Bound.UPPER == constraint.getCoefficients().get( constraintVariable ) > 0 ) {
                    if ( !lowerBounds.containsKey( variable ) || lowerBounds.get( variable ).getBoundOn( variable ) < bound ) {
                        lowerBounds.put( variable, constraint );
                    }
                } else {
                    if ( !upperBounds.containsKey( variable ) || upperBounds.get( variable ).getBoundOn( variable ) > bound ) {
                        upperBounds.put( variable, constraint );
                    }
                }
            }
        }
        
        keptConstraints.addAll( lowerBounds.values() );
        keptConstraints.addAll( upperBounds.values() );
        constraints = keptConstraints;
        
        return Pair.of( lowerBounds, upperBounds );
    }
    
    private SimplexResult checkBoundsConsistency( Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferedBounds ) {
        Map<String, LinearConstraint> lowerBounds = inferedBounds.getLeft();
        Map<String, LinearConstraint> upperBounds = inferedBounds.getRight();
        for ( String variable : allVariables ) {
            LinearConstraint lowerBound = lowerBounds.get( variable );
            LinearConstraint upperBound = upperBounds.get( variable );
            double lowerBoundValue = lowerBound != null ? lowerBounds.get( variable ).getBoundOn( variable ) : Double.NEGATIVE_INFINITY;
            double upperBoundValue = upperBound != null ? upperBounds.get( variable ).getBoundOn( variable ) : Double.POSITIVE_INFINITY;
            if ( lowerBoundValue > upperBoundValue ) {
                Set<LinearConstraint> explanation = new HashSet<>();
                explanation.add( lowerBound.getRoot() );
                explanation.add( upperBound.getRoot() );
                return SimplexResult.infeasible( explanation );
            }
        }
        
        return null;
    }
    
    private void transformOffsetVariables( Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferedBounds ) {
        Map<String, LinearConstraint> lowerBounds = inferedBounds.getLeft();
        for ( ListIterator<String> iterator = allVariables.listIterator(); iterator.hasNext(); ) {
            String variable = iterator.next();
            if ( !lowerBounds.containsKey( variable ) )
                continue;
            
            LinearConstraint lowerBound = lowerBounds.get( variable );
            double bound = lowerBound.getBoundOn( variable );
            
            if ( bound == 0 )
                continue;
            
            String substitute = freshVariable( "subst" );
            offsets.put( substitute, bound );
            substitutions.put( variable, substitute );
            
            iterator.remove();
            iterator.add( substitute );

            nonBasicVariables.remove( variable );
            nonBasicVariables.add( substitute );

            for ( int i = 0; i < constraints.size(); i++ ) {
                LinearConstraint linearConstraint = constraints.get( i );
                if ( linearConstraint.getCoefficients().containsKey( variable ) ) {
                    LinearConstraint offsetConstraint = linearConstraint.offset( variable, substitute, bound );
                    constraints.set( i, offsetConstraint );
                    
                }
            }
            
            if ( objective != null ) {
                objective = objective.offset( variable, substitute, bound );
            }
        }
    }
    
    private void replaceUnboundedVariables( List<String> withoutLowerBounds ) {
        for ( String unboundedVariable : withoutLowerBounds ) {
            String positive = freshVariable( "p_" + unboundedVariable );
            String negative = freshVariable( "n_" + unboundedVariable );
            unbounded.put( unboundedVariable, Pair.of( positive, negative ) );

            allVariables.add( positive );
            allVariables.add( negative );
            allVariables.remove( unboundedVariable );

            nonBasicVariables.add( positive );
            nonBasicVariables.add( negative );
            nonBasicVariables.remove( unboundedVariable );

            for ( int i = 0; i < constraints.size(); i++ ) {
                LinearConstraint linearConstraint = constraints.get( i );
                if ( linearConstraint.getCoefficients().containsKey( unboundedVariable ) ) {
                    LinearConstraint positiveNegative = linearConstraint.positiveNegativeSubstitute( unboundedVariable, positive, negative );
                    constraints.set( i, positiveNegative );
                }
            }
            
            if ( objective != null ) {
                objective = objective.positiveNegativeSubstitute( unboundedVariable, positive, negative );
            }
        }
    }
    
    private void createTableau() {
        rows = constraints.size() + 1;
        columns = allVariables.size() + 1;
        
        this.tableau = new double[ rows ][ columns ];
        
        // Enter target function to tableau
        if ( objective != null ) {
            for ( int i = 0; i < allVariables.size(); i++ ) {
                String variable = allVariables.get( i );
                tableau[ 0 ][ i ] = -1 * objective.getCoefficients().getOrDefault( variable, 0.0 );
            }
        }
        
        for ( int i = 0; i < constraints.size(); i++ ) {
            LinearConstraint constraint = constraints.get( i );
            
            if ( constraint.getBound() == LinearConstraint.Bound.LOWER ) {
                for ( int j = 0; j < allVariables.size(); j++ ) {
                    String variable = allVariables.get( j );
                    tableau[ i + 1 ][ j ] = -1 * constraint.getCoefficients().getOrDefault( variable, 0.0 );
                }
                tableau[ i + 1 ][ allVariables.size() ] = -1 * constraint.getValue();
            } else {
                for ( int j = 0; j < allVariables.size(); j++ ) {
                    String variable = allVariables.get( j );
                    tableau[ i + 1 ][ j ] = constraint.getCoefficients().getOrDefault( variable, 0.0 );
                }
                tableau[ i + 1 ][ allVariables.size() ] = constraint.getValue();
            }
            tableau[ i + 1 ][ nonBasicVariables.size() + i ] = 1;
        }
    }
    
    private double getPureValue( String variable ) {
        if ( nonBasicVariables.contains( variable ) ) {
            return 0;
        } else {
            int basisIndex = basicVariables.indexOf( variable );
            return tableau[ basisIndex + 1 ][ columns - 1 ];
        }
    }
    
    private double getValue( String variable ) {
        double value;
        if ( substitutions.containsKey( variable ) ) {
            String substitute = substitutions.get( variable );
            double offset = offsets.get( substitute );
            value = offset + getPureValue( substitute );
        } else if ( unbounded.containsKey( variable ) ) {
            double positive = getPureValue( unbounded.get( variable ).getLeft() );
            double negative = getPureValue( unbounded.get( variable ).getRight() );
            value = positive - negative;
        } else {
            value = getPureValue( variable );
        }
        
        return value;
    }
    
    private VariableAssignment calculateSolution() {
        VariableAssignment assignment = new VariableAssignment();
        
        for ( String variable : originalVariables ) {
            double value = getValue( variable );
            assignment.assign( variable, value );
        }
        
        return assignment;
    }
    
    private double calculateObjectiveValue() {
        MutableDouble objectiveValue = new MutableDouble( 0 );
        originalObjective.getCoefficients().forEach( ( variable, coefficient ) -> {
            double value = getValue( variable );
            objectiveValue.add( coefficient * value );
        } );
        
        return objectiveValue.getValue();
    }
    
    // todo: clean up this method
    private Set<LinearConstraint> calculateExplanation( int pivotRow ) {
        Set<LinearConstraint> explanation = new HashSet<>();
        
        explanation.add( constraints.get( pivotRow - 1 ).getRoot() );
        for ( int j = 0; j < columns - 1; j++ ) {
            String variable = allVariables.get( j );
            
            if ( tableau[ pivotRow ][ j ] == 0 ) {
                continue;
            }
            
            if ( j >= nonBasicVariables.size() ) {
                int basisIndex = j - nonBasicVariables.size();
                explanation.add( constraints.get( basisIndex ).getRoot() );
            } else {
                String actual = substitutions.inverse().getOrDefault( variable, variable );
                for ( LinearConstraint originalConstraint : originalConstraints ) {
                    if ( originalConstraint.getBound() == LinearConstraint.Bound.UPPER ) continue;
                    if ( originalConstraint.getCoefficients().size() != 1 ) continue;
                    
                    String onlyVariable = originalConstraint.getCoefficients().keySet().iterator().next();
                    if ( onlyVariable.equals( actual ) ) {
                        explanation.add( originalConstraint );
                    }
                }
            }
        }
        
        return explanation;
    }
    
    public void printTableau() {
        for ( int i = 0; i < allVariables.size() + 1; i++ ) {
            System.out.print( "-----------" );
        }
        System.out.println();
        
        System.out.println( "basic variables: " + basicVariables );
        
        for ( String allVariable : allVariables ) {
            System.out.printf( "%1$10s ", allVariable );
        }
        System.out.printf( "%1$10s", "b" );
        System.out.println();
        
        for ( double[] row : tableau ) {
            for ( double d : row ) {
                System.out.printf( "%10.2f ", d );
            }
            System.out.println();
        }
        
        for ( int i = 0; i < allVariables.size() + 1; i++ ) {
            System.out.print( "-----------" );
        }
        System.out.println();
    }
    
    public void addConstraint( LinearConstraint constraint ) {
        if ( constraint.getBound() == LinearConstraint.Bound.EQUAL ) {
            LinearConstraint first = new LinearConstraint();
            LinearConstraint second = new LinearConstraint();
            first.setDerivedFrom( constraint );
            second.setDerivedFrom( constraint );
    
            first.setBound( LinearConstraint.Bound.UPPER );
            second.setBound( LinearConstraint.Bound.LOWER );
            
            constraint.getCoefficients().forEach(
                    ( variable, coefficient ) -> {
                        first.setCoefficient( variable, coefficient );
                        second.setCoefficient( variable, coefficient );
                    }
            );
            first.setValue( constraint.getValue() );
            second.setValue( constraint.getValue() );
            
            constraints.add( first );
            constraints.add( second );
        } else {
            constraints.add( constraint );
        }
    }
    
    private String freshVariable( String prefix ) {
        int i = 0;
        String fresh;
        do {
            fresh = prefix + i;
            i += 1;
        } while ( allVariables.contains( fresh ) );
        
        return fresh;
    }
    
    public static class SimplexResult {
        
        boolean feasible;
        boolean unbounded;
        boolean optimal;
        double optimum;
        VariableAssignment solution;
        Set<LinearConstraint> explanation;
        
        public SimplexResult( boolean feasible, boolean unbounded, boolean optimal, double optimum, VariableAssignment solution, Set<LinearConstraint> explanation ) {
            this.feasible = feasible;
            this.unbounded = unbounded;
            this.optimal = optimal;
            this.optimum = optimum;
            this.solution = solution;
            this.explanation = explanation;
        }
        
        public static SimplexResult infeasible( Set<LinearConstraint> explanation ) {
            return new SimplexResult(
                    false,
                    false,
                    false,
                    0,
                    null,
                    explanation
            );
        }
        
        public static SimplexResult feasible( VariableAssignment solution ) {
            return new SimplexResult(
                    true,
                    false,
                    false,
                    0,
                    solution,
                    null
            );
        }
        
        public static SimplexResult optimal( VariableAssignment solution, double optimum ) {
            return new SimplexResult(
                    true,
                    false,
                    true,
                    optimum,
                    solution,
                    null
            );
        }
        
        public static SimplexResult unbounded( VariableAssignment solution ) {
            return new SimplexResult(
                    true,
                    true,
                    false,
                    0,
                    solution,
                    null
            );
        }
        
        public boolean isFeasible() {
            return feasible;
        }
        
        public boolean isUnbounded() {
            return unbounded;
        }
        
        public boolean isOptimal() {
            return optimal;
        }
        
        public Set<LinearConstraint> getExplanation() {
            return explanation;
        }
        
        public VariableAssignment getSolution() {
            return solution;
        }
        
        public double getOptimum() {
            return optimum;
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
