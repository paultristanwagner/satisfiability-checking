package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.Config;
import me.paultristanwagner.satchecking.command.Command;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;

import java.util.List;

import static me.paultristanwagner.satchecking.AnsiColor.*;

public class SATCommand extends Command {

  public SATCommand() {
    super(
        "sat",
        List.of(),
        "Checks the satisfiability of a given formula in conjunctive normal form",
        "sat <formula>",
        """
          Examples:
            sat (~a | b) & (a)
            
            sat (a | b) & (~a | b) & (a | ~b) & (~a | ~b)
        """);
  }

  @Override
  public boolean execute(String label, String[] args) {
    String cnfString = String.join(" ", args);

    CNF cnf;
    try {
      cnf = CNF.parse(cnfString);
    } catch (SyntaxError e) {
      System.out.print(RED);
      e.printWithContext();
      System.out.print(RESET);

      return true;
    }

    SATSolver satSolver = Config.get().getSolver();
    long beforeMs = System.currentTimeMillis();
    satSolver.load(cnf);
    Assignment model = satSolver.nextModel();
    if (model == null) {
      System.out.println(RED + "UNSAT" + RESET);
      System.out.println();

      return true;
    }

    long modelCount = 0;
    System.out.println(GREEN + "SAT:");
    while (model != null && modelCount < Config.get().getMaxModelCount()) {
      modelCount++;

      if (Config.get().printModels()) {
        System.out.println("" + GREEN + model + ";" + RESET);
      }

      model = satSolver.nextModel();
    }

    long timeMs = System.currentTimeMillis() - beforeMs;

    System.out.println("" + GREEN + modelCount + " model/s found in " + timeMs + " ms" + RESET);
    if (modelCount == Config.get().getMaxModelCount()) {
      System.out.println(
          GRAY
              + "(List of models could be incomplete since maximum number of models is restricted)"
              + RESET);
    }
    System.out.println();

    return true;
  }
}
