package main.java.gmm.runtime.callables;

import main.java.gmm.ast.Token;
import main.java.gmm.errors.RuntimeError;

import java.util.HashMap;
import java.util.Map;

public class GmmInstance {
    final GmmClass kita;
    private final Map<String, Object> fields = new HashMap<>();

    GmmInstance(GmmClass kita) {
        this.kita = kita;
    }

    @Override
    public String toString() {
        return kita.name + " instance";
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme)) return fields.get(name.lexeme);
        GmmFunction method = kita.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }
    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}
