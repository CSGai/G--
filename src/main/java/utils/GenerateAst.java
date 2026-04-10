package main.java.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        String outputDir = "./src/main/java/gmm/ast";

        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Get      : Token name, Expr object",
                "Set      : Token name, Expr object, Expr value",
                "This     : Token keyword",
                "Lambda   : List<Token> params, Stmt body",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right",
                "Postfix  : Expr left, Token operator",
                "Ternary  : Expr condition, Expr thenBranch, Expr elseBranch",
                "Logical  : Expr left, Token operator, Expr right",
                "Variable : Token name"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "While      : Expr condition, Stmt body",
                "Break      : Token self",
                "Continue   : Token self",
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",
                "Function   : Token name, List<Token> params, List<Stmt> body, boolean isGetter, TokenType accessModifier, Token staticModifier",
                "Class      : Token name, List<Stmt.Function> methods",
                "Print      : Expr expression"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package main.java.gmm.ast;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("public abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        for (String type : types) {
            String[] split = type.split(":");
            String className = split[0].trim();
            String fields = split[1].trim();

            defineType(writer, baseName, className, fields);
        }

        writer.println();
        writer.println("    public abstract <R> R accept(Visitor<R> visitor);");
        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    public interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fields) {
        writer.println("    public static class " + className + " extends " + baseName + " {");

        // Constructor.
        writer.println("        public " + className + "(" + fields + ") {");

        String[] fieldList = fields.split(", ");
        for (String field : fieldList) {
            if (field.isEmpty()) continue;
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");

        // Visitor pattern.
        writer.println();
        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        // Fields.
        writer.println();
        for (String field : fieldList) {
            if (field.isEmpty()) continue;
            writer.println("        public final " + field + ";");
        }

        writer.println("    }");
    }
}