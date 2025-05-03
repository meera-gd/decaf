package decaf.compiler.phases;

import decaf.compiler.types.Token;
import decaf.compiler.types.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LexicalAnalyzer {

    private final String sourceText;
    private final String sourceFile;
    private final List<Token> tokens = new ArrayList<>();
    private int currentPosition = 0;
    private int lookahead = 0;
    private int lineNumber = 1;
    private int lookaheadLineCount = 0;

    public LexicalAnalyzer(String sourceText, String sourceFile) {
        this.sourceText = sourceText;
        this.sourceFile = sourceFile;
    }

    private static boolean isUnescapedCharacterInStringOrCharacterLiteral(char c) {
        return ' ' <= c && c <= '~' && c != '"' && c != '\'' && c != '\\';
    }

    private static boolean isEscapedCharacterInStringOrCharacterLiteral(char c) {
        return c == '"' || c == '\'' || c == '\\' || c == 't' || c == 'n';
    }

    private static boolean isCharacterAtStartOfKeywordOrIdentifier(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    private static boolean isCharacterInsideKeywordOrIdentifier(char c) {
        return isCharacterAtStartOfKeywordOrIdentifier(c) || ('0' <= c && c <= '9');
    }

    private static boolean isNonCommentWhiteSpaceCharacter(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private boolean lookaheadStartsWithString(String s) {
        return sourceText.startsWith(s, lookahead);
    }

    private boolean lookaheadStartsWithCharacterInClass(Predicate<Character> isInClass) {
        return sourceText.length() > lookahead && isInClass.test(sourceText.charAt(lookahead));
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

    private void readToken(TokenType type) {
        tokens.add(new Token(sourceText.substring(currentPosition, lookahead), type, lineNumber));
        advanceCurrentPosition();
    }

    private void abort() {
        throw new RuntimeException("Unrecognized token on line " + lineNumber);
    }

    public List<Token> analyze() {
        while (currentPosition < sourceText.length()) { // TODO replace

            // WHITESPACE and COMMENTS
            if (lookaheadStartsWithCharacterInClass(LexicalAnalyzer::isNonCommentWhiteSpaceCharacter)) {
                advanceLookahead(1);
                advanceCurrentPosition();
            } else if (lookaheadStartsWithString("//")) {
                int commentLength = sourceText.indexOf("\n", lookahead) - lookahead;
                if (commentLength < 2) {
                    commentLength = sourceText.length() - lookahead;
                }
                advanceLookahead(commentLength);
                advanceCurrentPosition();

            // IDENTIFIERS and BOOLEAN LITERALS and KEYWORDS
            } else if (lookaheadStartsWithCharacterInClass(LexicalAnalyzer::isCharacterAtStartOfKeywordOrIdentifier)) {
                    advanceLookahead(1);
                    while (lookaheadStartsWithCharacterInClass(LexicalAnalyzer::isCharacterInsideKeywordOrIdentifier)) {
                        advanceLookahead(1);
                    }
                    String tokenString = sourceText.substring(currentPosition, lookahead);
                    if(tokenString.equals("true") || tokenString.equals("false")) {
                        readToken(TokenType.BOOLEANLITERAL);
                    } else if (tokenString.equals("boolean") || tokenString.equals("break") || tokenString.equals("callout") ||
                        tokenString.equals("class") || tokenString.equals("continue") || tokenString.equals("else") ||
                        tokenString.equals("for") || tokenString.equals("if") || tokenString.equals("int") ||
                        tokenString.equals("return") || tokenString.equals("void")) {
                        readToken(null);
                    } else {
                        readToken(TokenType.IDENTIFIER);
                    }

            // CHARACTER LITERALS
            } else if (lookaheadStartsWithString("'")) {
                advanceLookahead(1);
                if (lookaheadStartsWithCharacterInClass(LexicalAnalyzer::isUnescapedCharacterInStringOrCharacterLiteral)) {
                    advanceLookahead(1);
                } else if (lookaheadStartsWithString("\\")) {
                    advanceLookahead(1);
                    if (lookaheadStartsWithCharacterInClass(LexicalAnalyzer::isEscapedCharacterInStringOrCharacterLiteral)) {
                        advanceLookahead(1);
                    }
                    else {
                        abort();
                    }
                } else {
                    abort();
                }
                if (lookaheadStartsWithString("'")) {
                    advanceLookahead(1);
                    readToken(TokenType.CHARLITERAL);
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