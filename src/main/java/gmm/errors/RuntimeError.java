package main.java.gmm.errors;

import main.java.gmm.ast.Token;

public class RuntimeError extends RuntimeException{
    public final Token token;

    public RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
