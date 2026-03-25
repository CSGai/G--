package main.java.gmm.runtime;

import main.java.gmm.Gmm;
import main.java.gmm.ast.Expr;
import main.java.gmm.ast.Stmt;
import main.java.gmm.ast.Token;
import main.java.gmm.runtime.callables.NativeFunctions;
import main.java.gmm.runtime.exceptions.Break;
import main.java.gmm.runtime.exceptions.Continue;
import main.java.gmm.runtime.exceptions.Return;
import main.java.gmm.runtime.callables.GmmCallable;
import main.java.gmm.runtime.callables.GmmFunction;
import main.java.gmm.runtime.callables.GmmLambda;
import main.java.gmm.errors.RuntimeError;

import java.util.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    public Interpreter() {
        NativeFunctions.functions.forEach(globals::define);
    }

    public void interpret(List<Stmt> statments) {
        try {
            for ( Stmt statement : statments) {
                execute(statement);
            }
        }
        catch (RuntimeError error) {
            Gmm.runtimeError(error);
        }
        catch (Break breakException) {
            Gmm.error(breakException.self, "shbor lo yachol lehiot kaiam mechots le loop");
        }
        catch (Continue continueException) {
            Gmm.error(continueException.self, "daleg lo yachol lehiot kaiam mechots le loop");
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
            throw new RuntimeError(expr.operator, "Operand haiav lehiot mispar");
        }

        double result = switch (expr.operator.type) {
            case PLUS_PLUS -> (double) left + 1;
            case MINUS_MINUS -> (double) left - 1;
            default -> throw new RuntimeError(expr.operator, "mapheil lo hoki le Postfix");
        };

        if (expr.left instanceof Expr.Variable var) environment.assign(var.name, result);
        else throw new RuntimeError(expr.operator, "matara lo betokeph le postfix mapheil");

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
        return lookUpVariable(expr.name, expr);
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = eval(expr.value);
        assignVariable(expr, value);
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
                throw new RuntimeError(expr.operator,"Operands haiav lehiot shney misparim oh shney machrozot");
            case MINUS:
                if (left instanceof Double l && right instanceof Double r) return l - r;
                if (left instanceof String l && right instanceof String r) return l.replace(r, "");
                throw new RuntimeError(expr.operator,"Operands haiav lehiot shney misparim oh shney machrozot");
            case STAR:
                if (left instanceof Double l && right instanceof Double r) return l * r;
                if (left instanceof String && right instanceof Double) return ((String) left).repeat(((Double) right).intValue());
                throw new RuntimeError(expr.operator,"Operands haiav lehiot shney misparim oh machrozet veh mispar");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if (((Double)right).intValue() == 0) throw new RuntimeError(expr.operator, "ei efshar lehalek be 0");
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
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = eval(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for ( Expr arg : expr.arguments) arguments.add(eval(arg));

        if (!(callee instanceof GmmCallable function)) {
            throw new RuntimeError(expr.paren,callee.toString() + "lo nitan lekria");
        }
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                           "Tsipa " + function.arity() + " argumentim aval kibel " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }
    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        return new GmmLambda(expr, environment);
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
        try {while (Truthful(eval(stmt.condition))) execute(stmt.body);}
        catch (Break ignored) {}
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break(stmt.self);
    }
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new Continue(stmt.self);
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = eval(stmt.value);

        throw new Return(value);
    }
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer!=null) value = eval(stmt.initializer);
        environment.define(stmt.name.lexeme, value);
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        GmmFunction function = new GmmFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }
    @Deprecated
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
        throw new RuntimeError(operator, "Operand haiav lehiot mispar");
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operand haiav lehiot mispar");
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
    public void execute(Stmt statement, Environment environment) {
        // account for parent enviorment and current
        Environment previousEnv = this.environment;
        try {
            // process expression in env
            this.environment = environment;
            execute(statement);
        }
        finally {
            // flush new current environment once block is finished
            this.environment = previousEnv;
        }
    }
    public void executeBlock(List<Stmt> statements, Environment environment) {
        // account for parent enviorment and current
        Environment previousEnv = this.environment;

        try {
            this.environment = environment;
            // process statements
            for ( Stmt statement : statements) {
                execute(statement);
            };
        }
        catch (Continue ignored) {}
        finally {
            // flush new current environment once block is finished
            this.environment = previousEnv;
        }
    }
    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }
    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) return environment.getAt(distance, name.lexeme);
        return globals.get(name);
    }
    private void assignVariable(Expr.Assign expr, Object value) {
        Integer distance = locals.get(expr);
        if (distance != null) environment.assignAt(distance, expr.name, value);
        else globals.assign(expr.name, value);
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
