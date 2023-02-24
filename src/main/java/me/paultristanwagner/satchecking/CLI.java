package me.paultristanwagner.satchecking;

import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.Theory;
import me.paultristanwagner.satchecking.theory.solver.SimplexOptimizationSolver;
import me.paultristanwagner.satchecking.theory.solver.TheorySolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import static me.paultristanwagner.satchecking.AnsiColor.*;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class CLI {

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void main(String[] args) throws IOException {
    System.out.println("SMT-Solver version 1.0-SNAPSHOT © 2021 Paul T. Wagner");
    System.out.println("Type '?' for help.");

    Config config = Config.load();

    Scanner scanner = new Scanner(System.in);
    while (true) {
      System.out.print("> ");
      String input;
      try {
        input = scanner.nextLine();
      } catch (RuntimeException ignored) {
        // Program was terminated
        return;
      }

      String cnfString;
      String[] split = input.split(" ");
      String command = split[0];

      if (command.equals("read")) {
        if (split.length != 2) {
          System.out.println(RED + "Syntax: read <file>" + RESET);
          System.out.println();
          continue;
        }

        File file = new File(split[1]);
        if (!file.exists()) {
          System.out.printf("%sFile '%s' does not exists%s%n", RED, split[1], RESET);
          System.out.println();
          continue;
        }

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          builder.append(line);
        }

        input = builder.toString();
        split = input.split(" ");
        command = split[0];
      }

      if (command.equalsIgnoreCase("?") || command.equalsIgnoreCase("help")) {
        System.out.println("   ? - View this help page");
        System.out.println("   reloadConfig - Reloads the configuration file");
        System.out.println("   read <file> - Reads a command from the specified file");
        System.out.println(
            "   simplex <constraints> ... - Applies the simplex algorithm to the specified constraints");
        System.out.println("   smt <theory> <cnf of theory constraints> - Solves an SMT problem");
        System.out.println();
        continue;
      } else if (command.equals("reloadConfig")) {
        System.out.println(GREEN + "Reloading config..." + RESET);
        config = Config.reload();
        System.out.println(GREEN + "Done." + RESET);
        System.out.println();
        continue;
      } else if (command.equals("simplex")) {
        if (split.length == 1) {
          System.out.println(RED + "Syntax: simplex <constraints> ..." + RESET);
          continue;
        }

        SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
        LinearConstraintParser parser = new LinearConstraintParser();
        boolean syntaxError = false;
        for (int i = 1; i < split.length; i++) {
          try {
            LinearConstraint lc = parser.parse(split[i]);

            if (lc instanceof MaximizingConstraint) {
              simplex.maximize(lc);
            } else if (lc instanceof MinimizingConstraint) {
              simplex.minimize(lc);
            } else {
              simplex.addConstraint(lc);
            }
          } catch (SyntaxError e) {
            System.out.println(RED + "Syntax error: " + e.getMessage());
            System.out.println(split[i]);
            Parser.printPointer(e.getIndex());
            System.out.print(RESET);

            syntaxError = true;
            break;
          }
        }

        if (syntaxError) {
          continue;
        }

        long beforeMs = System.currentTimeMillis();
        SimplexResult result = simplex.solve();
        long afterMs = System.currentTimeMillis();

        if (result.isUnbounded()) {
          System.out.println(
              RED + "UNSAT! " + GRAY + "(" + RED + "feasible, but unbounded" + GRAY + ")");
          System.out.print(GREEN + "Solution: ");
          System.out.println(result);
        } else if (result.isOptimal()) {
          System.out.println();
          System.out.println(GREEN + "SAT! " + GRAY + "(" + GREEN + "optimal" + GRAY + ")");
          System.out.print(GREEN + "Solution: ");
          System.out.print(result);
          System.out.println(GREEN + "Optimum: " + result.getOptimum());
        } else if (result.isFeasible()) {
          System.out.println();
          System.out.println(GREEN + "SAT!");
          System.out.print(GREEN + "Solution: ");
          System.out.print(result);
        } else {
          System.out.println();
          System.out.println(RED + "UNSAT!");
          System.out.print("Explanation: ");
          System.out.print(result);
        }
        System.out.println(GRAY + "Time: " + (afterMs - beforeMs) + "ms" + RESET);
        System.out.println(RESET);
        continue;
      } else if (command.equals("smt")) {
        String theoryName = split[1];

        cnfString = input.substring(5 + theoryName.length());

        Theory theory = Theory.get(theoryName);
        TheoryCNFParser parser = theory.getCNFParser();
        SMTSolver smtSolver = theory.getSMTSolver();
        smtSolver.setSATSolver(new DPLLCDCLSolver());
        TheorySolver theorySolver = theory.getTheorySolver();
        smtSolver.setTheorySolver(theorySolver);

        TheoryCNF cnf;
        try {
          cnf = parser.parse(cnfString);
        } catch (Exception e) {
          e.printStackTrace();
          continue;
        }
        smtSolver.load(cnf);

        long beforeMs = System.currentTimeMillis();
        SMTResult<?> result = smtSolver.solve();
        long afterMs = System.currentTimeMillis();

        if (result.isSatisfiable()) {
          System.out.println(GREEN + "SAT:");
          System.out.println("" + result.getSolution());
        } else if (!theory.isComplete() && result.isUnknown()) {
          System.out.println(YELLOW + "UNKNOWN " + GRAY + "(theory is incomplete)");
        } else {
          System.out.println(RED + "UNSAT");
        }
        System.out.println(GRAY + "Time: " + (afterMs - beforeMs) + "ms");
        System.out.println(RESET);

        continue;
      } else {
        cnfString = input;
      }

      CNF cnf;
      try {
        cnf = CNF.parse(cnfString);
      } catch (RuntimeException e) {
        System.out.println(RED + "Syntax Error: " + e.getMessage() + RESET);
        System.out.println();
        continue;
      }

      SATSolver satSolver = config.getSolver();
      long beforeMs = System.currentTimeMillis();
      satSolver.load(cnf);
      Assignment model = satSolver.nextModel();
      if (model == null) {
        System.out.println(RED + "UNSAT" + RESET);
        System.out.println();
        continue;
      }

      long modelCount = 0;
      System.out.println(GREEN + "SAT:");
      while (model != null && modelCount < config.getMaxModelCount()) {
        modelCount++;

        if (config.printModels()) {
          System.out.println("" + GREEN + model + ";" + RESET);
        }

        model = satSolver.nextModel();
      }
      long timeMs = System.currentTimeMillis() - beforeMs;

      System.out.println("" + GREEN + modelCount + " model/s found in " + timeMs + " ms" + RESET);
      if (modelCount == config.getMaxModelCount()) {
        System.out.println(
            GRAY
                + "(List of models could be incomplete since maximum number of models is restricted)"
                + RESET);
      }
      System.out.println();
    }
  }
}
