package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;
import me.paultristanwagner.satchecking.command.CommandRegistry;

import java.util.List;

public class HelpCommand extends Command {

  private static final String FORMAT = "  %s - %s\n";

  public HelpCommand() {
    super(
        "?",
        List.of("help"),
        "Views the help page"
    );
  }

  @Override
  public boolean execute(String label, String[] args) {
    if(args.length == 1) {
      String commandName = args[0];
      Command command = CommandRegistry.getInstance().getCommandByName(commandName);

      System.out.printf(FORMAT, command.getName(), command.getDescription());
      System.out.println();
      System.out.println(command.getInformation());
      System.out.println();
      return true;
    }

    for (Command command : CommandRegistry.getInstance().getCommands()) {
      System.out.printf(FORMAT, command.getName(), command.getDescription());
      System.out.println();
    }

    System.out.println("  Type '? <command>' for more information on a specific command.");

    return true;
  }
}
