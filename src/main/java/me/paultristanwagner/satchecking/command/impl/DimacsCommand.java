package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;
import me.paultristanwagner.satchecking.parse.DimacsParser;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Result;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static me.paultristanwagner.satchecking.AnsiColor.GREEN;
import static me.paultristanwagner.satchecking.AnsiColor.RED;
import static me.paultristanwagner.satchecking.AnsiColor.RESET;

public class DimacsCommand extends Command {

  public DimacsCommand() {
    super(
        "dimacs",
        List.of(),
        "Reads a DIMACS CNF file and checks its satisfiability",
        "dimacs <file>",
        """
              Example:
                dimacs problem.cnf
            """);
  }

  @Override
  public boolean execute(String label, String[] args) {
    if (args.length < 1) {
      return false;
    }

    String fileName = String.join(" ", args);
    Path path = Path.of(fileName);
    if (!Files.exists(path)) {
      System.out.printf("%sFile '%s' does not exist%s%n", RED, fileName, RESET);
      System.out.println();
      return true;
    }

    String content;
    try {
      content = Files.readString(path);
    } catch (IOException e) {
      System.out.printf("%sCould not read file '%s': %s%s%n", RED, fileName, e.getMessage(), RESET);
      System.out.println();
      return true;
    }

    CNF cnf;
    try {
      cnf = new DimacsParser().parse(content);
    } catch (SyntaxError e) {
      System.out.print(RED);
      e.printWithContext();
      System.out.print(RESET);
      System.out.println();
      return true;
    }

    Result result = DPLLCDCLSolver.check(cnf);
    if (result.isSatisfiable()) {
      System.out.println(GREEN + "SAT" + RESET);
      System.out.println(GREEN + result.getAssignment().toString() + RESET);
    } else {
      System.out.println(RED + "UNSAT" + RESET);
    }
    System.out.println();

    return true;
  }
}
