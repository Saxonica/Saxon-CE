package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;

/**
 * Tokenizer for expressions and inputs.
 *
 * This code was originally derived from James Clark's xt, but has been almost entirely rewritten.
 */


@SuppressWarnings({"StringEquality"})
public final class Tokenizer {


    private int state = DEFAULT_STATE;
        // we may need to make this a stack at some time

    /**
     * Initial default state of the Tokenizer
     */
    public static final int DEFAULT_STATE = 0;

    /**
     * State in which a name is NOT to be merged with what comes next, for example "("
     */
    public static final int BARE_NAME_STATE = 1;

    /**
     * The number identifying the most recently read token
     */
    public int currentToken = Token.EOF;
    /**
     * The string value of the most recently read token
     */
    public String currentTokenValue = null;
    /**
     * The position in the input expression where the current token starts
     */
    public int currentTokenStartOffset = 0;
    /**
     * The number of the next token to be returned
     */
    private int nextToken = Token.EOF;
    /**
     * The string value of the next token to be returned
     */
    private String nextTokenValue = null;
    /**
     * The position in the expression of the start of the next token
     */
    private int nextTokenStartOffset = 0;
    /**
     * The string being parsed
     */
    public String input;
    /**
     * The current position within the input string
     */
    public int inputOffset = 0;
    /**
     * The length of the input string
     */
    private int inputLength;

    /**
     * The token number of the token that preceded the current token
     */
    private int precedingToken = Token.UNKNOWN;

    public Tokenizer() {}

    /**
     * Get the current tokenizer state
     * @return the current state
     */

    //
    // Lexical analyser for expressions, queries, and XSLT patterns
    //

    /**
     * Prepare a string for tokenization.
     * The actual tokens are obtained by calls on next()
     *
     * @param input the string to be tokenized
     * @param start start point within the string
     * @param end end point within the string (last character not read):
     * -1 means end of string
     * @throws XPathException if a lexical error occurs, e.g. unmatched
     *     string quotes
     */
    public void tokenize(String input, int start, int end) throws XPathException {
        nextToken = Token.EOF;
        nextTokenValue = null;
        nextTokenStartOffset = 0;
        inputOffset = start;
        this.input = input;
         if (end==-1) {
            inputLength = input.length();
        } else {
            inputLength = end;
        }

        // The tokenizer actually reads one token ahead. The raw lexical analysis performed by
        // the lookAhead() method does not (in general) distinguish names used as QNames from names
        // used for operators, axes, and functions. The next() routine further refines names into the
        // correct category, by looking at the following token. In addition, it combines compound tokens
        // such as "instance of" and "cast as".

        lookAhead();
        next();
    }

    //diagnostic version of next(): change real version to realnext()
    //
    //public void next() throws XPathException {
    //    realnext();
    //    System.err.println("Token: " + currentToken + "[" + tokens[currentToken] + "]");
    //}

    /**
     * Get the next token from the input expression. The type of token is returned in the
     * currentToken variable, the string value of the token in currentTokenValue.
     *
     * @throws XPathException if a lexical error is detected
     */

    public void next() throws XPathException {
        precedingToken = currentToken;
        currentToken = nextToken;
        currentTokenValue = nextTokenValue;
        if (currentTokenValue==null) {
            currentTokenValue="";
        }
        currentTokenStartOffset = nextTokenStartOffset;

        // disambiguate the current token based on the tokenizer state

        switch (currentToken) {
            case Token.NAME:
                int optype = getBinaryOp(currentTokenValue);
                if (optype!=Token.UNKNOWN && !followsOperator(precedingToken)) {
                    currentToken = optype;
                }
                break;
            case Token.STAR:
                if (!followsOperator(precedingToken)) {
                    currentToken = Token.MULT;
                }
                break;
        }

        if (currentToken == Token.RCURLY) {
            // End of an AVT
            return;
        }

        int oldPrecedingToken = precedingToken;
        lookAhead();

        if (currentToken == Token.NAME) {
            if (state == BARE_NAME_STATE) {
                return;
            }
            if (oldPrecedingToken == Token.DOLLAR) {
                return;
            }
            switch (nextToken) {
                case Token.LPAR:
                    int op = getBinaryOp(currentTokenValue);
                    // the test on followsOperator() is to cater for an operator being used as a function name,
                    // e.g. is(): see XQTS test K-FunctionProlog-66
                    if (op == Token.UNKNOWN || followsOperator(oldPrecedingToken)) {
	                    currentToken = getFunctionType(currentTokenValue);
	                    lookAhead();    // swallow the "("
                    } else {
                        currentToken = op;
                    }
                    break;

                case Token.COLONCOLON:
                    lookAhead();
                    currentToken = Token.AXIS;
                    break;

                case Token.COLONSTAR:
                    lookAhead();
                    currentToken = Token.PREFIX;
                    break;

                case Token.DOLLAR:
                    if (currentTokenValue.equals("for")) {
                        currentToken = Token.FOR;
                    } else if (currentTokenValue.equals("some")) {
                        currentToken = Token.SOME;
                    } else if (currentTokenValue.equals("every")) {
                        currentToken = Token.EVERY;
                    }
                    break;

                case Token.NAME:
                    String composite = currentTokenValue + ' ' + nextTokenValue;
                    Integer val = Token.doubleKeywords.get(composite);
                    if (val==null) {
                        break;
                    } else {
                        currentToken = val;
                        currentTokenValue = composite;
                        lookAhead();
                        return;
                    }
                default:
                    // no action needed
            }
        }
    }


