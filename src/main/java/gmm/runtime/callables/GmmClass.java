package main.java.gmm.runtime.callables;

import main.java.gmm.ast.Token;
import main.java.gmm.errors.RuntimeError;
import main.java.gmm.runtime.Interpreter;

import java.util.List;
import java.util.Map;

public class GmmClass extends GmmInstance implements GmmCallable{
    public final String name;
    private final Map<String, GmmFunction> methods;

    public GmmClass(String name, Map<String, GmmFunction> methods, Map<Token, GmmFunction> staticMethods) {
        super(null);
        this.name = name;
        this.methods = methods;
        if (staticMethods == null) return;
        staticMethods.forEach(super::set);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        GmmInstance instance = new GmmInstance(this);
        GmmFunction initializer = findMethod("itchol");
        if (initializer != null) initializer.bind(instance).call(interpreter, arguments);
        return instance;
    }
    @Override
    public Object get(Token name) {
        GmmFunction method = findMethod(name.lexeme);
        if (method != null) return method;
        if (fields.containsKey(name.lexeme)) return fields.get(name.lexeme);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme);
    }

    @Override
    public int arity() {
        GmmFunction initializer = findMethod("itchol");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    public GmmFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
