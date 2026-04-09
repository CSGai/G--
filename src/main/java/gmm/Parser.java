package main.java.gmm;

import main.java.gmm.ast.Expr;
import main.java.gmm.ast.Stmt;
import main.java.gmm.ast.Token;
import main.java.gmm.ast.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static main.java.gmm.ast.TokenType.*;

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
    // declerations
    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDecleration();
            if (match(FUNCTION)) return function("function", PUBLIC);
            if (match(VAR)) return varDeclaration();
            return statement();
        }
        catch (ParseError error) {
            synchronize();
            return null;
        }
    }
    private Stmt classDecleration() {
        Token name = consume(IDENTIFIER, "Expected class name after declaration keyword");
        consume(LEFT_BRACE, "Expected '{' before class body");
        List<Stmt.Function> methods = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !endOfFile()) {
            methods.add(getterMethod());
        }
        consume(RIGHT_BRACE, "Expected '}' after class body");

        return new Stmt.Class(name, methods);
    }
    private Stmt.Function getterMethod() {
        TokenType accessMod = PUBLIC;
        if (match(PRIVATE, PUBLIC)) accessMod = previous().type;

        if (tokens.get(current_idx+1).type == LEFT_ARROW) {
            Token name = consume(IDENTIFIER, "Expected getter method name");
            consume(LEFT_ARROW, "Expected '<-' after getter method name");
            consume(LEFT_BRACE, "Expected '{' before getter method body");
            List<Stmt> body = block();
            return new Stmt.Function(name, new ArrayList<>(), body, true, accessMod);
        }

        return function("method", accessMod);
    }
    private Stmt.Function function(String kind, TokenType accessMod) {
        Token name = consume(IDENTIFIER, "Expected " + kind + " name");

        consume(COLON, "Expected ':' after " + kind + " name");
        List<Token> params = new ArrayList<>();

        if (!check(RIGHT_ARROW)) {
            do {
                if (params.size() >= 255) error(peek(), "Can't have more than 255 params");
                params.add(consume(IDENTIFIER, "Expected param name"));
            }
            while (match(COMMA));
        }
        Token arrow = consume(RIGHT_ARROW, "Expected '->' after arguments");

        consume(LEFT_BRACE, "Expected '{' before " + kind + " body");
        List<Stmt> body = block();

        return new Stmt.Function(name, params, body, false, accessMod);
    }
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected identifier after declaration keyword");

        Expr initializer = null;
        if (match(LEFT_ARROW)) initializer = expression();

        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }


    //statements
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(IF)) return ifStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(BREAK)) return breakStatement();
        if (match(CONTINUE)) return continueStatement();
