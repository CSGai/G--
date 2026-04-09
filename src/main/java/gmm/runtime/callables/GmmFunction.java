package main.java.gmm.runtime.callables;

import main.java.gmm.runtime.Environment;
import main.java.gmm.runtime.Interpreter;
import main.java.gmm.ast.Stmt;
import main.java.gmm.runtime.exceptions.Return;
import java.util.List;

public class GmmFunction implements GmmCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;
    public final boolean isGetter;

    public GmmFunction(Stmt.Function declaration, Environment closure, boolean isInitializer, boolean isGetter) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.isGetter = isGetter;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment local = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            local.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, local);
        }
        catch (Return returnValue) {
            if (isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }
    public GmmFunction bind(GmmInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new GmmFunction(declaration, environment, isInitializer, false);
    }
    public Object getAccessModifier() {
        return declaration.accessModifier;
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
