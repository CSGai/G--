package main.java.gmm.constructs;

public class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.line = line;
        this.literal = literal;
        this.lexeme = lexeme;
    }

    public String toString() {
        return String.format(
                "{lexeme: %-20s literal: %-20s type: %-20s line: %d}",
                raw(lexeme),
                literal instanceof String ? raw((String) literal) : String.valueOf(literal),
                type,
                line
        );
    }
    public static String raw(String s) {
        if (s == null) return "null";
        return s
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
