package decaf.compiler.types;

public record Token (String text, TokenType type, int line) {
    @Override
    public String toString() {
        return line + " " + type + " " + text;
    }
}