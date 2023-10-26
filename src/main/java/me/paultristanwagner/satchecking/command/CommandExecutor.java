package me.paultristanwagner.satchecking.command;

import static me.paultristanwagner.satchecking.AnsiColor.RED;
import static me.paultristanwagner.satchecking.AnsiColor.RESET;

public class CommandExecutor {

  private final CommandRegistry commandRegistry = CommandRegistry.getInstance();

  private final Command fallbackCommand = commandRegistry.getCommandByName("sat");

  public void execute(String commandString) {
    String[] rawArgs = commandString.split(" ");

    String commandName = rawArgs[0];
    Command command = commandRegistry.getCommandByName(commandName);
    if (command == null) {
      System.out.println(RED + commandName + ": command not found" + RESET);
      System.out.println("Type '?' for help.");
      return;
    }

    String[] newArgs = new String[rawArgs.length - 1];
    System.arraycopy(rawArgs, 1, newArgs, 0, rawArgs.length - 1);
    boolean success = command.execute(commandName, newArgs);

    if(!success) {
      System.out.println(RED + "Syntax: " + command.getUsage() + RESET);
    }
  }
}
