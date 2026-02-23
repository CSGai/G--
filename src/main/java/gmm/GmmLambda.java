package main.java.gmm;

import main.java.gmm.constructs.Expr;
import main.java.gmm.exceptions.Return;

import java.util.List;

class GmmLambda implements GmmCallable {
    private final Expr.Lambda declaration;
    private final Environment closure;

    GmmLambda(Expr.Lambda declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment local = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            local.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.execute(declaration.body, local);
        }
        catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }
    @Override
    public String toString() {
        return "<fn lambda >";
    }
}