//        if (match(PRINT)) return printStatement();
        return expressionStatement();
    }
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expr);
    }
    private Stmt returnStatement() {
        Token keyword = previous();

        Expr value = null;
        if(!check(SEMICOLON)) value = expression();
        consume(SEMICOLON, "Expected ';' after return value");

        return new Stmt.Return(keyword, value);
    }
    private Stmt forStatement() {
        Stmt initializer;
        if (match(SEMICOLON)) initializer = null;
        else if (match(VAR)) initializer = varDeclaration();
        else initializer = expressionStatement();

        Expr condition = null;
        if (!check(SEMICOLON)) condition = expression();
        consume(SEMICOLON, "Expect ';' after loop condition");

        Expr increment = null;
        if (!check(RIGHT_ARROW)) increment = expression();
        consume(RIGHT_ARROW, "Expected '->' after loop clause");
        Stmt body = statement();

        if (increment != null) body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) body = new Stmt.Block(Arrays.asList(initializer, body));


        return body;
    }
    private Stmt whileStatement() {
        Expr condition = expression();
        consume(RIGHT_ARROW, "Expect '->' after condition");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }
    private Stmt breakStatement() {
        Token self = previous();
        consume(SEMICOLON, "Expected ';' after break statement");
        return new Stmt.Break(self);
    }
    private Stmt continueStatement() {
        Token self = previous();
        consume(SEMICOLON, "Expected ';' after break statement");
        return new Stmt.Continue(self);
    }
    private Stmt ifStatement() {
        Expr condition = expression();
        consume(RIGHT_ARROW, "Expect '->' after condition");
        Stmt thenBranch = statement();

        Stmt elseBranch = null;
        if (match(ELSE)) elseBranch = statement();

        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private List<Stmt> block() {
        List<Stmt> statments = new ArrayList<>();

        while(!check(RIGHT_BRACE) && !endOfFile()) statments.add(declaration());

        consume(RIGHT_BRACE, "Expected } at end of block");
        return statments;
    }
    @Deprecated
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ; after statement");
        return new Stmt.Print(value);
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

            if (expr instanceof Expr.Variable variable) {
                Token name = (variable).name;
                return new Expr.Assign(name, value);
            }
            else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.name, get.object , value);
            }
            error(assignSymbol, "Invalid assignment target");
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
        Expr expr = checkMissingLHO(this::factor, PLUS);

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
        if (match(MINUS, BANG)) {
            Token operator = previous();
            Expr expr = unary();
            return new Expr.Unary(operator, expr);
        }
        return postfix();
    }
    private Expr postfix() {
        Expr expr = call();
        if (match(MINUS_MINUS, PLUS_PLUS)) {
            Token operator = previous();
            return new Expr.Postfix(expr, operator);
        }
        return expr;
    }
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) expr = finishCall(expr);
            else if (match(COLON_COLON)) {
            Token name = consume(IDENTIFIER, "Expected property name after '::'");

            expr = new Expr.Get(name, expr);
            }

            else break;
        }

        return expr;
    }
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) error(peek(), "Can't have more than 255 arguments");
                arguments.add(assignment());
            }
            while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments");
        return new Expr.Call(callee, paren, arguments);

    }
    private Expr primary() {
        Token token = advance();

        switch (token.type) {
            case TRUE: return new Expr.Literal(true);
            case FALSE: return new Expr.Literal(false);
            case NULL: return new Expr.Literal(null);
            case NUMBER, STRING: return new Expr.Literal(token.literal);
            case IDENTIFIER: return new Expr.Variable(previous());
            case THIS: return new Expr.This(previous());
            case FUNCTION:
                consume(COLON, "Expected ':' after lambda");
                List<Token> params = new ArrayList<>();
                if (!check(RIGHT_ARROW)) {
                    do {
                        if (params.size() >= 255) error(peek(), "Can't have more than 255 params");
                        params.add(consume(IDENTIFIER, "Expected param name"));
                    }
                    while (match(COMMA));
                }
                consume(RIGHT_ARROW, "Expected '->' after arguments");
                return new Expr.Lambda(params, statement());
            case LEFT_PAREN:
                Expr expr = expression();
                consume(RIGHT_PAREN, "Expected ')' after expression");
                return new Expr.Grouping(expr);
            default:
                throw error(peek(), "Expected expression");
        }
    }

    // consume

    /**
     * Advances by one token if the current token matches the given endToken.
     *
     * @param endToken the token to match against the current token
     * @param message error message
     * @return the current token
     */
    private Token consume(TokenType endToken, String message) {
        if (!endOfFile() && endToken == peek().type) return advance();
        throw error(peek(), message);
    }
    /**
     * Advances by one token
     * @return the current token
     */
    private Token advance() {
        if (!endOfFile()) current_idx++;
        return previous();
    }

    // non-consume
    private boolean check(TokenType type) {
        if (endOfFile()) return false;
        return peek().type == type;
    }
    private boolean match(TokenType... targets) {
        if (endOfFile()) return false;

        boolean result = false;
        Token current_token = peek();

        for (TokenType type : targets) {
            result = type == current_token.type;
            if (result) break;
        }
        if (result) current_idx++;
        return result;
    }
    private Token peek() {
        return tokens.get(current_idx);
    }
    private Token previous() {
        return tokens.get(current_idx - 1);
    }

    // special methods
    private boolean endOfFile() {
        return peek().type == EOF;
    }
    private Expr checkMissingLHO(Supplier<Expr> nextInHierarchy, TokenType... types) {
        if (match(types)) error(previous(), "Missing left-hand operand");
        return nextInHierarchy.get();
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
//                case PRINT: @Deprecated
                case RETURN:
                    return;
            }
            advance();
        }
    }
}
