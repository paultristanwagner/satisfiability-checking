package me.paultristanwagner.satchecking.command;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {

  private final String name;
  private final List<String> aliases;
  private final String description;
  private final String usage;
  private final String information;

  protected Command(String name, List<String> aliases, String description) {
    this(name, aliases, description, "");
  }

  protected Command(String name, List<String> aliases, String description, String usage) {
    this(name, aliases, description, usage, "");
  }

  protected Command(String name, List<String> aliases, String description, String usage, String information) {
    this.name = name;
    this.description = description;
    this.usage = usage;

    this.aliases = new ArrayList<>();
    for(String alias : aliases) {
      this.aliases.add(alias.toLowerCase());
    }

    this.information = information;
  }

  public abstract boolean execute(String label, String[] args);

  public String getName() {
    return name;
  }

  public List<String> getAliases() {
    return aliases;
  }

  public String getDescription() {
    return description;
  }

  public String getUsage() {
    return usage;
  }

  public String getInformation() {
    return information;
  }
}
