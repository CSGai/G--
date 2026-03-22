package main.java.gmm;

import main.java.gmm.ast.Token;
import main.java.gmm.ast.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;
import static main.java.gmm.ast.TokenType.*;

class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    Lexer(String source) {
        this.source = source;
    }

    private int lex_start_idx = 0 ;
    private int current_idx = 0;
    private int line = 0;
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("veh", AND);
        keywords.put("oh", OR);
        keywords.put("kita", CLASS);
        keywords.put("zeh", THIS);
        keywords.put("eim", IF);
        keywords.put("acheret", ELSE);
        keywords.put("lo-nachon", FALSE);
        keywords.put("nachon", TRUE);
        keywords.put("zilch", NULL);
        keywords.put("kef", FUNCTION);
//        keywords.put("hadpes", PRINT); @Deprecated
        keywords.put("hachzer", RETURN);
        keywords.put("super", SUPER);
        keywords.put("mish", VAR); //mishtaneh
        keywords.put("bezman", WHILE);
        keywords.put("leh", FOR);
        keywords.put("shbor", BREAK);
        keywords.put("daleg", CONTINUE);
    }

    List<Token> scanTokens() {
        while (!endOfFile()) {
            lex_start_idx = current_idx;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            // general
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case ';': addToken(SEMICOLON); break;
            case ':': addToken(COLON); break;
            // boolean
            case '?': addToken(QUESTION); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            // assignment
            case '<': addToken(match('=') ? LESS_EQUAL : match('-')? LEFT_ARROW : LESS); break;
            case '-': addToken(match('>') ? RIGHT_ARROW : match('-') ? MINUS_MINUS : MINUS); break;
            // math
            case '*': addToken(STAR); break;
            case '+': addToken(match('+') ? PLUS_PLUS : PLUS); break;
            // special handling for comments
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !endOfFile()) advance();
                }
                else if (match('*')) {
                    while (peek() != '*' && peekFuture() != '/' && !endOfFile()) advance();
                    match('*');
                    match('/');
                }
                else {
                    addToken(SLASH);
                }
                break;

            // escape characters
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            // new line
            case '\n':
                line++;
                break;

            // string handling
            case '"': string(); break;

            default:
                if (isDigit(c)) number();
                else if (isAlphabetic(c)) identifier();
                else Gmm.error(line, "Unexpected characer: " + c);
                break;
        }
    }


    // LITERALS
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String lexeme = getLexeme();
        TokenType type = keywords.get(lexeme);
        if (type == null) addToken(IDENTIFIER);
        else addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peekFuture() == '.' && isDigit(peek())) {
            do advance();
            while (isDigit(peek()));
        }
        addToken(NUMBER, Double.parseDouble(getLexeme()));
    }

    private void string() {
        while (peek() != '"' && !endOfFile()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (endOfFile()) {
            Gmm.error(line, "Unterminated String from lines");
            return;
        }
        advance();
        addToken(STRING, source.substring(lex_start_idx+1, current_idx-1));
    }

    // HELPER FUNCTIONS

    // consume
    private boolean match(char c) {
        if (endOfFile()) return false;
        if (peek() != c) return false;
        current_idx++;
        return true;
    }

    char advance() {
        current_idx += 1;
        return source.charAt(current_idx - 1);
    }

    // non-consume
    private char peek() {
        if (endOfFile()) return '\0';
        return source.charAt(current_idx);
    }
    private char peekFuture() {
        if (current_idx + 1 >= source.length()) return '\0';
        return source.charAt(current_idx + 1);
    }

    // tokens
    void addToken(TokenType type) {
        addToken(type, null);
    }
    void addToken(TokenType type, Object literal) {
        tokens.add(new Token(type, getLexeme(), literal, line));
    }

    // misc
    private boolean endOfFile() {
        return current_idx >= source.length();
    }
    private boolean isAlphaNumeric(char c) {
        return isAlphabetic(c) || isDigit(c);
    }
    private String getLexeme() {
        return source.substring(lex_start_idx, current_idx);
    }

}
