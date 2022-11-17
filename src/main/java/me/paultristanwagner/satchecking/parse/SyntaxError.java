package me.paultristanwagner.satchecking.parse;

public class SyntaxError extends Error {

    private final String input;
    private final int index;

    public SyntaxError( String input, int index ) {
        this.input = input;
        this.index = index;
    }

    public SyntaxError( String message, String input, int index ) {
        super( message );
        this.input = input;
        this.index = index;
    }

    public String getInput() {
        return input;
    }

    public int getIndex() {
        return index;
    }
}
