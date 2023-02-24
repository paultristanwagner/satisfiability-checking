package me.paultristanwagner.satchecking;

import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicExpression;
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

  private static final String WELCOME_MESSAGE =
      "SMT-Solver version 1.0-SNAPSHOT © 2021 Paul T. Wagner\n" + "Type '?' for help.";

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void main(String[] args) throws IOException {
    System.out.println(WELCOME_MESSAGE);

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

      if (command.equals("clear")) {
        SystemUtil.clearConsole();
        System.out.println(WELCOME_MESSAGE);
        continue;
      }
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
        System.out.println("     Available theories:");
        System.out.println(
            "       QF_LRA (Linear real arithmetic), QF_LIA (Linear integer arithmetic), ");
        System.out.println(
            "       QF_EQ (Equality logic), QF_EQUF (Equality logic with uninterpreted functions)");
        System.out.println(
            "   tseitin <formula> - Transforms a given formula in propositional logic into conjuctive normal form");
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

        String constraints = input.substring(8);
        String[] splitConstraints = constraints.split(" ");

        SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
        LinearConstraintParser parser = new LinearConstraintParser();
        boolean syntaxError = false;

        int index = 0;
        for (String splitConstraint : splitConstraints) {
          try {
            LinearConstraint lc = parser.parse(splitConstraint);

            if (lc instanceof MaximizingConstraint) {
              simplex.maximize(lc);
            } else if (lc instanceof MinimizingConstraint) {
              simplex.minimize(lc);
            } else {
              simplex.addConstraint(lc);
            }
          } catch (SyntaxError e) {
            SyntaxError derivedError =
                new SyntaxError(e.getInternalMessage(), constraints, index + e.getIndex());
            System.out.print(RED);
            derivedError.printWithContext();
            System.out.print(RESET);

            syntaxError = true;
            break;
          }

          index += splitConstraint.length() + 1;
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
          System.out.println(GREEN + "SAT! " + GRAY + "(" + GREEN + "optimal" + GRAY + ")");
          System.out.print(GREEN + "Solution: ");
          System.out.print(result);
          System.out.println(GREEN + "Optimum: " + result.getOptimum());
        } else if (result.isFeasible()) {
          System.out.println(GREEN + "SAT!");
          System.out.print(GREEN + "Solution: ");
          System.out.print(result);
        } else {
          System.out.println(RED + "UNSAT!");
          System.out.print("Explanation: ");
          System.out.print(result);
        }
        System.out.println(GRAY + "Time: " + (afterMs - beforeMs) + "ms" + RESET);
        System.out.println(RESET);
        continue;
      } else if (command.equals("smt")) {
        if (split.length < 3) {
          System.out.println(RED + "Syntax: smt <theory> <cnf of theory constraints>");
          System.out.println(RESET);
          continue;
        }

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
        } catch (SyntaxError e) {
          System.out.print(RED);
          e.printWithContext();
          System.out.print(RESET);
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
      } else if(command.equals( "tseitin" )) {
        if (split.length == 1) {
          System.out.println(RED + "Syntax: tseitin <formula>" + RESET);
          continue;
        }

        String formulaString = input.substring(8);

        PropositionalLogicParser propositionalLogicParser = new PropositionalLogicParser();
        PropositionalLogicExpression expression;
        try {
          expression = propositionalLogicParser.parse(formulaString);
        } catch (SyntaxError e) {
          System.out.print(RED);
          e.printWithContext();
          System.out.print(RESET);
          continue;
        }

        CNF cnf = PropositionalLogicParser.tseitin( expression );

        System.out.println(GREEN + "Tseitin's transformation:");
        System.out.println( cnf );
        System.out.println(RESET);
        continue;
      } else {
        cnfString = input;

        // strip trailing whitespace
        cnfString = cnfString.replaceAll("\\s+$", "");
      }

      CNF cnf;
      try {
        cnf = CNF.parse(cnfString);
      } catch (SyntaxError e) {
        System.out.print(RED);
        e.printWithContext();
        System.out.print(RESET);
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
