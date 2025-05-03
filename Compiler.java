package decaf.compiler;

import decaf.compiler.phases.LexicalAnalyzer;
import decaf.compiler.types.Token;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class Compiler {

    private static String target = "assembly";
    private static String sourceFile;
    private static String outputFile;

    private static void parseOptions(String[] args) {
        int index = 0;
        while (index < args.length) {
            String current = args[index];
            if (current.startsWith("-")) {
                String option = current;
                index++;
                current = args[index];
                switch (option) {
                    case "-target":
                        target = current;
                        break;
                    case "-o":
                        outputFile = current;
                        break;
                    default:
                        System.out.println("Unrecognized option: " + current);
                        System.exit(1);
                }
            } else {
                sourceFile = current;
            }
            index++;
        }

        if (sourceFile == null) {
            System.out.println("Source file unspecified.");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        try {
            parseOptions(args);

            String sourceText = new String(Files.readAllBytes(Paths.get(sourceFile)), StandardCharsets.UTF_8);
            List<Token> tokens = new LexicalAnalyzer(sourceText, sourceFile).analyze();

            switch (target) {
                case "scan":
                    Files.write(
                        Paths.get(outputFile),
                        tokens.stream().map(Token::toString).toList()
                    );
                    break;
                default:
                    System.out.println("Unknown target.");
                    System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Unknown exception: " + e);
            System.exit(1);
        }
    }
}