    /**
     * Look ahead by one token. This method does the real tokenization work.
     * The method is normally called internally, but the XQuery parser also
     * calls it to resume normal tokenization after dealing with pseudo-XML
     * syntax.
     * @throws XPathException if a lexical error occurs
     */
    public void lookAhead() throws XPathException {
        precedingToken = nextToken;
        nextTokenValue = null;
        nextTokenStartOffset = inputOffset;
        for (;;) {
            if (inputOffset >= inputLength) {
	            nextToken = Token.EOF;
	            return;
            }
            char c = input.charAt(inputOffset++);
            switch (c) {
            case '/':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '/') {
	                inputOffset++;
	                nextToken = Token.SLSL;
	                return;
	            }
	            nextToken = Token.SLASH;
	            return;
            case ':':
	            if (inputOffset < inputLength) {
	                if (input.charAt(inputOffset) == ':') {
	                    inputOffset++;
	                    nextToken = Token.COLONCOLON;
	                    return;
	                }
	            }
	            throw new XPathException("Unexpected colon at start of token");
            case '@':
	            nextToken = Token.AT;
	            return;
	        case '?':
	            nextToken = Token.QMARK;
	            return;
            case '[':
	            nextToken = Token.LSQB;
	            return;
            case ']':
	            nextToken = Token.RSQB;
	            return;
            case '}':
	            nextToken = Token.RCURLY;
	            return;
            case '(':
	            if (inputOffset < inputLength && input.charAt(inputOffset) == ':') {
                    // XPath comment syntax is (: .... :)
                    // Comments may be nested, and may now be empty
                    inputOffset++;
                    int nestingDepth = 1;
                    while (nestingDepth > 0 && inputOffset < (inputLength-1)) {
                        if (input.charAt(inputOffset) == ':' &&
                                input.charAt(inputOffset+1) == ')') {
                            nestingDepth--;
                            inputOffset++;
                        } else if (input.charAt(inputOffset) == '(' &&
                               input.charAt(inputOffset+1) == ':') {
                            nestingDepth++;
                            inputOffset++;
                        }
                        inputOffset++;
                    }
                    if (nestingDepth > 0) {
                        throw new XPathException("Unclosed XPath comment");
                    }
                    lookAhead();
                } else {
	                nextToken = Token.LPAR;
	            }
	            return;
            case ')':
	            nextToken = Token.RPAR;
	            return;
            case '+':
	            nextToken = Token.PLUS;
	            return;
            case '-':
	            nextToken = Token.MINUS;   // not detected if part of a name
	            return;
            case '=':
	            nextToken = Token.EQUALS;
	            return;
            case '!':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '=') {
	                inputOffset++;
	                nextToken = Token.NE;
	                return;
	            }
	            throw new XPathException("'!' without '='");
            case '*':
                // disambiguation of MULT and STAR is now done later
                if (inputOffset < inputLength
                        && input.charAt(inputOffset) == ':') {
                    inputOffset++;
                    nextToken = Token.SUFFIX;
                    // we leave the parser to get the following name as a separate
                    // token, but first check there's no intervening white space or comments
                    if (inputOffset < inputLength) {
                        char ahead = input.charAt(inputOffset);
                        if (" \r\t\n(".indexOf(ahead) >= 0) {
                            throw new XPathException("Whitespace and comments are not allowed after '*:'");
                        }
                    }
                    return;
                }
                nextToken = Token.STAR;
	            return;
            case ',':
	            nextToken = Token.COMMA;
	            return;
            case '$':
	            nextToken = Token.DOLLAR;
	            return;
            case '|':
	            nextToken = Token.UNION;
	            return;
            case '#':
	            nextToken = Token.HASH;
	            return;
            case '<':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '=') {
	                inputOffset++;
	                nextToken = Token.LE;
	                return;
	            }
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '<') {
	                inputOffset++;
	                nextToken = Token.PRECEDES;
	                return;
	            }
	            nextToken = Token.LT;
	            return;
            case '>':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '=') {
	                inputOffset++;
	                nextToken = Token.GE;
	                return;
	            }
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '>') {
	                inputOffset++;
	                nextToken = Token.FOLLOWS;
	                return;
	            }
	            nextToken = Token.GT;
	            return;
            case '.':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '.') {
	                inputOffset++;
	                nextToken = Token.DOTDOT;
	                return;
	            }
	            if (inputOffset == inputLength
	                    || input.charAt(inputOffset) < '0'
	                    || input.charAt(inputOffset) > '9') {
	                nextToken = Token.DOT;
	                return;
	            }
                // otherwise drop through: we have a number starting with a decimal point
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                // The logic here can return some tokens that are not legitimate numbers,
                // for example "23e" or "1.0e+". However, this will only happen if the XPath
                // expression as a whole is syntactically incorrect.
                // These errors will be caught by the numeric constructor.
                boolean allowE = true;
                boolean allowSign = false;
                boolean allowDot = true;
                boolean endOfNum = false;
            numloop:
                while (!endOfNum) {
	                switch (c) {
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9':
                            allowSign = false;
                            break;
                        case '.':
                            if (allowDot) {
                                allowDot = false;
                                allowSign = false;
                            } else {
                                inputOffset--;
                                break numloop;
                            }
                            break;
                        case 'E': case 'e':
                            if (allowE) {
                                allowSign = true;
                                allowE = false;
                            } else {
                                inputOffset--;
                                break numloop;
                            }
                            break;
                        case '+': case '-':
                            if (allowSign) {
                                allowSign = false;
                            } else {
                                inputOffset--;
                                break numloop;
                            }
                            break;
                        default:
                            if (('a' <= c && c <= 'z') || c>127) {
                                // this prevents the famous "10div 3"
                                throw new XPathException("Separator needed after numeric literal");
                            }
                            inputOffset--;
                            break numloop;
                    }
                    if (inputOffset >= inputLength) break;
                    c = input.charAt(inputOffset++);
	            }
	            nextTokenValue = input.substring(nextTokenStartOffset, inputOffset);
	            nextToken = Token.NUMBER;
	            return;
            case '"':
            case '\'':
                nextTokenValue = "";
                while (true) {
    	            inputOffset = input.indexOf(c, inputOffset);
    	            if (inputOffset < 0) {
    	                inputOffset = nextTokenStartOffset + 1;
    	                throw new XPathException("Unmatched quote in expression");
    	            }
    	            nextTokenValue += input.substring(nextTokenStartOffset + 1, inputOffset++);
                    if (inputOffset < inputLength) {
                        char n = input.charAt(inputOffset);
                        if (n == c) {
                            // Doubled delimiters
                            nextTokenValue += c;
                            nextTokenStartOffset = inputOffset;
                            inputOffset++;
                        } else {
                            break;
                        }
                    } else {
	                    break;
	                }
	            }

	            nextToken = Token.STRING_LITERAL;
	            return;
            case '\n':
            case ' ':
            case '\t':
            case '\r':
	            nextTokenStartOffset = inputOffset;
	            break;
            default:
	            if (c < 0x80 && !Character.isLetter(c)) {
	                throw new XPathException("Invalid character '" + c + "' in expression");
                }
                /* fall through */
            case '_':
            loop:
	            for (;inputOffset < inputLength; inputOffset++) {
	                c = input.charAt(inputOffset);
	                switch (c) {
                    case ':':
        	            if (inputOffset+1 < inputLength) {
    	                    char nc = input.charAt(inputOffset+1);
                            if (nc == ':') {
                                nextTokenValue = input.substring(nextTokenStartOffset, inputOffset);
                                //nextTokenValue = nextTokenValue.intern();
                                nextToken = Token.AXIS;
                                inputOffset+=2;
                                return;
        	                } else if (nc == '*') {
                                nextTokenValue = input.substring(nextTokenStartOffset, inputOffset);
                                //nextTokenValue = nextTokenValue.intern();
                                nextToken = Token.PREFIX;
                                inputOffset+=2;
                                return;
                            } else if (nc == '=') {
                                // as in "let $x:=2"
                                nextTokenValue = input.substring(nextTokenStartOffset, inputOffset);
                                //nextTokenValue = nextTokenValue.intern();
                                nextToken = Token.NAME;
                                return;
                            }
        	            }
                        break;
	                case '.':
	                case '-':
	                case '_':
	                    break;

	                default:
	                    if (c < 0x80 && !Character.isLetterOrDigit(c))
	                        break loop;
	                    break;
	                }
	            }
	            nextTokenValue = input.substring(nextTokenStartOffset, inputOffset);
                //nextTokenValue = nextTokenValue.intern();
                nextToken = Token.NAME;
	            return;
            }
        }
    }

    /**
     * Identify a binary operator
     *
     * @param s String representation of the operator - must be interned
     * @return the token number of the operator, or UNKNOWN if it is not a
     *     known operator
     */

    private static int getBinaryOp(String s) {
        switch(s.length()) {
            case 2:
                if (s.equals("or")) return Token.OR;
                if (s.equals("is")) return Token.IS;
                if (s.equals("to")) return Token.TO;
                if (s.equals("in")) return Token.IN;
                if (s.equals("eq")) return Token.FEQ;
                if (s.equals("ne")) return Token.FNE;
                if (s.equals("gt")) return Token.FGT;
                if (s.equals("ge")) return Token.FGE;
                if (s.equals("lt")) return Token.FLT;
                if (s.equals("le")) return Token.FLE;
                if (s.equals("as")) return Token.AS;
                break;
            case 3:
                if (s.equals("and")) return Token.AND;
                if (s.equals("div")) return Token.DIV;
                if (s.equals("mod")) return Token.MOD;
                break;
            case 4:
                if (s.equals("idiv")) return Token.IDIV;
                if (s.equals("then")) return Token.THEN;
                if (s.equals("else")) return Token.ELSE;
                break;
            case 5:
                if (s.equals("union")) return Token.UNION;
                break;
            case 6:
                if (s.equals("except")) return Token.EXCEPT;
                if (s.equals("return")) return Token.RETURN;
                break;
            case 9:
                if (s.equals("intersect")) return Token.INTERSECT;
                if (s.equals("satisfies")) return Token.SATISFIES;
                break;
        }
        return Token.UNKNOWN;
    }

    /**
     * Distinguish nodekind names, "if", and function names, which are all
     * followed by a "("
     *
     * @param s the name - must be interned
     * @return the token number
     */

    private static int getFunctionType(String s) {
        switch(s.length()) {
            case 2:
                if (s.equals("if")) return Token.IF;
                break;
            case 4:
                if (s.equals("node")) return Token.NODEKIND;
                if (s.equals("item")) return Token.NODEKIND;
                if (s.equals("text")) return Token.NODEKIND;
                break;
            case 7:
                if (s.equals("element")) return Token.NODEKIND;
                if (s.equals("comment")) return Token.NODEKIND;
                break;
            case 9:
                if (s.equals("attribute")) return Token.NODEKIND;
                break;
            default:
                if (s.equals("document-node")) return Token.NODEKIND;
                if (s.equals("empty-sequence")) return Token.NODEKIND;
                //if (s.equals("namespace-node")) return Token.NODEKIND;
                if (s.equals("schema-element")) return Token.NODEKIND;
                if (s.equals("schema-attribute")) return Token.NODEKIND;
                if (s.equals("processing-instruction")) return Token.NODEKIND;

                break;
        }
        return Token.FUNCTION;
    }

    /**
     * Test whether the previous token is an operator
     * @param precedingToken the token to be tested
     * @return true if the previous token is an operator token
     */

    private boolean followsOperator(int precedingToken) {
        return precedingToken <= Token.LAST_OPERATOR;
    }

     /**
     * Get the most recently read text (for use in an error message)
     * @param offset the offset of the offending token, if known, or -1 to use the current offset
     * @return a chunk of text leading up to the error
     */

    public String recentText(int offset) {
        if (offset == -1) {
            // if no offset was supplied, we want the text immediately before the current reading position
            if (inputOffset > inputLength) {
                inputOffset = inputLength;
            }
            if (inputOffset < 34) {
                return input.substring(0, inputOffset);
            } else {
                return Whitespace.collapseWhitespace(
                        "..." + input.substring(inputOffset-30, inputOffset)).toString();
            }
        } else {
            // if a specific offset was supplied, we want the text *starting* at that offset
            int end = offset + 30;
            if (end > inputLength) {
                end = inputLength;
            }
            return Whitespace.collapseWhitespace(
                        (offset > 0 ? "..." : "") + 
                        input.substring(offset, end)).toString();
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.