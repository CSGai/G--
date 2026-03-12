package main.java.gmm;

import main.java.gmm.ast.Expr;
import main.java.gmm.ast.Stmt;
import main.java.gmm.ast.Token;
import main.java.gmm.runtime.Interpreter;

import java.util.*;

class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    // binds name to resolve status in a scope, collected into a scope stack
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();


    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
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
        if (expr.body instanceof Stmt.Block body) resolveFunction(expr.params, body.statements);
        else resolveFunction(expr.params, List.of(expr.body));
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
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Gmm.error(expr.name, "Can't read local variable in its own initializer.");
        }
        resolveLocal(expr, expr.name);
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
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value != null) {
            resolve(stmt.value);
        }
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

        resolveFunction(stmt.params, stmt.body);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }


    // visitor helpers
    private void resolve(List<Stmt> statements) {
        for (Stmt stmt : statements) resolve(stmt);
    }
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
    private void resolveFunction(List<Token> params, List<Stmt> body) {
        startScope();
        for (Token param : params) {
            declare(param);
            define(param);
        }
        resolve(body);
        endScope();
    }

    // scope helpers
    private void startScope() {
        scopes.push(new HashMap<String, Boolean>());
    }
    private void endScope() {
        scopes.pop();
    }
    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> scope = scopes.peek();
        scope.put(name.lexeme, false);
    }
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

}
