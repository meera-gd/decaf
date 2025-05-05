package decaf.compiler.types;

public record ErrorToken (String filename, int lineNumber, int characterNumber, String expectation, char actual)
    implements TokenOrError {
    @Override
    public String toString() {
        String prefix = filename + " line " + lineNumber + ":" + characterNumber + ": ";
        String actualString = (actual == '\n')? "0xA" : "'" + actual + "'";
        if (expectation == null) {
            return prefix + "unexpected char: " + actualString;
        }
       return prefix + "expecting '" + expectation + "', found " + actualString;
    }
}