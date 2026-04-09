package main.java.gmm;

import main.java.gmm.ast.Expr;
import main.java.gmm.ast.Stmt;
import main.java.gmm.ast.Token;
import main.java.gmm.ast.TokenType;
import main.java.gmm.runtime.Interpreter;
import main.java.gmm.runtime.callables.NativeFunctions;

import java.util.*;


enum ScopeType {
    NONE,
    FUNCTION,
    LAMBDA,
    METHOD,
    INITIALIZER,
    PRIVATE
}
enum ClassType {
    NONE,
    CLASS
}
class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;

    private static class varStatus {
        final Token name; final ScopeType scope; Boolean defined; Boolean used;
        varStatus(Token name, ScopeType scope, boolean defined, boolean used) {
            this.name = name;
            this.scope = scope;
            this.defined = defined;
            this.used = used;
        }
    }
    // binds name to resolve status in a scope, collected into a scope stack
    private final Stack<Map<String, varStatus>> scopes = new Stack<>();
    private final Map<String, Map<String, Stmt.Function>> declerations = new HashMap<>();

    private int loopDepth = 0;
    private ClassType currentClass = ClassType.NONE;
    private ScopeType currentScopeType = ScopeType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    void resolve(List<Stmt> statements) {
        for (Stmt stmt : statements) resolve(stmt);
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
    public Object visitGetExpr(Expr.Get expr) {
        checkPrivateAccess(expr.name);
        resolve(expr.object);
        return null;
    }
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }
    @Override
    public Object visitThisExpr(Expr.This expr) {
        if (currentClass != ClassType.CLASS) Gmm.error(expr.keyword, "'this' not allowed outside class scope");
        if (currentScopeType != ScopeType.METHOD) Gmm.error(expr.keyword, "'this' not allowed outside method scope");
        resolveLocal(expr, expr.keyword);
        return null;
    }
    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        resolveFunction(expr);
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
                Gmm.error(expr.name, "Can't read local variable in its own initializer");
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
        resolveStmts(stmt.statements);
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
        loopDepth++;
        resolve(stmt.condition);
        resolve(stmt.body);
        loopDepth--;
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (loopDepth == 0) Gmm.error(stmt.self, "Can't break outside loop");
        return null;
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (loopDepth == 0) Gmm.error(stmt.self, "Can't continue outside loop");
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentScopeType == ScopeType.NONE) Gmm.error(stmt.keyword, "Can't return from top-level code");
        if (currentScopeType == ScopeType.INITIALIZER) Gmm.error(stmt.keyword, "Can't return a value from an initializer");
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
        if (currentClass == ClassType.NONE && stmt.accessModifier != TokenType.NULL)
            Gmm.error(stmt.name, "Access modifier used outside of class");

        resolveFunction(stmt, ScopeType.FUNCTION);
        return null;
    }
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);
        startScope();
        declerations.put(stmt.name.lexeme, new HashMap<>());
        scopes.peek().put("this", new varStatus(null, currentScopeType, true, false));
        for (Stmt.Function method : stmt.methods) {
            ScopeType declaration = ScopeType.METHOD;
            if (method.name.lexeme.equals("itchol")) declaration = ScopeType.INITIALIZER;
            else if (method.accessModifier == TokenType.PRIVATE) declaration = ScopeType.PRIVATE;
            declerations.get(stmt.name.lexeme).put(method.name.lexeme, method);
            resolveFunction(method, declaration);
        }
        endScope();

        currentClass = enclosingClass;
        return null;
    }
    @Deprecated
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }


    // visitor helpers
    private void resolveFunction(Stmt.Function function, ScopeType type) {
        resolveFunction(function.params, function.body, type);
    }
    private void resolveFunction(Expr.Lambda lambda) {
        if (lambda.body instanceof Stmt.Block body) resolveFunction(lambda.params, body.statements, ScopeType.LAMBDA);
        else resolveFunction(lambda.params, List.of(lambda.body), ScopeType.LAMBDA);
    }
    private void resolveFunction(List<Token> params, List<Stmt> body, ScopeType type) {
        ScopeType enclosingFunction = currentScopeType;
        currentScopeType = type;
        startScope();
        for (Token param : params) {
            declare(param);
            define(param);
        }
        resolveStmts(body);
        endScope();
        currentScopeType = enclosingFunction;
    }
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    private void resolve(Expr expr) {
        expr.accept(this);
    }
    private void resolveStmts(List<Stmt> statements) {
        for (Stmt stmt : statements) resolve(stmt);
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
    private void checkPrivateAccess(Token name) {

        Stmt.Function decl = declerations.values().stream()
                .map(m -> m.get(name.lexeme))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (decl == null) return;
        if (decl.accessModifier != TokenType.PRIVATE) return;
        Gmm.error(name, "Cannot call private method outside class");
    }

    // scope helpers
    private void startScope() {
        scopes.push(new HashMap<String, varStatus>());
    }
    private void endScope() {
        Map<String, varStatus> scope = scopes.peek();
        scope.forEach((name, status) -> {
            if (status.name != null && !status.used) Gmm.warning(status.name, "Variable never used");
        });
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, varStatus> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) Gmm.error(name, "Variable already exists in scope");
        scope.put(name.lexeme, new varStatus(name, currentScopeType, false, false));
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

