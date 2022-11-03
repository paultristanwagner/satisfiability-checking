import me.paultristanwagner.satchecking.parse.EqualityConstraintParser;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class EqualityConstraintParserTest {

    @Test
    public void testEquality() {
        EqualityConstraintParser parser = new EqualityConstraintParser();
        EqualityConstraint constraint = parser.parse( "f1=z" );

        assertEquals( "f1", constraint.getLeft() );
        assertEquals( "z", constraint.getRight() );
        assertTrue( constraint.areEqual() );
    }

    @Test
    public void testInequality() {
        EqualityConstraintParser parser = new EqualityConstraintParser();
        EqualityConstraint constraint = parser.parse( "x!=y" );

        assertEquals( "x", constraint.getLeft() );
        assertEquals( "y", constraint.getRight() );
        assertFalse( constraint.areEqual() );
    }
}
