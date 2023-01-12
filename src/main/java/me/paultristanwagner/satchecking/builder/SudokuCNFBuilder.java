package me.paultristanwagner.satchecking.builder;

import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;

import java.util.ArrayList;
import java.util.List;

public class SudokuCNFBuilder extends CNFBuilder {

  public static void main(String[] args) {
    int blockSize = 3;
    SudokuCNFBuilder cnfBuilder = new SudokuCNFBuilder(blockSize);

    CNF sudokuCNF = cnfBuilder.build();
    System.out.println(sudokuCNF);

    SATSolver satSolver = new DPLLCDCLSolver();
    System.out.println("Load CNF...");
    satSolver.load(sudokuCNF);
    System.out.println("Try to solve...");

    long timeBefore = System.currentTimeMillis();

    int solutions = 0;
    Assignment assignment;
    while ((assignment = satSolver.nextModel()) != null) {
      if (solutions >= 1) {
        System.out.println();
        System.out.println();
      }
      System.out.println();

      solutions++;
      print(assignment, blockSize);
    }

    System.out.println();
    if (solutions == 0) {
      System.out.println("Sudoku not solvable.");
    } else {
      System.out.println("Found " + solutions + " solutions.");
    }

    long timeNeeded = System.currentTimeMillis() - timeBefore;
    long timeNeededSeconds = timeNeeded / 1000;
    System.out.println();
    System.out.println("Time needed: " + timeNeededSeconds + " s");
  }

  public SudokuCNFBuilder(final int blockSize) {
    final int n = blockSize * blockSize;

    // every cell has at least one value
    for (int i = 1; i <= n; i++) {
      for (int j = 1; j <= n; j++) {
        List<Literal> literals = new ArrayList<>();
        for (int v = 1; v <= n; v++) {
          Literal literal = cellLiteral(i, j, v);
          literals.add(literal);
        }
        Clause clause = new Clause(literals);
        add(clause);
      }
    }
    // every cell has at most one value
    for (int i = 1; i <= n; i++) {
      for (int j = 1; j <= n; j++) {
        for (int v1 = 1; v1 <= n - 1; v1++) {
          for (int v2 = v1 + 1; v2 <= n; v2++) {
            Literal l1 = cellLiteral(i, j, v1).not();
            Literal l2 = cellLiteral(i, j, v2).not();
            Clause clause = new Clause(List.of(l1, l2));
            add(clause);
          }
        }
      }
    }

    // in every row, every value is at least in one column
    for (int i = 1; i <= n; i++) {
      for (int v = 1; v <= n; v++) {
        List<Literal> literals = new ArrayList<>();
        for (int j = 1; j <= n; j++) {
          literals.add(cellLiteral(i, j, v));
        }
        Clause clause = new Clause(literals);
        add(clause);
      }
    }

    // in every column, every value is at least in one row
    for (int j = 1; j <= n; j++) {
      for (int v = 1; v <= n; v++) {
        List<Literal> literals = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
          literals.add(cellLiteral(i, j, v));
        }
        Clause clause = new Clause(literals);
        add(clause);
      }
    }

    // in every block, every value is at least in one cell
    for (int blockI = 0; blockI < blockSize; blockI++) {
      for (int blockJ = 0; blockJ < blockSize; blockJ++) {
        for (int v = 1; v <= n; v++) {
          List<Literal> literals = new ArrayList<>();
          for (int offsetI = 1; offsetI <= blockSize; offsetI++) {
            for (int offsetJ = 1; offsetJ <= blockSize; offsetJ++) {
              int i = blockI * blockSize + offsetI;
              int j = blockJ * blockSize + offsetJ;

              literals.add(cellLiteral(i, j, v));
            }
          }
          add(new Clause(literals));
        }
      }
    }
  }

  private Literal cellLiteral(int row, int collumn, int value) {
    return new Literal("c" + row + "" + collumn + "" + value);
  }

  private void cellWithValue(int row, int collumn, int value) {
    add(new Clause(List.of(cellLiteral(row, collumn, value))));
  }

  public static void print(Assignment assignment, int blockSize) {
    int n = blockSize * blockSize;

    for (int i = 1; i <= n; i++) {
      for (int j = 1; j <= n; j++) {
        for (int v = 1; v <= n; v++) {
          if (assignment.getValue("c" + i + j + v)) {
            System.out.print(v);
            if (j % blockSize == 0 && j != n) {
              System.out.print("     ");
            } else {
              System.out.print("  ");
            }
          }
        }
      }
      System.out.println();
      if (i % blockSize == 0 && i != n) {
        System.out.println();
      }
    }
  }
}
