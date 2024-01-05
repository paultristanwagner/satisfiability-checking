package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicExpression;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.sat.CNF;

import java.util.List;

import static me.paultristanwagner.satchecking.AnsiColor.*;

public class TseitinCommand extends Command {

  public TseitinCommand() {
    super("tseitin",
        List.of("tseytin"),
        "Transforms a propositional logic formula into a satisfiability equivalent formula in CNF",
        "tseitin <formula>",
        """
              Examples:
                tseitin a -> b -> c
                
                tseitin ~(a <-> b | c)
            """
    );
  }

  @Override
  public boolean execute(String label, String[] args) {
    if (args.length == 0) {
      return false;
    }

    String formulaString = String.join(" ", args);

    PropositionalLogicParser propositionalLogicParser = new PropositionalLogicParser();
    PropositionalLogicExpression expression;
    try {
      expression = propositionalLogicParser.parse(formulaString);
    } catch (SyntaxError e) {
      System.out.print(RED);
      e.printWithContext();
      System.out.print(RESET);

      return true;
    }

    CNF cnf = PropositionalLogicParser.tseitin(expression);

    System.out.println(GREEN + "Tseitin's transformation:");
    System.out.println(cnf);
    System.out.println(RESET);

    return true;
  }
}
