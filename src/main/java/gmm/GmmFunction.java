package main.java.gmm;

import java.util.List;

class GmmFunction implements GmmCallable {
    private final Stmt.Function declaration;
    GmmFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment local = new Environment(interpreter.globals);
        for (int i = 0; i < declaration.params.size(); i++) {
            local.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        interpreter.executeBlock(declaration.body, local);
        return null;
    }
    @Override
    public int arity() {
        return declaration.params.size();
    }
    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
