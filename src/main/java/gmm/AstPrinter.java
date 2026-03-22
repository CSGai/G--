package main.java.gmm;

import main.java.gmm.ast.Expr;
import main.java.gmm.ast.Stmt;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    String print(Stmt stmt) {
        return stmt.accept(this);
    }
    String print(Expr expr) {
        return expr.accept(this);
    }

    // Expr Printer
    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize(expr.name + "<-" + expr.value);
    }
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }
    @Override
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize(expr.callee.toString(), expr.callee);
    }
    @Override
    public String visitLambdaExpr(Expr.Lambda expr) {
        return parenthesize("Lambda", expr);
    }
    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }
    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "null";
        return expr.value.toString();
    }
    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }
    @Override
    public String visitPostfixExpr(Expr.Postfix expr) {
        return parenthesize(expr.operator.lexeme, expr.left);
    }
    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return "("
                    + parenthesize("if",expr.condition)
                    + parenthesize("then", expr.thenBranch)
                    + parenthesize("else", expr.elseBranch) +
                ")";
    }
    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }
    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    private String parenthesize(String lexeme, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append(" ( ").append(lexeme);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(" ) ");

        return builder.toString();
    }

    // Stmt Printer
    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        return "";
    }
    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return "";
    }
    @Override
    public String visitIfStmt(Stmt.If stmt) {
        return "";
    }
    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return "";
    }
    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return "";
    }
    @Override
    public String visitContinueStmt(Stmt.Continue stmt) {
        return "";
    }
    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        return "";
    }
    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return "";
    }
    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        return "";
    }
    @Deprecated
    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return "";
    }

//    private Stmt destructur(Stmt stmt) {
//
//    }
}