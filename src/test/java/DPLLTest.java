import me.paultristanwagner.satchecking.Assignment;
import me.paultristanwagner.satchecking.CNF;
import me.paultristanwagner.satchecking.DPLLSolver;
import me.paultristanwagner.satchecking.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DPLLTest {
    
    @Test
    public void testSat() {
        CNF cnf = CNF.parse( "(a) & (~a | b) & (~b | c) & (~c | d)" );
        Result result = DPLLSolver.check( cnf );
        assertTrue( result.isSatisfiable() );
        Assignment assignment = result.getAssignment();
        assertTrue( assignment.evaluate( cnf ) );
    }
    
    @Test
    public void testUnsat() {
        List<String> cnfStrings = List.of(
                "(a) & (b) & (c) & (d) & (e) & (~a | ~b | ~c | ~d | ~e)",
                "(a | b) & (~a | b) & (a | ~b) & (~a | ~b)"
        );
        
        for ( String cnfString : cnfStrings ) {
            CNF cnf = CNF.parse( cnfString );
            Result result = DPLLSolver.check( cnf );
            assertFalse( result.isSatisfiable() );
        }
    }
    
    @Test
    public void testTiming() {
        long beforeMs = System.currentTimeMillis();
        
        CNF cnf = CNF.parse( "(a) & (b) & (c) & (d) & (e) & (f) & (g) & (h) & (i) & (j) & (k) & (l) & (m) & (n) & " +
                " (~a | ~b | ~c | ~d | ~e | ~f | ~g | ~h | ~i | ~j | ~k | ~l | ~m | ~n)" );
        DPLLSolver.check( cnf );
        
        long timeNeeded = System.currentTimeMillis() - beforeMs;
        System.out.printf( "Time needed for DPLL algorithm: %d ms\n", timeNeeded );
    }
}