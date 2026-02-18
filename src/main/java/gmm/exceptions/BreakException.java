package main.java.gmm.exceptions;

public class BreakException extends RuntimeException {
    public BreakException() {
        super(null, null, true, false);
    }
}
