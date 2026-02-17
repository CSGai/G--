//package main.java.gmm;
//
//class AstChallenge implements Expr.Visitor<String> {
//
//    public static void main(String[] args) {
//        Expr expression = new Expr.Binary(
//                // 1+2
//                new Expr.Binary(
//                        new Expr.Literal(1),
//                        new Token(TokenType.MINUS, "+", null, 1),
//                        new Expr.Literal(2)),
//                // multiplication
//                new Token(TokenType.STAR, "*", null, 1),
//                // 4-3
//                new Expr.Binary(
//                        new Expr.Literal(4),
//                        new Token(TokenType.MINUS, "-", null, 1),
//                        new Expr.Literal(3))
//                );
//
//        System.out.println(new AstChallenge().printRPN(expression));
//    }
//
//    String printRPN(Expr expr) {
//        return expr.accept(this);
//    }
//
//    @Override
//    public String visitAssignExpr(Expr.Assign expr) {
//        return "";
//    }
//
//    @Override
//    public String visitBinaryExpr(Expr.Binary expr) {
//        return  String.format(
//            "%s %s %s",
//            expr.left.accept(this),
//            expr.right.accept(this),
//            expr.operator.lexeme
//        );
//    }
//
//    @Override
//    public String visitGroupingExpr(Expr.Grouping expr) {
//        return parenthesize("group", expr.expression);
//    }
//
//    @Override
//    public String visitLiteralExpr(Expr.Literal expr) {
//        if (expr.value == null) return "null";
//        return expr.value.toString();
//    }
//
//    @Override
//    public String visitUnaryExpr(Expr.Unary expr) {
//        return parenthesize(expr.operator.lexeme, expr.right);
//    }
//
//    @Override
//    public String visitTernaryExpr(Expr.Ternary expr) {
//        return parenthesize("if",expr.condition)
//                + parenthesize("then", expr.thenBranch)
//                + parenthesize("else", expr.elseBranch);
//    }
//
//    @Override
//    public String visitVariableExpr(Expr.Variable expr) {
//        return expr.name.lexeme;
//    }
//
//
//    private String parenthesize(String lexeme, Expr... exprs) {
//        StringBuilder builder = new StringBuilder();
//
//        for ( Expr expr : exprs ) {
//            builder.append(" ");
//            builder.append(expr.accept(this));
//        }
//
//        return builder.toString();
//    }
//}
