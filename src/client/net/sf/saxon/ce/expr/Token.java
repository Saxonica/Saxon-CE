package client.net.sf.saxon.ce.expr;

import java.util.HashMap;

/**
 * This class holds static constants and methods defining the lexical tokens used in
 * XPath and XQuery, and associated keywords.
 */

public abstract class Token {

    /**
     * Token numbers. Those in the range 0 to 100 are tokens that can be followed
     * by a name or expression; those in the range 101 to 200 are tokens that can be
     * followed by an binary operator.
     */

    /**
     * Pseudo-token representing the end of the expression
     */
    public static final int EOF = 0;
    /**
     * "union" or "|" token
     */
    public static final int UNION = 1;
    /**
     * Forwards "/"
     */
    public static final int SLASH = 2;
    /**
     * At token, "@"
     */
    public static final int AT = 3;
    /**
     * Left square bracket
     */
    public static final int LSQB = 4;
    /**
     * Left parenthesis
     */
    public static final int LPAR = 5;
    /**
     * Equals token ("=")
     */
    public static final int EQUALS = 6;
    /**
     * Comma token
     */
    public static final int COMMA = 7;
    /**
     * Double forwards slash, "//"
     */
    public static final int SLSL = 8;
    /**
     * Operator "or"
     */
    public static final int OR = 9;
    /**
     * Operator "and"
     */
    public static final int AND = 10;
    /**
     * Operator ">"
     */
    public static final int GT = 11;
    /**
     * Operator "<"
     */
    public static final int LT = 12;
    /**
     * Operator ">="
     */
    public static final int GE = 13;
    /**
     * Operator "<="
     */
    public static final int LE = 14;
    /**
     * Operator "+"
     */
    public static final int PLUS = 15;
    /**
     * Binary minus operator
     */
    public static final int MINUS = 16;
    /**
     * Multiply operator, "*" when used in an operator context
     */
    public static final int MULT = 17;
    /**
     * Operator "div"
     */
    public static final int DIV = 18;
    /**
     * Operator "mod"
     */
    public static final int MOD = 19;
    /**
     * Operator "is"
     */
    public static final int IS = 20;
    /**
     * "$" symbol
     */
    public static final int DOLLAR = 21;
    /**
     * Operator not-equals. That is, "!="
     */
    public static final int NE = 22;
    /**
     * Operator "intersect"
     */
    public static final int INTERSECT = 23;
    /**
     * Operator "except"
     */
    public static final int EXCEPT = 24;
    /**
     * Keyword "return"
     */
    public static final int RETURN = 25;
    /**
     * Ketword "then"
     */
    public static final int THEN = 26;
    /**
     * Keyword "else"
     */
    public static final int ELSE = 27;
    /**
     * Operator "to"
     */
    public static final int TO = 29;
    /**
     * Keyword "in"
     */
    public static final int IN = 30;
    /**
     * Keyword "some"
     */
    public static final int SOME = 31;
    /**
     * Keyword "every"
     */
    public static final int EVERY = 32;
    /**
     * Keyword "satisfies"
     */
    public static final int SATISFIES = 33;
    /**
     * Token representing the name of a function and the following "(" symbol
     */
    public static final int FUNCTION = 34;
    /**
     * Token representing the name of an axis and the following "::" symbol
     */
    public static final int AXIS = 35;
    /**
     * Keyword "if"
     */
    public static final int IF = 36;
    /**
     * Operator "<<"
     */
    public static final int PRECEDES = 37;
    /**
     * Operator ">>"
     */
    public static final int FOLLOWS = 38;
    /**
     * "::" symbol
     */
    public static final int COLONCOLON = 39;
    /**
     * ":*" symbol
     */
    public static final int COLONSTAR = 40;
    /**
     * # symbol
     */
    public static final int HASH = 44;
    /**
     * operator "instance of"
     */
    public static final int INSTANCE_OF = 45;
    /**
     * operator "cast as"
     */
    public static final int CAST_AS = 46;
    /**
     * operator "treat as"
     */
    public static final int TREAT_AS = 47;
    /**
     * operator "eq"
     */
    public static final int FEQ = 50;       // "Fortran" style comparison operators eq, ne, etc
    /**
     * operator "ne"
     */
    public static final int FNE = 51;
    /**
     * operator "gt"
     */
    public static final int FGT = 52;
    /**
     * operator "lt"
     */
    public static final int FLT = 53;
    /**
     * operator "ge"
     */
    public static final int FGE = 54;
    /**
     * opeartor "le"
     */
    public static final int FLE = 55;
    /**
     * operator "idiv"
     */
    public static final int IDIV = 56;
    /**
     * operator "castable as"
     */
    public static final int CASTABLE_AS = 57;

     /**
     * Node kind, e.g. "node()" or "comment()"
     */
    public static final int NODEKIND = 69;
    /**
     * "*:" token
     */
    public static final int SUFFIX = 70;    // e.g. *:suffix - the suffix is actually a separate token
    /**
     * "as" (in XQuery Update rename expression)
     */
    public static final int AS = 71;





    /**
     * Constant identifying the token number of the last token to be classified as an operator
     */
    static int LAST_OPERATOR = 150;

    // Tokens that set "operator" context, so an immediately following "div" is recognized
    // as an operator, not as an element name

    /**
     * Name token (a QName, in general)
     */
    public static final int NAME = 201;
    /**
     * String literal
     */
    public static final int STRING_LITERAL = 202;
    /**
     * Right square bracket
     */
    public static final int RSQB = 203;
    /**
     * Right parenthesis
     */
    public static final int RPAR = 204;
    /**
     * "." symbol
     */
    public static final int DOT = 205;
    /**
     * ".." symbol
     */
    public static final int DOTDOT = 206;
    /**
     * "*" symbol when used as a wildcard
     */
    public static final int STAR = 207;
    /**
     * "prefix:*" token
     */
    public static final int PREFIX = 208;    // e.g. prefix:*
    /**
     * Numeric literal
     */
    public static final int NUMBER = 209;

