package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;

import java.util.List;

public class ExitCommand extends Command {

  public ExitCommand() {
    super(
        "exit",
        List.of("quit"),
        "Exits the program"
    );
  }

  public boolean execute(String label, String[] args) {
    System.exit(0);
    return true;
  }
}
