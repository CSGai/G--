package main.java.gmm;

import main.java.gmm.ast.Expr;
import main.java.gmm.ast.Stmt;
import main.java.gmm.ast.Token;
import main.java.gmm.runtime.Interpreter;

import java.util.*;

class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;

    private class varStatus {
        final Token name; Boolean defined; Boolean used;
        varStatus(Token name, boolean defined, boolean used) {
            this.name = name;
            this.defined = defined;
            this.used = used;
        }
    };
    // binds name to resolve status in a scope, collected into a scope stack
    private final Stack<Map<String, varStatus>> scopes = new Stack<>();

    private enum ScopeType { NONE, FUNCTION, LAMBDA, LOOP }
    private ScopeType currentScope = ScopeType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    void resolve(List<Stmt> statements) {
        startScope();
        for (Stmt stmt : statements) resolve(stmt);
        endScope();
    }

    // expressions
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr arg : expr.arguments) resolve(arg);
        return null;
    }
    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        resolveFunction(expr, ScopeType.LAMBDA);
        return null;
    }
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return null;
    }
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
    @Override
    public Object visitPostfixExpr(Expr.Postfix expr) {
        resolve(expr.left);
        return null;
    }
    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        // unlike an if statement both branches are guaranteed
        resolve(expr.condition);
        resolve(expr.thenBranch);
        resolve(expr.elseBranch);
        return null;
    }
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty()) {
            varStatus status = scopes.peek().get(expr.name.lexeme);
            if (status != null && !status.defined) {
                Gmm.error(expr.name, "Can't read local variable in its own initializer.");
            }
        }
        resolveLocal(expr, expr.name);
        used(expr.name);
        return null;
    }

    // statements
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        startScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        ScopeType enclosingFunction = currentScope;
        currentScope = ScopeType.LOOP;
        resolve(stmt.condition);
        resolve(stmt.body);
        currentScope = enclosingFunction;
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (currentScope != ScopeType.LOOP) Gmm.error(stmt.self, "Can't break outside loop.");
        return null;
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (currentScope != ScopeType.LOOP) Gmm.error(stmt.self, "Can't continue outside loop.");
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentScope != ScopeType.FUNCTION && currentScope != ScopeType.LAMBDA) Gmm.error(stmt.keyword, "Can't return from top-level code.");
        if (stmt.value != null) resolve(stmt.value);
        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) resolve(stmt.initializer);
        define(stmt.name);
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, ScopeType.FUNCTION);
//        resolveFunction(stmt.params, stmt.body);
        return null;
    }
    @Deprecated
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }


    // visitor helpers
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    private void resolve(Expr expr) {
        expr.accept(this);
    }
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                // distance from current scope
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
    private void resolveFunction(Stmt.Function function, ScopeType type) {
        resolveFunction(function.params, function.body, type);
    }
    private void resolveFunction(Expr.Lambda lambda, ScopeType type) {
        if (lambda.body instanceof Stmt.Block body) resolveFunction(lambda.params, body.statements, type);
        else resolveFunction(lambda.params, List.of(lambda.body), type);
    }
    private void resolveFunction(List<Token> params, List<Stmt> body, ScopeType type) {
        ScopeType enclosingFunction = currentScope;
        currentScope = type;
        startScope();
        for (Token param : params) {
            declare(param);
            define(param);
        }
        resolve(body);
        endScope();
        currentScope = enclosingFunction;
    }

    // scope helpers
    private void startScope() {
        scopes.push(new HashMap<String, varStatus>());
    }
    private void endScope() {
        Map<String, varStatus> scope = scopes.peek();
        scope.forEach((name, status) -> {
            if (!status.used) Gmm.warning(status.name, "Variable never used.");
        });
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, varStatus> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) Gmm.error(name, "Variable already exists in scope.");
        scope.put(name.lexeme, new varStatus(name, false, false));
    }
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().get(name.lexeme).defined = true;
    }
    private void used(Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            varStatus status = scopes.get(i).get(name.lexeme);
            if (status != null) { status.used = true; return; }
            }
        }
}

