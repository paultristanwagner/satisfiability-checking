package me.paultristanwagner.satchecking.parse;

import java.util.concurrent.atomic.AtomicInteger;

public interface Parser<T> {

  default T parse(String string) {
    return parseWithRemaining(string).result();
  }

  ParseResult<T> parseWithRemaining(String string);

  static char nextProperChar(String string, AtomicInteger index) {
    while (index.get() < string.length()) {
      char character = string.charAt(index.get());
      index.incrementAndGet();

      if (character != ' ' && character != '\n') {
        return character;
      }
    }

    index.incrementAndGet();
    return 0;
  }
}
