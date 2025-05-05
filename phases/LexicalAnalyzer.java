package decaf.compiler.phases;

import decaf.compiler.types.ErrorToken;
import decaf.compiler.types.Token;
import decaf.compiler.types.TokenOrErrorToken;
import decaf.compiler.types.TokenType;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LexicalAnalyzer {

    private final String sourceText;
    private final String sourceFile;
    private final List<TokenOrErrorToken> tokens = new ArrayList<>();
    private int currentPosition = 0;
    private int lookahead = 0;
    private int lineNumber = 1;
    private int characterNumber = 1;

    private static record FixedLengthPredicate (Predicate<String> function, int length) {
    }

    private static final List<String> keywords = Arrays.asList(
        "boolean", "break", "callout", "class", "continue",
        "else", "for", "if", "int", "return", "void"
    );
    private static final List<String> oneCharacterOperators = Arrays.asList("+", "-", "*", "/", "%", "<", ">", "!", "=");
    private static final List<String> twoCharacterOperators = Arrays.asList("==", "!=", "<=", ">=", "&&", "||", "+=", "-=");
    private static final List<String> punctuationCharacters = Arrays.asList("(", ")", "{", "}", "[", "]", ",", ";");

    private static final FixedLengthPredicate isNonCommentWhiteSpaceCharacter = new FixedLengthPredicate(s -> {
        return s.equals(" ") || s.equals("\t") || s.equals("\n") || s.equals("\r") || s.equals("\f");
    }, 1);

    private static final FixedLengthPredicate isDecimalDigit = new FixedLengthPredicate(s -> {
        char c = s.charAt(0);
        return '0' <= c && c <= '9';
    }, 1);

    private static final FixedLengthPredicate isHexDigit = new FixedLengthPredicate(s -> {
        char c = s.charAt(0);
        return isDecimalDigit.function().test(s) || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
    }, 1);

    private static final FixedLengthPredicate isUnescapedCharacterInStringOrCharacterLiteral = new FixedLengthPredicate(s -> {
        char c = s.charAt(0);
        return ' ' <= c && c <= '~' && c != '"' && c != '\'' && c != '\\';
    }, 1);

    private static final FixedLengthPredicate isEscapedCharacterInStringOrCharacterLiteral = new FixedLengthPredicate(s -> {
        return s.equals("\"") || s.equals("'") || s.equals("\\") || s.equals("t") || s.equals("n");
    }, 1);

    private static final FixedLengthPredicate isCharacterAtStartOfKeywordOrIdentifier = new FixedLengthPredicate(s -> {
        char c = s.charAt(0);
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }, 1);

    private static final FixedLengthPredicate isCharacterInsideKeywordOrIdentifier = new FixedLengthPredicate(s -> {
        return isCharacterAtStartOfKeywordOrIdentifier.function().test(s) || isDecimalDigit.function().test(s);
    }, 1);

    private static final FixedLengthPredicate isOneCharacterOperator = new FixedLengthPredicate(s -> {
        return oneCharacterOperators.contains(s);
    }, 1);

    private static final FixedLengthPredicate isTwoCharacterOperator = new FixedLengthPredicate(s -> {
        return twoCharacterOperators.contains(s);
    }, 2);

    private static final FixedLengthPredicate isPunctuationCharacter = new FixedLengthPredicate(s -> {
        return punctuationCharacters.contains(s);
    }, 1);

    public LexicalAnalyzer(String sourceText, String sourceFile) {
        this.sourceText = sourceText;
        this.sourceFile = sourceFile;
    }

    private boolean lookaheadStartsWith(String s) {
        return sourceText.startsWith(s, lookahead);
    }

    private boolean lookaheadStartsWith(FixedLengthPredicate pred) {
        return sourceText.length() >= lookahead + pred.length()
            && pred.function().test(sourceText.substring(lookahead, lookahead + pred.length()));
    }

    private void advanceCurrentPosition() {
        if (sourceText.charAt(currentPosition) == '\n') {
            lineNumber += 1;
            characterNumber = 1;
        } else {
            characterNumber += lookahead - currentPosition;
        }
        currentPosition = lookahead;
    }

    private void advanceLookahead(int n) {
        lookahead += n;
    }

    private void readToken(TokenType type) {
        tokens.add(new Token(sourceText.substring(currentPosition, lookahead), type, lineNumber));
        advanceCurrentPosition();
    }

    private void readErrorToken(String expectation) {
        if (currentPosition < lookahead) {
            advanceCurrentPosition();
        }
        advanceLookahead(1);
        tokens.add(new ErrorToken(sourceFile, lineNumber, characterNumber, expectation, sourceText.charAt(currentPosition)));
        advanceCurrentPosition();
    }

    public List<TokenOrErrorToken> analyze() {
        while (currentPosition < sourceText.length()) {

            // WHITESPACE and COMMENTS
            if (lookaheadStartsWith(isNonCommentWhiteSpaceCharacter)) {
                advanceLookahead(1);
                advanceCurrentPosition();
            } else if (lookaheadStartsWith("//")) {
                int commentLength = sourceText.indexOf("\n", lookahead) - lookahead;
                if (commentLength < 2) {
                    commentLength = sourceText.length() - lookahead;
                }
                advanceLookahead(commentLength);
                advanceCurrentPosition();

            // IDENTIFIERS and BOOLEAN LITERALS and KEYWORDS
            } else if (lookaheadStartsWith(isCharacterAtStartOfKeywordOrIdentifier)) {
                advanceLookahead(1);
                while (lookaheadStartsWith(isCharacterInsideKeywordOrIdentifier)) {
                    advanceLookahead(1);
                }
                String tokenString = sourceText.substring(currentPosition, lookahead);
                if(tokenString.equals("true") || tokenString.equals("false")) {
                    readToken(TokenType.BOOLEANLITERAL);
                } else if (keywords.contains(tokenString)) {
                    readToken(null);
                } else {
                    readToken(TokenType.IDENTIFIER);
                }

            // INTEGER LITERALS
            } else if (lookaheadStartsWith(isDecimalDigit)) {
                if (lookaheadStartsWith("0x")) {
                    advanceLookahead(2);
                    if (lookaheadStartsWith(isHexDigit)) {
                        while (lookaheadStartsWith(isHexDigit)) {
                            advanceLookahead(1);
                        }
                        readToken(TokenType.INTLITERAL);
                    } else {
                        readErrorToken(null);
                    }
                } else {
                    while (lookaheadStartsWith(isDecimalDigit)) {
                        advanceLookahead(1);
                    }
                    readToken(TokenType.INTLITERAL);
                }

            // CHARACTER LITERALS
            } else if (lookaheadStartsWith("'")) {
                advanceLookahead(1);
                if (lookaheadStartsWith(isUnescapedCharacterInStringOrCharacterLiteral)) {
                    advanceLookahead(1);
                    if (lookaheadStartsWith("'")) {
                        advanceLookahead(1);
                        readToken(TokenType.CHARLITERAL);
                    } else {
                        readErrorToken("'");
                    }
                } else if (lookaheadStartsWith("\\")) {
                    advanceLookahead(1);
                    if (lookaheadStartsWith(isEscapedCharacterInStringOrCharacterLiteral)) {
                        advanceLookahead(1);
                        if (lookaheadStartsWith("'")) {
                            advanceLookahead(1);
                            readToken(TokenType.CHARLITERAL);
                        } else {
                            readErrorToken("'");
                        }
                    } else {
                        readErrorToken(null);
                    }
                } else {
                    readErrorToken(null);
                }

            // STRING LITERALS
            } else if (lookaheadStartsWith("\"")) {
                advanceLookahead(1);
                while (!lookaheadStartsWith("\"")) {
                    if (lookaheadStartsWith(isUnescapedCharacterInStringOrCharacterLiteral)) {
                        advanceLookahead(1);
                    } else if (lookaheadStartsWith("\\")) {
                        advanceLookahead(1);
                        if (lookaheadStartsWith(isEscapedCharacterInStringOrCharacterLiteral)) {
                            advanceLookahead(1);
                        } else {
                            readErrorToken(null);
                            break;
                        }
                    } else {
                        readErrorToken("\"");
                        break;
                    }
                }
                if (lookaheadStartsWith("\"")) {
                    advanceLookahead(1);
                    readToken(TokenType.STRINGLITERAL);
                }

            // OPERATORS
            } else if (lookaheadStartsWith(isTwoCharacterOperator)) {
                advanceLookahead(2);
                readToken(null);
            } else if (lookaheadStartsWith(isOneCharacterOperator)) {
                advanceLookahead(1);
                readToken(null);

            // PUNCTUATION CHARACTERS
            } else if (lookaheadStartsWith(isPunctuationCharacter)) {
                advanceLookahead(1);
                readToken(null);

            // unrecognized character
            } else {
                readErrorToken(null);
            }
        }
        return tokens;
    }

}