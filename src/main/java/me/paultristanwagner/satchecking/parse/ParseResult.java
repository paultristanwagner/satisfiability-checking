package me.paultristanwagner.satchecking.parse;

public record ParseResult<T>(T result, String remaining) {

  public boolean isComplete() {
    return remaining.isEmpty();
  }
}
