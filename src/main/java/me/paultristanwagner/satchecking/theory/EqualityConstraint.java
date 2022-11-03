package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.Constraint;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class EqualityConstraint implements Constraint {

    private final String left;
    private final String right;
    private final boolean equal;

    public EqualityConstraint( String left, String right, boolean equal ) {
        this.left = left;
        this.right = right;
        this.equal = equal;
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }

    public boolean areEqual() {
        return equal;
    }
}
