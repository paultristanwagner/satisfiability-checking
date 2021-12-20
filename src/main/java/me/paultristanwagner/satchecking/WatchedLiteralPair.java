package me.paultristanwagner.satchecking;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class WatchedLiteralPair {

    private final Clause clause;
    private final Literal[] watched;

    public WatchedLiteralPair( Clause clause, Assignment assignment ) {
        this.clause = clause;
        this.watched = new Literal[Math.min( 2, clause.getLiterals().size() )];

        int correct = 0;
        for ( int i = 0; i < clause.getLiterals().size(); i++ ) {
            Literal literal = clause.getLiterals().get( i );
            if ( !assignment.assigns( literal ) || assignment.evaluate( literal ) ) {
                watched[correct] = literal;
                correct++;
                if ( correct == 2 ) {
                    return;
                }
            } else {
                watched[correct + i % 2] = literal;
            }
        }
    }

    public boolean isConflicting( Assignment assignment ) {
        for ( Literal literal : watched ) {
            if ( !assignment.assigns( literal ) || assignment.evaluate( literal ) ) {
                return false;
            }
        }
        return true;
    }

    public Literal getUnitLiteral( Assignment assignment ) {
        int unassignedCount = 0;
        Literal unassignedLiteral = null;
        for ( Literal literal : watched ) {
            if ( !assignment.assigns( literal ) ) {
                unassignedCount++;
                unassignedLiteral = literal;
            } else if ( assignment.evaluate( literal ) ) {
                return null;
            }
        }

        if ( unassignedCount == 1 ) {
            return unassignedLiteral;
        }
        return null;
    }

    public Literal attemptReplace( Literal literal, Assignment assignment ) {
        Literal other = getOther( literal );
        if ( assignment.assigns( other ) && assignment.evaluate( other ) ) {
            return null;
        }

        Literal replacement = null;
        for ( Literal lit : clause.getLiterals() ) {
            if ( !lit.equals( literal ) && !isWatched( lit ) && ( !assignment.assigns( lit ) || assignment.evaluate( lit ) ) ) {
                replacement = lit;
                break;
            }
        }

        if ( replacement != null ) {
            replace( literal, replacement );
        }
        return replacement;
    }

    private void replace( Literal replaced, Literal replacement ) {
        for ( int i = 0; i < watched.length; i++ ) {
            Literal lit = watched[i];
            if ( lit.equals( replaced ) ) {
                watched[i] = replacement;
                break;
            }
        }
    }

    public boolean isWatched( Literal literal ) {
        for ( Literal lit : watched ) {
            if ( lit.equals( literal ) ) {
                return true;
            }
        }
        return false;
    }

    public Literal getOther( Literal one ) {
        for ( Literal literal : watched ) {
            if ( !literal.equals( one ) ) {
                return literal;
            }
        }
        return null;
    }

    public Literal[] getWatched() {
        return watched;
    }
}
