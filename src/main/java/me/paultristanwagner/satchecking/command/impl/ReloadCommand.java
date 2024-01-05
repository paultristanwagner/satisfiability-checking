package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.Config;
import me.paultristanwagner.satchecking.command.Command;

import java.util.List;

public class ReloadCommand extends Command {

  public ReloadCommand() {
    super("reload", List.of("rl"), "Reloads the configuration file");
  }

  @Override
  public boolean execute(String label, String[] args) {
    Config.reload();

    System.out.println("Configuration reloaded");

    return true;
  }
}
