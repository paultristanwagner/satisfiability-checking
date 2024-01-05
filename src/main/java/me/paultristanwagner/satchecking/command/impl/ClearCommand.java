package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;
import org.jline.reader.impl.LineReaderImpl;

import java.io.IOException;
import java.util.List;

import static me.paultristanwagner.satchecking.Main.TERMINAL;

public class ClearCommand extends Command {

  public ClearCommand() {
    super("clear", List.of("cls"), "Clears the console");
  }

  @Override
  public boolean execute(String label, String[] args) {
    try {
      new LineReaderImpl(TERMINAL).clearScreen();
    } catch (IOException ignored) { }

    return true;
  }
}
