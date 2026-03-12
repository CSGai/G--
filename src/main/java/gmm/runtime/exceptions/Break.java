package main.java.gmm.runtime.exceptions;

import main.java.gmm.ast.Token;

public class Break extends RuntimeException {
    public final Token self;
    public Break(Token self) {
        super(null, null, false, false);
        this.self = self;
    }
}
