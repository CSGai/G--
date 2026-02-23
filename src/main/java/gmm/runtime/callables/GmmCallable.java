package main.java.gmm.runtime.callables;

import main.java.gmm.runtime.Interpreter;

import java.util.List;

public interface GmmCallable {
    Object call(Interpreter interpreter, List<Object> arguments);
    int arity();
}
