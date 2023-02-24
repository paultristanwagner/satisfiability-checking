package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.EqualityConstraint;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class EqualityConstraintParser implements Parser<EqualityConstraint> {

  /* Grammar for equality constraints:
     S   -> V = V
         -> V != V
     V   -> VAR_NAME
         -> VAR_NAME NUMBER
  */

  @Override
  public EqualityConstraint parse(String string, AtomicInteger index) {
    String left = VAR(string, index);

    char c = Parser.nextProperChar(string, index);

    boolean equal = true;

    if (c == '!') {
      if (string.charAt(index.get()) != '=') {
        throw new SyntaxError("= expected", string, index.get());
      }
      index.incrementAndGet();
      equal = false;
    } else if (c != '=') {
      int lastIndex = index.get() - 1;
      throw new SyntaxError("= or != expected", string, lastIndex);
    }

    String right = VAR(string, index);

    return new EqualityConstraint(left, right, equal);
  }

  private static String VAR(String string, AtomicInteger index) {
    String result = VAR_NAME(string, index);
    int fallback = index.get();
    try {
      result += NUMBER(string, index);
    } catch (SyntaxError e) {
      index.set(fallback);
    }
    return result;
  }

  private static String VAR_NAME(String string, AtomicInteger index) {
    StringBuilder builder = new StringBuilder();
    while (index.get() < string.length()) {
      char character = Parser.nextProperChar(string, index);
      if ((character < '0' || character > '9')
          && (character < 'a' || character > 'z')
          && (character < 'A' || character > 'Z')
          && character != '_') {
        index.decrementAndGet();
        break;
      }

      builder.append(character);
    }

    if (builder.isEmpty()) {
      throw new SyntaxError("Variable expected", string, index.get());
    }

    return builder.toString();
  }

  private static String NUMBER(String string, AtomicInteger index) {
    StringBuilder builder = new StringBuilder();
    while (index.get() < string.length()) {
      char character = Parser.nextProperChar(string, index);
      if (character < '0' || character > '9') {
        index.decrementAndGet();
        break;
      }

      builder.append(character);
    }

    if (builder.isEmpty()) {
      throw new SyntaxError("Number expected", string, index.get());
    }
    return builder.toString();
  }
}
