package decaf.compiler.phases;

import decaf.compiler.types.Token;
import decaf.compiler.types.TokenType;

import java.util.ArrayList;
import java.util.List;

public class LexicalAnalyzer {

    private final String sourceText;
    private final List<Token> tokens = new ArrayList<>();
    private int currentPosition = 0;
    private int lookahead = 0;
    private int lineNumber = 1;
    private int lookaheadLineCount = 0;

    public LexicalAnalyzer(String sourceText) {
        this.sourceText = sourceText;
    }

    private static boolean isUnescapedCharacterInStringOrCharacterLiteral(char c) {
        return ' ' <= c && c <= '~' && c != '"' && c != '\'' && c != '\\';
    }

    private static boolean isEscapedCharacterInStringOrCharacterLiteral(char c) {
        return c == '"' || c == '\'' || c == '\\' || c == 't' || c == 'n';
    }

    private static boolean isNonCommentWhiteSpaceCharacter(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private void advanceCurrentPosition() {
        currentPosition = lookahead;
        lineNumber = lineNumber + lookaheadLineCount;
        lookaheadLineCount = 0;
    }

    private void advanceLookahead(int n) {
        for (int i = 0; i < n; i++) {
            lookahead += 1;
            if (sourceText.startsWith("\n", lookahead)) {
                lookaheadLineCount += 1;
            }
        }
    }

    private void abort() {
        throw new RuntimeException("Unrecognized token on line " + lineNumber);
    }

    public List<Token> analyze() {
        while (currentPosition < sourceText.length()) {
            if (sourceText.length() > lookahead && isNonCommentWhiteSpaceCharacter(sourceText.charAt(lookahead))) {
                advanceLookahead(1);
                advanceCurrentPosition();
            } else if (sourceText.startsWith("//", lookahead)) {
                int commentLength = sourceText.indexOf("\n", lookahead) + 1 - lookahead;
                if (commentLength < 3) {
                    commentLength = sourceText.length() - lookahead;
                }
                advanceLookahead(commentLength);
                advanceCurrentPosition();
            } else if (sourceText.startsWith("'", lookahead)) {
                advanceLookahead(1);
                if (sourceText.length() > lookahead && isUnescapedCharacterInStringOrCharacterLiteral(sourceText.charAt(lookahead))) {
                    advanceLookahead(1);
                } else if (sourceText.startsWith("\\", lookahead)) {
                    advanceLookahead(1);
                    if (sourceText.length() > lookahead && isUnescapedCharacterInStringOrCharacterLiteral(sourceText.charAt(lookahead))) {
                        advanceLookahead(1);
                    }
                    else {
                        abort();
                    }
                } else {
                    abort();
                }
                if (sourceText.startsWith("'", lookahead)) {
                    advanceLookahead(1);
                    tokens.add(new Token(sourceText.substring(currentPosition, lookahead), TokenType.CHARLITERAL, lineNumber));
                    advanceCurrentPosition();
                } else {
                    abort();
                }
            } else {
                abort();
            }
        }
        return tokens;
    }

}