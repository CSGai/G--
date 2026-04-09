package main.java.gmm.runtime.callables;

import main.java.gmm.runtime.Interpreter;

import java.util.List;
import java.util.Map;

public class GmmClass implements GmmCallable{
    public final String name;
    private final Map<String, GmmFunction> methods;

    public GmmClass(String name, Map<String, GmmFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        GmmInstance instance = new GmmInstance(this);
        GmmFunction initializer = findMethod("itchol");
        if (initializer != null) initializer.bind(instance).call(interpreter, arguments);
        return instance;
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
