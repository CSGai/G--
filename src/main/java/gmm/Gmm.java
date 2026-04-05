package main.java.gmm;

import main.java.gmm.ast.Stmt;
import main.java.gmm.ast.Token;
import main.java.gmm.ast.TokenType;
import main.java.gmm.runtime.Interpreter;
import main.java.gmm.errors.RuntimeError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class Gmm {
    static boolean errorFlag = false;
    static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        // for testing:
        runFile("src/main/misc/testRead.gmm");

//        if (args.length > 1) {
//            System.exit(64);
//        } else if (args.length == 1) {
//            runFile(args[0]);
//        } else {
//            runPrompt();
//        }

    }

    private static void runPrompt() throws IOException {
        StringBuilder programData = new StringBuilder();
        for (;;) {
            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            String line = buffer.readLine();
            programData.append(line).append("\n");
            if (line == null) break;
            else if (line.equals("exit()")) System.exit(64);
            else if (line.equals("reset()")) programData.setLength(0);
            run(programData.toString());
            errorFlag = false;
        }
    }
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, StandardCharsets.UTF_8));
        if (errorFlag) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }
    private static void run(String sourceCode) throws RuntimeException {
        if (errorFlag) System.exit(65);

        // Scanner
        Lexer lex = new Lexer(sourceCode);
        List<Token> tokens =  lex.scanTokens();
        System.out.println("tokens:");
        for (Token token : tokens) {
            System.out.println(token);
            if (token.type == TokenType.SEMICOLON) System.out.println(" ");
        }
        System.out.println("...........");

        // Parser
        AstPrinter ast = new AstPrinter();
        Parser parser = new Parser(tokens);
        List<Stmt> statments = parser.parse();
        System.out.println(ast.printAll(statments));

        System.out.println("...........");

        // Stop if there was a syntax/parse error.
        if (errorFlag) return;

        // Resolver (semantic analysis)
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statments);

        // Stop if there was a resolution error.
        if (errorFlag) return;

        // Interpreter
        interpreter.interpret(statments);
    }

    // error handling
    public static void error(int line, String message) {
        report(line, "", message);
    }
    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF) report(token.line, " at end", message);
        else report(token.line, token.lexeme, message);
    }
    public static void warning(Token token, String message) {
        System.err.println("[line " + token.line + "] Warning at '" + token.lexeme + "' -> " + message);
    }
    private static void report(int line, String location, String message) {
        line++;
        System.err.println("[line " + line + "] Error at '" + location + "' -> " + message);
        errorFlag = true;
    }

    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
