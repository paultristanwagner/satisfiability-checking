import me.paultristanwagner.satchecking.CNF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CNFParserTest {
    
    @Test
    public void testParser() {
        List<String> cnfStrings = List.of(
                "(a)",
                "(a | b | c)",
                "(~a)",
                "(~a | b | ~c)",
                "(a) & (b)",
                "(a) & (a | b) & (~b | c)"
        );
        
        for ( String cnfString : cnfStrings ) {
            CNF cnf = CNF.parse( cnfString );
            Assertions.assertEquals( cnfString, cnf.toString() );
        }
    }
}
