package main.java.gmm.constructs;

import java.util.List;

public abstract class Expr {
  public interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
    R visitPostfixExpr(Postfix expr);
    R visitTernaryExpr(Ternary expr);
    R visitLogicalExpr(Logical expr);
    R visitVariableExpr(Variable expr);
  }
  public static class Assign extends Expr {
   public Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    public final Token name;
    public final Expr value;
  }
  public static class Binary extends Expr {
   public Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    public final Expr left;
    public final Token operator;
    public final Expr right;
  }
  public static class Call extends Expr {
   public Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    public final Expr callee;
    public final Token paren;
    public final List<Expr> arguments;
  }
  public static class Grouping extends Expr {
   public Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    public final Expr expression;
  }
  public static class Literal extends Expr {
   public Literal(Object value) {
      this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    public final Object value;
  }
  public static class Unary extends Expr {
   public Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    public final Token operator;
    public final Expr right;
  }
  public static class Postfix extends Expr {
   public Postfix(Expr left, Token operator) {
      this.left = left;
      this.operator = operator;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitPostfixExpr(this);
    }

    public final Expr left;
    public final Token operator;
  }
  public static class Ternary extends Expr {
   public Ternary(Expr condition, Expr thenBranch, Expr elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitTernaryExpr(this);
    }

    public final Expr condition;
    public final Expr thenBranch;
    public final Expr elseBranch;
  }
  public static class Logical extends Expr {
   public Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    public final Expr left;
    public final Token operator;
    public final Expr right;
  }
  public static class Variable extends Expr {
   public Variable(Token name) {
      this.name = name;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    public final Token name;
  }

  public abstract <R> R accept(Visitor<R> visitor);
}
