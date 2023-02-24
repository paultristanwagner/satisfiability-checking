package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EqualityFunctionParser implements Parser<EqualityFunctionConstraint> {

  /* Grammar for equality function constraints:
     S   -> F = F
         -> F != F
     F   -> F_NAME
         -> F_NAME ( F )
         -> F_NAME ( F, ... )
     F_NAME -> VAR_NAME
            -> VAR_NAME NUMBER
  */

  @Override
  public EqualityFunctionConstraint parse(String string) {
    return parse(string, new AtomicInteger());
  }

  @Override
  public EqualityFunctionConstraint parse(String string, AtomicInteger index) {
    Function left = FUNCTION(string, index);

    char c = Parser.nextProperChar(string, index);

    boolean equal = true;

    if (c == '!') {
      if (string.charAt(index.get()) != '=') {
        throw new SyntaxError("= expected", string, index.get());
      }
      index.incrementAndGet();
      equal = false;
    } else if (c != '=') {
      throw new SyntaxError("= or != expected", string, index.get());
    }

    Function right = FUNCTION(string, index);

    return new EqualityFunctionConstraint(left, right, equal);
  }

  private static Function FUNCTION(String string, AtomicInteger index) {
    String functionName = VAR(string, index);
    List<Function> parameters = new ArrayList<>();

    char c = Parser.nextProperChar(string, index);
    if (c == '(') {
      parameters.add(FUNCTION(string, index));
      c = Parser.nextProperChar(string, index);
      while (c == ',') {
        parameters.add(FUNCTION(string, index));
        c = Parser.nextProperChar(string, index);
      }
      if (c != ')') {
        int lastIndex = index.get() - 1;
        throw new SyntaxError(") expected", string, lastIndex);
      }
    } else {
      index.decrementAndGet();
    }

    return Function.of(functionName, parameters);
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
