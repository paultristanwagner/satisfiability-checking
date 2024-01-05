package me.paultristanwagner.satchecking.command.impl;

import me.paultristanwagner.satchecking.command.Command;
import me.paultristanwagner.satchecking.parse.ParseResult;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.Theory;
import me.paultristanwagner.satchecking.theory.solver.TheorySolver;

import java.util.List;

import static me.paultristanwagner.satchecking.AnsiColor.*;

public class SMTCommand extends Command {

  public SMTCommand() {
    super(
        "smt",
        List.of(),
        "Solves an SMT problem",
        "smt <theory> <cnf of theory constraints>",
        """
              Available theories:
                QF_LRA (Linear real arithmetic),
                QF_LIA (Linear integer arithmetic),
                QF_EQ (Equality logic),
                QF_EQUF (Equality logic with uninterpreted functions)
              
              Examples:
                smt QF_LRA (x <= 5) & (max(x))
              
                smt QF_LIA (x <= 3/2) & (max(x))
              
                smt QF_EQ (a=b) & (b=c) & (a!=c | c!=d)
              
                smt QF_EQUF (x=y) & (f(x) = y) & (f(f(x)) = y)
            """
    );
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public boolean execute(String label, String[] args) {
    if (args.length < 2) {
      return false;
    }

    String theoryName = args[0];

    String[] cnfArgs = new String[args.length - 1];
    System.arraycopy(args, 1, cnfArgs, 0, args.length - 1);
    String cnfString = String.join(" ", cnfArgs);

    Theory theory;
    try {
      theory = Theory.get(theoryName);
    } catch (IllegalArgumentException e) {
      System.out.println(RED);
      System.out.println("Unknown theory: " + theoryName);
      System.out.println(RESET);
      return true;
    }

    TheoryCNFParser parser = theory.getCNFParser();
    SMTSolver smtSolver = theory.getSMTSolver();
    smtSolver.setSATSolver(new DPLLCDCLSolver());
    TheorySolver theorySolver = theory.getTheorySolver();
    smtSolver.setTheorySolver(theorySolver);

    ParseResult<TheoryCNF> parseResult;
    try {
      parseResult = parser.parseWithRemaining(cnfString);
    } catch (SyntaxError e) {
      System.out.print(RED);
      e.printWithContext();
      System.out.print(RESET);

      return true;
    }

    if (!parseResult.complete()) {
      SyntaxError error = new SyntaxError("Unexpected token", cnfString, parseResult.charsRead());
      System.out.println(RED);
      error.printWithContext();
      System.out.println(RESET);
      return true;
    }

    TheoryCNF theoryCNF = parseResult.result();
    smtSolver.load(theoryCNF);

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

    return true;
  }
}
