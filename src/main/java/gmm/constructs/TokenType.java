package main.java.gmm.constructs;

public enum TokenType {
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, COLON, QUESTION,
    SLASH, STAR, PLUS_PLUS, MINUS_MINUS,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Assignment
    LEFT_ARROW, RIGHT_ARROW,

    // Literals.
    IDENTIFIER, STRING, NUMBER,

    // Keywords.
    AND, CLASS, ELSE, FALSE, FUNCTION, FOR, IF, NULL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, BREAK, CONTINUE,

    EOF
}
