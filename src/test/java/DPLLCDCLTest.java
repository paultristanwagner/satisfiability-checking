import me.paultristanwagner.satchecking.*;
import me.paultristanwagner.satchecking.builder.CNFBuilder;
import me.paultristanwagner.satchecking.builder.FunctionCNFBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DPLLCDCLTest {
    
    @Test
    public void testSat() {
        CNF cnf = CNF.parse( "(a) & (~a | b) & (~b | c) & (~c | d)" );
        Result result = DPLLCDCLSolver.check( cnf );
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
            Result result = DPLLCDCLSolver.check( cnf );
            assertFalse( result.isSatisfiable() );
        }
    }
    
    @Test
    public void testTiming() {
        long beforeMs = System.currentTimeMillis();
        
        CNF cnf = CNF.parse( "(a) & (b) & (c) & (d) & (e) & (f) & (g) & (h) & (i) & (j) & (k) & (l) & (m) & (n) & " +
                " (~a | ~b | ~c | ~d | ~e | ~f | ~g | ~h | ~i | ~j | ~k | ~l | ~m | ~n)" );
        DPLLCDCLSolver.check( cnf );
        
        long timeNeeded = System.currentTimeMillis() - beforeMs;
        System.out.printf( "Time needed for DPLL+CDCL algorithm: %d ms\n", timeNeeded );
    }
    
    @Test
    public void testModelCount() {
        List<String> domain = List.of( "a", "b", "c", "d", "e" );
        List<Integer> codomain = List.of( 1, 2, 3, 4, 5 );
        FunctionCNFBuilder<String, Integer> builder = CNFBuilder.function( domain, codomain );
        builder.bijective();
        
        CNF cnf = builder.build();
        
        Solver solver = new DPLLCDCLSolver();
        solver.load( cnf );
        
        int models = 0;
        while ( solver.nextModel() != null ) {
            models++;
        }
        
        assertEquals( 120, models );
    }
}