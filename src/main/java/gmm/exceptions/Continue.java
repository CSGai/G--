package main.java.gmm.exceptions;

import main.java.gmm.Token;

public class Continue extends RuntimeException {
    public final Token self;
    public Continue(Token self) {
        super(null, null, false, false);
        this.self = self;
    }
}

