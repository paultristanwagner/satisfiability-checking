import me.paultristanwagner.satchecking.command.impl.ReadCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReadCommandTest {

  // Regression for #26: `read <file>` produces args of length 1 (the label is stripped by the
  // CommandExecutor), so the old `args.length < 2` check rejected every single-file invocation.
  @Test
  public void rejectsOnlyWhenNoFileGiven() {
    ReadCommand command = new ReadCommand();
    // No file argument -> usage error (returns false). Does not touch the command executor.
    assertFalse(command.execute("read", new String[0]));
  }

  @Test
  public void acceptsAFileArgument() {
    ReadCommand command = new ReadCommand();
    // A (non-existent) file argument is accepted as a valid invocation: the command handles it
    // gracefully and returns true rather than printing the usage error. Avoids the executor.
    assertTrue(command.execute("read", new String[] {"this-file-does-not-exist-xyz.smt"}));
  }
}
