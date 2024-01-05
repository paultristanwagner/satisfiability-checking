package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;
import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.solver.SimplexOptimizationSolver;

import java.util.List;

import static me.paultristanwagner.satchecking.AnsiColor.*;

public class SimplexCommand extends Command {

  public SimplexCommand() {
    super(
        "simplex",
        List.of(),
        "Applies the simplex algorithm to the specified constraints",
        "simplex <constraints> ...",
        """
           Examples:
             simplex x+y>=10 x-y<=5 1/2x-y<=0
             
             simplex min(x) x+y>=10 x-y<=5 1/2x-y<=0
         """);
  }

  @Override
  public boolean execute(String label, String[] args) {
    if(args.length == 0) {
      return false;
    }

    String constraints = String.join(" ", args);

    SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
    LinearConstraintParser parser = new LinearConstraintParser();
    boolean syntaxError = false;

    int index = 0;
    for (String arg : args) {
      try {
        LinearConstraint lc = parser.parse(arg);

        if (lc instanceof LinearConstraint.MaximizingConstraint) {
          simplex.maximize(lc);
        } else if (lc instanceof LinearConstraint.MinimizingConstraint) {
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

      index += arg.length() + 1;
    }

    if (syntaxError) {
      return true;
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

    return true;
  }
}