    /**
     * "for" keyword
     */
    public static final int FOR = 211;


    /**
     * Question mark symbol. That is, "?"
     */
    public static final int QMARK = 213;
    /**
     * "}" symbol (XQuery only)
     */
    public static final int RCURLY = 215;


    /**
     * Unary minus sign
     */
    public static final int NEGATE = 299;    // unary minus: not actually a token, but we
                                             // use token numbers to identify operators.


    /**
     * The following strings are used to represent tokens in error messages
     */

    public final static String[] tokens = new String[300];
    static {
        tokens [ EOF ] = "<eof>";
        tokens [ UNION ] = "|";
        tokens [ SLASH ] = "/";
        tokens [ AT ] = "@";
        tokens [ LSQB ] = "[";
        tokens [ LPAR ] = "(";
        tokens [ EQUALS ] = "=";
        tokens [ COMMA ] = ",";
        tokens [ SLSL ] = "//";
        tokens [ OR ] = "or";
        tokens [ AND ] = "and";
        tokens [ GT ] = ">";
        tokens [ LT ] = "<";
        tokens [ GE ] = ">=";
        tokens [ LE ] = "<=";
        tokens [ PLUS ] = "+";
        tokens [ MINUS ] = "-";
        tokens [ MULT ] = "*";
        tokens [ DIV ] = "div";
        tokens [ MOD ] = "mod";
        tokens [ IS ] = "is";
        tokens [ DOLLAR ] = "$";
        tokens [ NE ] = "!=";
        tokens [ INTERSECT ] = "intersect";
        tokens [ EXCEPT ] = "except";
        tokens [ RETURN ] = "return";
        tokens [ THEN ] = "then";
        tokens [ ELSE ] = "else";
        //tokens [ ISNOT ] = "isnot";
        tokens [ TO ] = "to";
        tokens [ IN ] = "in";
        tokens [ SOME ] = "some";
        tokens [ EVERY ] = "every";
        tokens [ SATISFIES ] = "satisfies";
        tokens [ FUNCTION ] = "<function>(";
        tokens [ AXIS ] = "<axis>";
        tokens [ IF ] = "if(";
        tokens [ PRECEDES ] = "<<";
        tokens [ FOLLOWS ] = ">>";
        tokens [ COLONCOLON ] = "::";
        tokens [ COLONSTAR ] = ":*";
        tokens [ HASH ] = "#";
        tokens [ INSTANCE_OF ] = "instance of";
        tokens [ CAST_AS ] = "cast as";
        tokens [ TREAT_AS ] = "treat as";
        tokens [ FEQ ] = "eq";
        tokens [ FNE ] = "ne";
        tokens [ FGT ] = "gt";
        tokens [ FGE ] = "ge";
        tokens [ FLT ] = "lt";
        tokens [ FLE ] = "le";
        tokens [ IDIV ] = "idiv";
        tokens [ CASTABLE_AS ] = "castable as";
        tokens [ AS ] = "as";


        tokens [ NAME ] = "<name>";
        tokens [ STRING_LITERAL ] = "<string-literal>";
        tokens [ RSQB ] = "]";
        tokens [ RPAR ] = ")";
        tokens [ DOT ] = ".";
        tokens [ DOTDOT ] = "..";
        tokens [ STAR ] = "*";
        tokens [ PREFIX ] = "<prefix:*>";
        tokens [ NUMBER ] = "<numeric-literal>";
        tokens [ NODEKIND ] = "<node-type>()";
        tokens [ FOR ] = "for";
        tokens [ SUFFIX ] = "<*:local-name>";
        tokens [ RCURLY ] = "}";
        tokens [ NEGATE ] = "-";
    }

    /**
     * Lookup table for composite (two-keyword) tokens
     */
    public static HashMap<String, Integer> doubleKeywords = new HashMap<String, Integer>(30);
    /**
     * Pseudo-token representing the start of the expression
     */
    public static final int UNKNOWN = -1;

    private Token() {
    }

    static {
        mapDouble("instance of", INSTANCE_OF);
        mapDouble("cast as", CAST_AS);
        mapDouble("treat as", TREAT_AS);
        mapDouble("castable as", CASTABLE_AS);
    }

    private static void mapDouble(String doubleKeyword, int token) {
        doubleKeywords.put(doubleKeyword, new Integer(token));
        tokens[token] = doubleKeyword;
    }

    /**
	* Return the inverse of a relational operator, so that "a op b" can be
	* rewritten as "b inverse(op) a"
	*/

    public static int inverse(int operator) {
        switch(operator) {
            case LT:
                return GT;
            case LE:
                return GE;
            case GT:
                return LT;
            case GE:
                return LE;
            case FLT:
                return FGT;
            case FLE:
                return FGE;
            case FGT:
                return FLT;
            case FGE:
                return FLE;
            default:
                return operator;
        }
    }

    /**
	* Return the negation of a relational operator, so that "a op b" can be
	* rewritten as not(b op' a)
	*/

    public static int negate(int operator) {
        switch(operator) {
            case FEQ:
                return FNE;
            case FNE:
                return FEQ;
            case FLT:
                return FGE;
            case FLE:
                return FGT;
            case FGT:
                return FLE;
            case FGE:
                return FLT;
            default:
                throw new IllegalArgumentException("Invalid operator for negate()");
        }
    }

    public static boolean isOrderedOperator(int operator) {
        return operator != FEQ && operator != FNE;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.