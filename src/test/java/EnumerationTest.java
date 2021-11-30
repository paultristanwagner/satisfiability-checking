import me.paultristanwagner.satchecking.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnumerationTest {
    
    @Test
    public void testSat() {
        CNF cnf = CNF.parse( "(a) & (~a | b) & (~b | c) & (~c | d)" );
        Result result = Enumeration.check( cnf );
        assertTrue( result.isSatisfiable() );
        Assignment assignment = result.getAssignment();
        assertTrue( assignment.getValue( "a" ) );
        assertTrue( assignment.getValue( "b" ) );
        assertTrue( assignment.getValue( "c" ) );
        assertTrue( assignment.getValue( "d" ) );
    }
    
    @Test
    public void testUnsat() {
        CNF cnf = CNF.parse( "(a) & (b) & (c) & (d) & (e) & (~a | ~b | ~c | ~d | ~e)" );
        Result result = Enumeration.check( cnf );
        assertFalse( result.isSatisfiable() );
    }
}
