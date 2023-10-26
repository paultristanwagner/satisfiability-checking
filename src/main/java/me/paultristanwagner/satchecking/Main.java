package me.paultristanwagner.satchecking;

import me.paultristanwagner.satchecking.command.CommandExecutor;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Main {

  private static final String WELCOME_MESSAGE =
      "SMT-Solver version 1.0-SNAPSHOT © 2021 Paul T. Wagner\n" + "Type '?' for help.";

  public static final CommandExecutor COMMAND_EXECUTOR = new CommandExecutor();

  public static Terminal TERMINAL;
  public static LineReader LINE_READER;

  public static void main(String[] args) throws IOException {
    TERMINAL = TerminalBuilder.terminal();
    LINE_READER = LineReaderBuilder.builder().terminal(TERMINAL).build();

    System.out.println(WELCOME_MESSAGE);

    Config.load();

    while (true) {
      String input;
      try {
        input = LINE_READER.readLine("> ");
      } catch (RuntimeException ignored) {
        // Program was terminated
        return;
      }

      COMMAND_EXECUTOR.execute(input);
    }
  }
}
