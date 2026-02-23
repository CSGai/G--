package main.java.gmm.exceptions;

import main.java.gmm.constructs.Token;

public class Break extends RuntimeException {
    public final Token self;
    public Break(Token self) {
        super(null, null, false, false);
        this.self = self;
    }
}
