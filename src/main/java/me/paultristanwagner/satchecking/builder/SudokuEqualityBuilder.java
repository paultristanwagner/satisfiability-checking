package me.paultristanwagner.satchecking.builder;

public class SudokuEqualityBuilder {

  private final int blockSize;
  private final int N;
  private final StringBuilder builder;

  public SudokuEqualityBuilder(int blockSize) {
    this.blockSize = blockSize;
    this.builder = new StringBuilder();
    this.N = blockSize * blockSize;
  }

  private String cell(int i, int j) {
    return "a" + i + "_" + j;
  }

  private String constant(int i) {
    if (i < 1 || i > N) {
      throw new IllegalArgumentException("Constant must be between 1 and " + N);
    }

    return "c" + i;
  }

  private void equal(String a, String b) {
    builder.append("[").append(a).append("=").append(b).append("]");
  }

  private void notEqual(String a, String b) {
    builder.append("[").append(a).append("!=").append(b).append("]");
  }

  public String build() {
    /*
       Define constants. which differ pairwise.
    */
    for (int i = 1; i <= N; i++) {
      for (int j = i + 1; j <= N; j++) {
        builder.append(" & (");
        notEqual(constant(i), constant(j));
        builder.append(")");
      }
    }

    // Every cell has a value
    for (int i = 1; i <= N; i++) {
      for (int j = 1; j <= N; j++) {
        builder.append(" & (");
        for (int k = 1; k <= N; k++) {
          if (k != 1) {
            builder.append(" | ");
          }
          equal(cell(i, j), constant(k));
        }
        builder.append(")");
      }
    }

    // No two cells in a row have the same value
    for (int i = 1; i <= N; i++) {
      for (int j1 = 1; j1 <= N; j1++) {
        for (int j2 = j1 + 1; j2 <= N; j2++) {
          builder.append(" & (");
          notEqual(cell(i, j1), cell(i, j2));
          builder.append(")");
        }
      }
    }

    // No two cells in a column have the same value
    for (int j = 1; j <= N; j++) {
      for (int i1 = 1; i1 <= N; i1++) {
        for (int i2 = i1 + 1; i2 <= N; i2++) {
          builder.append(" & (");
          notEqual(cell(i1, j), cell(i2, j));
          builder.append(")");
        }
      }
    }

    // No two cells in a block have the same value
    for (int i = 1; i <= N; i += blockSize) {
      for (int j = 1; j <= N; j += blockSize) {
        for (int i1 = i; i1 < i + blockSize; i1++) {
          for (int j1 = j; j1 < j + blockSize; j1++) {
            for (int i2 = i; i2 < i + blockSize; i2++) {
              for (int j2 = j; j2 < j + blockSize; j2++) {
                if (i1 == i2 && j1 == j2) {
                  continue;
                }

                builder.append(" & (");
                notEqual(cell(i1, j1), cell(i2, j2));
                builder.append(")");
              }
            }
          }
        }
      }
    }

    return builder.substring(3);
  }
}
