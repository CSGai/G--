package main.java.gmm.exceptions;

public class ContinueException extends RuntimeException {
    public ContinueException() {
        super(null, null, true, false);
    }
}

