import me.paultristanwagner.satchecking.Assignment;
import me.paultristanwagner.satchecking.CNF;
import me.paultristanwagner.satchecking.DPLL;
import me.paultristanwagner.satchecking.DPLLResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EnumerationTest {
    
    @Test
    public void testSat() {
        CNF cnf = CNF.parse( "(a) & (~a | b) & (~b | c) & (~c | d)" );
        DPLLResult result = DPLL.enumeration( cnf );
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
        DPLLResult result = DPLL.enumeration( cnf );
        assertFalse( result.isSatisfiable() );
    }
}
