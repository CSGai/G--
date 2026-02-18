package main.java.gmm;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosingScope;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosingScope = null;
    }
    Environment(Environment enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    void define(String name, Object value) {values.put(name, value);}
    void undefine(String name, Object value) {values.remove(name, value);}
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {values.put(name.lexeme, value); return;}
        if (enclosingScope!=null) {enclosingScope.assign(name, value); return;}

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) return values.get(name.lexeme);
        if (enclosingScope!= null) return enclosingScope.get(name);

        throw new RuntimeError(name, "undefined variable '" + name.lexeme + "'.");
    }
}
