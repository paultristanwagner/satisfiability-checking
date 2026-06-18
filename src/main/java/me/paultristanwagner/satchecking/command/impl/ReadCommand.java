package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import static me.paultristanwagner.satchecking.AnsiColor.RED;
import static me.paultristanwagner.satchecking.AnsiColor.RESET;
import static me.paultristanwagner.satchecking.Main.COMMAND_EXECUTOR;

public class ReadCommand extends Command {

  public ReadCommand() {
    super("read", List.of(), "Reads a file and executes the commands in it", "read <file>");
  }

  @Override
  public boolean execute(String label, String[] args) {
    if (args.length < 1) {
      return false;
    }

    String fileName = String.join(" ", args);
    File file = new File(fileName);
    if (!file.exists()) {
      System.out.printf("%sFile '%s' does not exist%s%n", RED, fileName, RESET);
      System.out.println();
      return true;
    }

    // Execute each non-blank line as its own command. The previous implementation
    // concatenated all lines with no separator and ran the result as a single command.
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String command = line.trim();
        if (command.isEmpty()) {
          continue;
        }
        COMMAND_EXECUTOR.execute(command);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return true;
  }
}
