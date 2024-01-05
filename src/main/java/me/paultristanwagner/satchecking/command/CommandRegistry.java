package me.paultristanwagner.satchecking.command;

import me.paultristanwagner.satchecking.command.impl.*;

import java.util.ArrayList;
import java.util.List;

public class CommandRegistry {

  private static CommandRegistry instance;

  private final List<Command> commands = new ArrayList<>();

  private CommandRegistry() {
    register(
        new HelpCommand(),
        new SATCommand(),
        new SMTCommand(),
        new SimplexCommand(),
        new TseitinCommand(),
        new ReadCommand(),
        new ClearCommand(),
        new ExitCommand(),
        new ReloadCommand()
    );
  }

  public Command getCommandByName(String name) {
    String lowerCaseName = name.toLowerCase();
    for (Command command : commands) {
      if (command.getName().equalsIgnoreCase(name) || command.getAliases().contains(lowerCaseName)) {
        return command;
      }
    }
    return null;
  }

  public void register(Command... commands) {
    for (Command command : commands) {
      register(command);
    }
  }

  public void register(Command command) {
    commands.add(command);
  }

  public List<Command> getCommands() {
    return commands;
  }

  public static CommandRegistry getInstance() {
    if (instance == null) {
      instance = new CommandRegistry();
    }

    return instance;
  }
}
