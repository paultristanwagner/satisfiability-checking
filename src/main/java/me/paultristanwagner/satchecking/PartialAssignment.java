package me.paultristanwagner.satchecking;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class PartialAssignment extends Assignment {

    private boolean complete;

    public PartialAssignment( Assignment other ) {
        super( other );
        complete = false;
    }

    public static PartialAssignment incomplete( Assignment assignment ) {
        PartialAssignment pa = new PartialAssignment( assignment );
        pa.complete = false;
        return pa;
    }

    public static final PartialAssignment complete( Assignment assignment ) {
        PartialAssignment pa = new PartialAssignment( assignment );
        pa.complete = true;
        return pa;
    }

    public boolean isComplete() {
        return complete;
    }
}
