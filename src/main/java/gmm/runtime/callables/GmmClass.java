package main.java.gmm.runtime.callables;

import main.java.gmm.runtime.Interpreter;

import java.util.List;
import java.util.Map;

public class GmmClass implements GmmCallable{
    final String name;
    private final Map<String, GmmFunction> methods;

    public GmmClass(String name, Map<String, GmmFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new GmmInstance(this);
    }

    GmmFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        return null;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
