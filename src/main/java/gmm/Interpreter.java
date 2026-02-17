package main.java.gmm;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    Enviroment enviroment = new Enviroment();

    void interpret(List<Stmt> statments) {
        try {
            for ( Stmt statement : statments) {
                execute(statement);
            }
        }
        catch (RuntimeError error) {
            Gmm.runtimeError(error);
        }
    }

    // Expression visitors
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return eval(expr.expression);
    }
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = eval(expr.right);
        return switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield -(double) right;
            }
            case BANG -> !Truthful(right);
            default -> null;
        };
    }
    @Override
    public Object visitPostfixExpr(Expr.Postfix expr) {
        Object left = eval(expr.left);
        if (!(left instanceof Double)) {
            throw new RuntimeError(expr.operator, "Operand must be a number.");
        }

        double result = switch (expr.operator.type) {
            case PLUS_PLUS -> (double) left + 1;
            case MINUS_MINUS -> (double) left - 1;
            default -> throw new RuntimeError(expr.operator, "Ilegal operator for Postfix.");
        };

        if (expr.left instanceof Expr.Variable var) enviroment.assign(var.name, result);
        else throw new RuntimeError(expr.operator, "Invalid target for postfix operator.");

        return result;
    }
    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condition = eval(expr.condition);
        Object then = eval(expr.thenBranch);
        Object otherwise = eval(expr.elseBranch);
        if (Truthful(condition)) return then;
        return otherwise;
    }
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = eval(expr.left);
        return switch (expr.operator.type) {
            case OR -> Truthful(left) ? left : eval(expr.right);
            case AND -> !Truthful(left) ? left : eval(expr.right);
            default -> null;
        };
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return enviroment.get(expr.name);
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = eval(expr.value);
        enviroment.assign(expr.name, value);
        return value;
    }
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = eval(expr.left);
        Object right = eval(expr.right);

        switch (expr.operator.type) {
            // math
            case PLUS:
                if (left instanceof Double l && right instanceof Double r) return l + r;
                if (left instanceof String || right instanceof String) {
                    left = left instanceof Double ? ((Double) left).intValue() : left;
                    right = right instanceof Double ? ((Double) right).intValue() : right;
                    return String.valueOf(left) + right;
                }
                throw new RuntimeError(expr.operator,"Operands must be two numbers or two strings.");
            case MINUS:
                if (left instanceof Double l && right instanceof Double r) return l - r;
                if (left instanceof String l && right instanceof String r) return l.replace(r, "");
                throw new RuntimeError(expr.operator,"Operands must be two numbers or two strings.");
            case STAR:
                if (left instanceof Double l && right instanceof Double r) return l * r;
                if (left instanceof String && right instanceof Double) return ((String) left).repeat((int)right);
                throw new RuntimeError(expr.operator,"Operands must be two numbers or a string and a number.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if (((Double)right).intValue() == 0) throw new RuntimeError(expr.operator, "can't devide by 0.");
                return (double)left / (double)right;
            // comparison
            case LESS: checkNumberOperands(expr.operator, left, right); return (double)left < (double)right;
            case GREATER: checkNumberOperands(expr.operator, left, right); return (double)left > (double)right;
            case LESS_EQUAL: checkNumberOperands(expr.operator, left, right); return (double)left <= (double)right;
            case GREATER_EQUAL: checkNumberOperands(expr.operator, left, right); return (double)left >= (double)right;
            // equality
            case EQUAL_EQUAL: return equality(left, right);
            case BANG_EQUAL: return !equality(left, right);
            // special
            case COMMA: return right;
//            case COMMA:
//                if (left instanceof Object[] leftArray) {
//                    Object[] result = new Object[leftArray.length + 1];
//                    System.arraycopy(leftArray, 0, result, 0, leftArray.length);
//                    result[result.length - 1] = right;
//                    return result;
//                }
//                return new Object[] { left, right };
            default: return null;
        }
    }

    // Statement visitors
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        eval(stmt.expression);
        return null;
    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (Truthful(eval(stmt.condition))) {
            execute(stmt.thenBranch);
        }
        else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (Truthful(eval(stmt.condition))) execute(stmt.body);
        return null;
    }
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Enviroment(enviroment));
        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer!=null) value = eval(stmt.initializer);
        enviroment.define(stmt.name.lexeme, value);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object val = eval(stmt.expression);
        System.out.println(stringify(val));
        return null;
    }


    //  -- Helper Methods --
    // operand checks
    private void checkNumberOperand(Token operator, Object right) {
        if (right instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    // mics checks
    private Boolean equality(Object left, Object right) {
        if (left == null & right == null) return true;
        if (left == null) return false;
        return left.equals(right);
    }
    private Boolean Truthful(Object obj) {
        return switch (obj) {
            case null -> false;
            case Boolean bool -> bool;
            case String str -> str.isEmpty();
            case Object[] arr when arr.length == 0 -> false;
            default -> true;
        };
    }

    // visitor helpers
    private Object eval(Expr expr) {
        return expr.accept(this);
    }
    private void execute(Stmt statement) {
        statement.accept(this);
    }
    private void executeBlock(List<Stmt> statements, Enviroment enviroment) {
        // account for parent enviorment and current
        Enviroment previousEnv = this.enviroment;

        try{
            this.enviroment = enviroment;
            // process statements
            for ( Stmt statement : statements) execute(statement);
        }
        finally {
            // flush new current enviroment once block is finished
            this.enviroment = previousEnv;
        }
    }
    // misc
    private String stringify(Object obj) {
        switch (obj) {
            case null: return "zilch";

            case Object[] sequence:
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < sequence.length; i++) {
                    builder.append(stringify(sequence[i]));
                    if (i < sequence.length - 1) builder.append(", ");
                }
                return builder.toString();

            case Double v:
                String text = v.toString();
                if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
                return text;

            default: return obj.toString();
        }

    }

}
