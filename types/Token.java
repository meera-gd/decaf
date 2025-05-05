package decaf.compiler.types;

public record Token (String text, TokenType type, int line) implements TokenOrErrorToken {
    @Override
    public String toString() {
        if (type == null) {
            return line + " " + text;
        }
        return line + " " + type + " " + text;
    }
}