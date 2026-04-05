package main.java.gmm.runtime.callables;

import main.java.gmm.runtime.Interpreter;

import java.util.List;

public class GmmClass implements GmmCallable{
    final String name;

    public GmmClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new GmmInstance(this);
    }

    @Override
    public int arity() {
        return 0;
    }
}
