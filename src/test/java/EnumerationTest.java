import me.paultristanwagner.satchecking.Assignment;
import me.paultristanwagner.satchecking.CNF;
import me.paultristanwagner.satchecking.Enumeration;
import me.paultristanwagner.satchecking.Result;
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
        assertTrue(assignment.evaluate( cnf ));
    }
    
    @Test
    public void testUnsat() {
        CNF cnf = CNF.parse( "(a) & (b) & (c) & (d) & (e) & (~a | ~b | ~c | ~d | ~e)" );
        Result result = Enumeration.check( cnf );
        assertFalse( result.isSatisfiable() );
    }
    
    @Test
    public void testTiming() {
        long beforeMs = System.currentTimeMillis();
        
        CNF cnf = CNF.parse( "(a) & (b) & (c) & (d) & (e) & (f) & (g) & (h) & (i) & (j) & (k) & (l) & (m) & (n) & " +
                " (~a | ~b | ~c | ~d | ~e | ~f | ~g | ~h | ~i | ~j | ~k | ~l | ~m | ~n)" );
        Enumeration.check( cnf );
        
        long timeNeeded = System.currentTimeMillis() - beforeMs;
        System.out.printf( "Time needed for Enumeration algorithm: %d ms\n", timeNeeded );
    }
}
