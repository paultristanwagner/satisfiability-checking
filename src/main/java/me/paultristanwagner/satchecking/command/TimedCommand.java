package me.paultristanwagner.satchecking.command;

import java.util.List;

public abstract class TimedCommand extends Command {

  protected TimedCommand(String name, List<String> aliases, String description, String usage) {
    super(name, aliases, description, usage);
  }
}
