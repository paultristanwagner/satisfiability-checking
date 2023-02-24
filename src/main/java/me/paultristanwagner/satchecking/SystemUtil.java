package me.paultristanwagner.satchecking;

import java.io.Console;

public class SystemUtil {

  public static void clearConsole() {
    Console console = System.console();
    boolean ansiCompatible = console != null && System.getenv().get("TERM") != null;
    if (ansiCompatible) {
      System.out.print("\033[H\033[2J");
      System.out.flush();
    } else {
      for (int i = 0; i < 100; i++) {
        System.out.println();
      }
    }
  }
}
