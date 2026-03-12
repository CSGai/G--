package main.java.gmm.runtime;

import main.java.gmm.ast.Token;
import main.java.gmm.errors.RuntimeError;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosingScope;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        enclosingScope = null;
    }
    public Environment(Environment enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    public void define(String name, Object value) {values.put(name, value);}

    public void assign(Token name, Object value) {
        // recursivly assigns value to name in the relevent scope
        if (values.containsKey(name.lexeme)) {values.put(name.lexeme, value); return;}
        if (enclosingScope!=null) {enclosingScope.assign(name, value); return;}

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    public void assignAt(Integer distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) return values.get(name.lexeme);
        if (enclosingScope!= null) return enclosingScope.get(name);

        throw new RuntimeError(name, "undefined variable '" + name.lexeme + "'.");
    }
    public Object getAt(Integer distance, String name) {
        return ancestor(distance).values.get(name);
    }

    private Environment ancestor(Integer distance) {
        Environment ancestorPointer = this;
        for (int i = 0; i < distance; i++) {
            assert ancestorPointer != null;
            ancestorPointer = ancestorPointer.enclosingScope;
        }
        return ancestorPointer;
    }
}
