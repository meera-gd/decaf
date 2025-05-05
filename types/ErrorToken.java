package decaf.compiler.types;

public record ErrorToken (String filename, int lineNumber, int characterNumber, String expectation, char actual)
    implements TokenOrErrorToken {
    @Override
    public String toString() {
        String prefix = filename + " line " + lineNumber + ":" + characterNumber + ": ";
        String actualString;
        if (expectation == null) {
            actualString = switch (actual) {
                case '\f' -> "0xC";
                case '\n' -> "0xA";
                case '\t' -> "0x9";
                default -> "'" + actual + "'";
            };
            return prefix + "unexpected char: " + actualString;
        }
        actualString = switch (actual) {
            case '\f' -> "'\\f'";
            case '\n' -> "'\\n'";
            case '\t' -> "'\\t'";
            default -> "'" + actual + "'";
        };
        return prefix + "expecting '" + expectation + "', found " + actualString;
    }
}