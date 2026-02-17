package main.java.gmm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static main.java.gmm.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    final private List<Token> tokens;
    private int current_idx = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!endOfFile()) {
            statements.add(declaration());
        }
        return statements;
    }

    /* -- Heiarchy -- */
    // statements
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        }
        catch (ParseError error) {
            synchronize();
            return null;
        }

    }
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(WHILE)) return whileStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }
    private Stmt forStatement() {
        Stmt initilizer;
        if (match(VAR)) initilizer = varDeclaration();
        else initilizer = expressionStatement();

        Stmt endCondition = null;
        if ()
    }
    private Stmt whileStatement() {
        Expr condition = expression();
        consume(RIGHT_ARROW, "Expect '->' after condition");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }
    private Stmt ifStatement() {
        Expr condition = expression();
        consume(RIGHT_ARROW, "Expect '->' after condition");
        Stmt thenBranch = statement();

        Stmt elseBranch = null;
        if (match(ELSE)) elseBranch = statement();

        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ; after statement");
        return new Stmt.Print(value);
    }
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }
    private List<Stmt> block() {
        List<Stmt> statments = new ArrayList<>();

        while(!check(RIGHT_BRACE) && !endOfFile()) statments.add(declaration());

        consume(RIGHT_BRACE, "Expected } at end of block.");
        return statments;
    }
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected identifier after declaration keyword");

        Expr initializer = null;
        if (match(LEFT_ARROW)) initializer = expression();

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    // expressions
    private Expr expression() {
        return sequence();
    }
    private Expr sequence() {
        Expr lExpr = checkMissingLHO(this::assignment, COMMA);

        while (match(COMMA)) {
            Token operator = previous();
            Expr rExpr = sequence();
            lExpr = new Expr.Binary(lExpr, operator, rExpr);
        }
        return lExpr;
    }
    private Expr assignment() {
        Expr expr = checkMissingLHO(this::or, LEFT_ARROW);

        if (match(LEFT_ARROW)) {
            Token assignSymbol = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(assignSymbol, "Invalid assignment target.");
        }
        return expr;
    }
    private Expr or() {
        Expr expr = checkMissingLHO(this::and, OR);

        if (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr and() {
        Expr expr = checkMissingLHO(this::ternary, AND);

        if (match(AND)) {
            Token operator = previous();
            Expr right = ternary();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr ternary() {
        Expr expr = checkMissingLHO(this::equality, QUESTION);

        if (match(QUESTION)) {
            Expr left = expression();
            consume(COLON, "Expect : after 'then' branch of ternary");
            Expr right = ternary();
            expr = new Expr.Ternary(expr, left, right);
        }
        return expr;
    }
    private Expr equality() {
        TokenType[] ops = {BANG_EQUAL, EQUAL_EQUAL};
        Expr expr = checkMissingLHO(this::comparison, ops);

        while (match(ops)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr comparison() {
        TokenType[] ops = {LESS, GREATER, LESS_EQUAL, GREATER_EQUAL};
        Expr expr = checkMissingLHO(this::term, ops);

        while (match(ops)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr term() {
        TokenType[] ops = {PLUS, MINUS};
        Expr expr = checkMissingLHO(this::factor, ops);

        while (match(ops)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr factor() {
        TokenType[] ops = {SLASH, STAR};
        Expr expr = checkMissingLHO(this::unary, ops);

        while (match(ops)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    private Expr unary() {
        TokenType[] ops = {MINUS, BANG};
        if (match(ops)) {
            Token operator = previous();
            Expr expr = unary();
            return new Expr.Unary(operator, expr);
        }
        return primary();
    }
    private Expr primary() {
        Token token = peek();

        switch (token.type) {
            case TRUE: advance(); return new Expr.Literal(true);
            case FALSE: advance(); return new Expr.Literal(false);
            case NULL: advance(); return new Expr.Literal(null);
            case NUMBER, STRING: advance(); return new Expr.Literal(token.literal);
            case IDENTIFIER: advance(); return new Expr.Variable(previous());
            case LEFT_PAREN:
                advance();
                Expr expr = expression();
                consume(RIGHT_PAREN, "Expect ')' after expression");
                return new Expr.Grouping(expr);
            default:
                throw error(peek(), "Expected expression.");
        }
    }

    // consume
    private Token consume(TokenType endToken, String message) {
        if (!endOfFile() && endToken == peek().type) return advance();
        throw error(peek(), message);
    }
    private Token advance() {
        if (!endOfFile()) current_idx++;
        return previous();
    }

    // non-consume
    private Token peek() {
        return tokens.get(current_idx);
    }
    private Token previous() {
        return tokens.get(current_idx - 1);
    }
    private boolean check(TokenType type) {
        if (endOfFile()) return false;
        return peek().type == type;
    }

    // special methods
    private boolean endOfFile() {
        return peek().type == EOF;
    }
    private Expr checkMissingLHO(Supplier<Expr> nextInHierarchy, TokenType... types) {
        if (match(types)) error(previous(), "Missing left-hand operand");
        return nextInHierarchy.get();
    }

    private boolean match(TokenType... targets) {
        if (endOfFile()) return false;

        boolean result = false;
        Token current_token = tokens.get(current_idx);

        for (TokenType type : targets) {
            result = type == current_token.type;
            if (result) break;
        }
        if (result) current_idx++;
        return result;
    }

    // error handeling
    private ParseError error(Token token, String message) {
        Gmm.error(token, message);
        return new ParseError();
    }
    private void synchronize() {
        advance();

        while (!endOfFile()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUNCTION:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}
