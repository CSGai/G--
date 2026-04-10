package main.java.gmm;

import main.java.gmm.ast.Expr;
import main.java.gmm.ast.Stmt;
import main.java.gmm.ast.TokenType;

import java.util.List;
import java.util.stream.Collectors;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    private int indent = 0;
    private static final String INDENT = "    ";

    // Printers
    String print(Stmt stmt) {
        return stmt.accept(this);
    }
    String print(Expr expr) {
        return expr.accept(this);
    }
    String printAll(List<Stmt> stmts) {
        return stmts.stream()
                .map(stmt -> stmt.accept(this))
                .collect(Collectors.joining("\n"));
    }

    // Expr
    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }
    @Override
    public String visitCallExpr(Expr.Call expr) {
        String args = expr.arguments.stream()
                .map(arg -> arg.accept(this))
                .collect(Collectors.joining(" "));
        return "( call " + expr.callee.accept(this)
                + ( args.isEmpty() ? "" : " " + args ) + " )";
    }
    @Override
    public String visitGetExpr(Expr.Get expr) {
        return parenthesize("[Get] " + expr.object.accept(this) + " " + expr.name.lexeme);
    }
    @Override
    public String visitSetExpr(Expr.Set expr) {
        return parenthesize("[Set] " + expr.object.accept(this) + " " + expr.name.lexeme);
    }
    @Override
    public String visitThisExpr(Expr.This expr) {
        return "[This]";
    }
    @Override
    public String visitLambdaExpr(Expr.Lambda expr) {
        String params = expr.params.stream()
                .map(t -> t.lexeme)
                .collect(Collectors.joining(", "));

        indent++;
        String body = expr.body.accept(this);
        indent--;

        return "( lambda ( " + params + " )\n"
                + pad() + INDENT + body + "\n"
                + pad() + ")";
    }
    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }
    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value == null ? "null" : expr.value.toString();
    }
    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }
    @Override
    public String visitPostfixExpr(Expr.Postfix expr) {
        return parenthesize(expr.operator.lexeme + " postfix", expr.left);
    }
    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return "( ternary "
                + expr.condition.accept(this) + " ? "
                + expr.thenBranch.accept(this) + " : "
                + expr.elseBranch.accept(this)
                + " )";
    }
    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }
    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    // Stmt
    @Override
    public String visitBlockStmt(Stmt.Block block) {
        StringBuilder builder = new StringBuilder();
        builder.append(pad()).append("[Block] {\n");
        indent++;

        for (Stmt stmt : block.statements) {
            builder.append(stmt.accept(this)).append("\n");
        }

        indent--;
        builder.append(pad()).append("}");
        return builder.toString();
    }
    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return pad() + "[Expr] " + stmt.expression.accept(this);
    }
    @Override
    public String visitIfStmt(Stmt.If stmt) {
        StringBuilder builder = new StringBuilder();

        builder.append(pad()).append("[If] ( ")
                .append(stmt.condition.accept(this))
                .append(" )\n");

        indent++;
        builder.append(stmt.thenBranch.accept(this));

        if (stmt.elseBranch != null) {
            builder.append("\n").append(pad()).append("[Else]\n");
            builder.append(stmt.elseBranch.accept(this));
        }
        indent--;

        return builder.toString();
    }
    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        StringBuilder builder = new StringBuilder();

        builder.append(pad()).append("[While] ( ")
                .append(stmt.condition.accept(this))
                .append(" )\n");

        indent++;
        builder.append(stmt.body.accept(this));
        indent--;

        return builder.toString();
    }
    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return pad() + "[Break]";
    }
    @Override
    public String visitContinueStmt(Stmt.Continue stmt) {
        return pad() + "[Continue]";
    }
    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        return pad() + ( stmt.value == null
                ? "[Return]"
                : "[Return] ( " + stmt.value.accept(this) + " )" );
    }
    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        String init = stmt.initializer != null
                ? " = " + stmt.initializer.accept(this)
                : "";
        return pad() + "[Variable] " + stmt.name.lexeme + init;
    }
    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        String params = stmt.params.stream()
                .map(t -> t.lexeme)
                .collect(Collectors.joining(", "));

        StringBuilder builder = new StringBuilder();
        builder.append(pad());
        builder.append("[");
        if (stmt.accessModifier != TokenType.NULL) builder.append(stmt.accessModifier).append("-");
        if (stmt.staticModifier != null) builder.append(stmt.staticModifier).append("-");
        builder.append("Function] ")
                .append(stmt.name.lexeme)
                .append(" ( ").append(params).append(" )\n");

        indent++;
        builder.append(new Stmt.Block(stmt.body).accept(this));
        indent--;

        return builder.toString();
    }

    @Override
    public String visitClassStmt(Stmt.Class stmt) {
        String methods = stmt.methods.stream()
                .map(t -> t.accept(this))
                .collect(Collectors.joining(", "));

        return pad() + "[Class] " +
                stmt.name.lexeme +
                " { \n" + pad() + methods + "\n}";
    }

    @Deprecated
    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return pad() + "[Print] " + stmt.expression.accept(this);
    }

    // Misc
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("( ").append(name);
        for (Expr expr : exprs) {
            builder.append(" ").append(expr.accept(this));
        }
        builder.append(" )");
        return builder.toString();
    }
    private String pad() {
        return INDENT.repeat(indent);
    }
}