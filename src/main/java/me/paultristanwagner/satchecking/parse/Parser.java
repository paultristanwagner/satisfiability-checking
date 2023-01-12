package me.paultristanwagner.satchecking.parse;

import java.util.concurrent.atomic.AtomicInteger;

public interface Parser<T> {

  default T parse(String string) {
    return parse(string, new AtomicInteger());
  }

  T parse(String string, AtomicInteger index);

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

  static void printPointer(int index) {
    for (int i = 0; i < index; i++) {
      System.out.print(" ");
    }
    System.out.println("^");
  }
}
