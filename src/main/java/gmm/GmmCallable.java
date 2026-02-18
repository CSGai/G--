package main.java.gmm;

import java.util.List;

interface GmmCallable {
    Object call(Interpreter interpreter, List<Object> arguments);
    int arity();
}
