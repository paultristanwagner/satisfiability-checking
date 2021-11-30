package me.paultristanwagner.satchecking;

public enum AnsiColor {
    
    RESET("\u001b[0m"),
    RED("\u001b[31m"),
    GREEN("\u001b[32;1m");
    
    private final String code;
    
    AnsiColor( String code ) {
        this.code = code;
    }
    
    @Override
    public String toString() {
        return code;
    }
}
