package me.paultristanwagner.satchecking.parse;

public record ParseResult<T>(T result, int charsRead, boolean complete) {

}
