package main.java.gmm.runtime.callables;

import main.java.gmm.runtime.Interpreter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class NativeFunctions {
    public static final Map<String, GmmCallable> functions = new HashMap<>();
    static {
        functions.put("clock", new GmmCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }
            @Override
            public int arity() {return 0;}
            @Override
            public String toString() { return "<nativefn>";}
        });
        functions.put("input", new GmmCallable() {
            final Scanner scanner = new Scanner(System.in);
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {

                return scanner.nextLine();
            }

            @Override
            public int arity() {
                return 1;
            }
        });
        functions.put("hadpes", new GmmCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object arg = arguments.getFirst();
                if (arg instanceof Double darg) arg = darg.intValue();
                System.out.println(arg);
                return null;
            }

            @Override
            public int arity() {
                return 1;
            }
        });
    }

}
