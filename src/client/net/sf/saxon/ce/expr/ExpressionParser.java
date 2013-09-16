package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.Block;
import client.net.sf.saxon.ce.expr.instruct.Choose;
import client.net.sf.saxon.ce.functions.CurrentGroup;
import client.net.sf.saxon.ce.functions.CurrentGroupingKey;
import client.net.sf.saxon.ce.functions.RegexGroup;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Parser for XPath expressions and XSLT patterns.
 *
 * This code was originally inspired by James Clark's xt but has been totally rewritten (several times)
 *
 * The class handles parsing of XPath 2.0 syntax.
 */


public class ExpressionParser {

    protected Tokenizer t;
    protected StaticContext env;
    protected Stack<Binding> rangeVariables = new Stack<Binding>();
        // The stack holds a list of range variables that are in scope.
        // Each entry on the stack is a Binding object containing details
        // of the variable.

    protected Container defaultContainer;

    protected int language = XPATH;     // know which language we are parsing, for diagnostics
    public static final int XPATH = 0;
    public static final int XSLT_PATTERN = 1;
    public static final int SEQUENCE_TYPE = 2;


    /**
     * Create an expression parser
     */

    public ExpressionParser(){
    }

    /**
     * Get the tokenizer (the lexical analyzer)
     * @return  the tokenizer (the lexical analyzer)
     */

    public Tokenizer getTokenizer() {
        return t;
    }

    /**
     * Get the static context used by this expression parser
     * @return the static context
     */

    public StaticContext getStaticContext() {
        return env;
    }

    /**
     * Set the default container for newly constructed expressions
     * @param container the default container
     */

    public void setDefaultContainer(Container container) {
        this.defaultContainer = container;
    }

    /**
     * Read the next token, catching any exception thrown by the tokenizer
     */

    public void nextToken() throws XPathException {
        try {
            t.next();
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
    }

    /**
     * Expect a given token; fail if the current token is different. Note that this method
     * does not read any tokens.
     *
     * @param token the expected token
     * @throws XPathException if the current token is not the expected
     *     token
     */

    public void expect(int token) throws XPathException {
        if (t.currentToken != token)
            grumble("expected \"" + Token.tokens[token] +
                             "\", found " + currentTokenDisplay());
    }

    /**
     * Report a syntax error (a static error with error code XP0003)
     * @param message the error message
     * @throws XPathException always thrown: an exception containing the
     *     supplied message
     */

    public void grumble(String message) throws XPathException {
        grumble(message, (language == XSLT_PATTERN ? "XTSE0340" : "XPST0003"));
    }

    /**
     * Report a static error
     *
     * @param message the error message
     * @param errorCode the error code
     * @throws XPathException always thrown: an exception containing the
     *                                        supplied message
     */

    public void grumble(String message, String errorCode) throws XPathException {
        grumble(message, new StructuredQName("", NamespaceConstant.ERR, errorCode));
    }

    /**
     * Report a static error
     *
     * @param message the error message
     * @param errorCode the error code
     * @throws XPathException always thrown: an exception containing the
     *     supplied message
     */

    protected void grumble(String message, StructuredQName errorCode) throws XPathException {
        if (errorCode == null) {
            errorCode = new StructuredQName("err", NamespaceConstant.ERR, "XPST0003");
        }
        String s = t.recentText(-1);
        String prefix = getLanguage() + " syntax error " +
                    (s.startsWith("...") ? "near" : "in") +
                    ' ' + Err.wrap(s) + ":\n    ";
        XPathException err = new XPathException(message);
        err.setAdditionalLocationText(prefix);
        err.setIsStaticError(true);
        err.setErrorCodeQName(errorCode);
        throw err;
    }

    /**
     * Set the current language (XPath or XQuery, XSLT Pattern, or SequenceType)
     * @param language one of the constants {@link #XPATH}, {@link #XSLT_PATTERN}, {@link #SEQUENCE_TYPE}
     */

    public void setLanguage(int language) {
        switch (language) {
            case XPATH:
            case XSLT_PATTERN:
            case SEQUENCE_TYPE:
                break;
            default:
                throw new IllegalArgumentException("Unknown language " + language);
        }
        this.language = language;
    }

    /**
     * Get the current language (XPath or XQuery)
     * @return a string representation of the language being parsed, for use in error messages
     */

    protected String getLanguage() {
        switch (language) {
            case XPATH:
                return "XPath";
            case XSLT_PATTERN:
                return "XSLT Pattern";
            case SEQUENCE_TYPE:
                return "SequenceType";
            default:
                return "XPath";
        }
    }

    /**
     * Display the current token in an error message
     *
     * @return the display representation of the token
     */
    protected String currentTokenDisplay() {
        if (t.currentToken==Token.NAME) {
            return "name \"" + t.currentTokenValue + '\"';
        } else if (t.currentToken==Token.UNKNOWN) {
            return "(unknown token)";
        } else {
            return '\"' + Token.tokens[t.currentToken] + '\"';
        }
    }

	/**
	 * Parse a string representing an expression. This will accept an XPath expression if called on an
     * ExpressionParser, or an XQuery expression if called on a QueryParser.
	 *
	 * @throws XPathException if the expression contains a syntax error
	 * @param expression the expression expressed as a String
     * @param start offset within the string where parsing is to start
     * @param terminator character to treat as terminating the expression
     * @param env the static context for the expression
     * @return an Expression object representing the result of parsing
	 */

	public Expression parse(String expression, int start, int terminator, StaticContext env)
            throws XPathException {
        // System.err.println("Parse expression: " + expression);
	    this.env = env;

        //defaultContainer = new TemporaryContainer(env.getLocationMap(), 1);
        t = new Tokenizer();
        try {
	        t.tokenize(expression, start, -1);
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
        Expression exp = parseExpression();
        if (t.currentToken != terminator) {
            if (t.currentToken == Token.EOF && terminator == Token.RCURLY) {
                grumble("Missing curly brace after expression in attribute value template", "XTSE0350");
            } else {
                grumble("Unexpected token " + currentTokenDisplay() + " beyond end of expression");
            }
        }
        return exp;
    }


    /**
     * Parse a string representing a sequence type
     *
     * @param input the string, which should conform to the XPath SequenceType
     *      production
     * @param env the static context
     * @throws XPathException if any error is encountered
     * @return a SequenceType object representing the type
     */

    public SequenceType parseSequenceType(String input, StaticContext env) throws XPathException {
        this.env = env;
        language = SEQUENCE_TYPE;
        t = new Tokenizer();
        try {
            t.tokenize(input, 0, -1);
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
        SequenceType req = parseSequenceType();
        if (t.currentToken != Token.EOF) {
            grumble("Unexpected token " + currentTokenDisplay() + " beyond end of SequenceType");
        }
        return req;
    }


    //////////////////////////////////////////////////////////////////////////////////
    //                     EXPRESSIONS                                              //
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Parse a top-level Expression:
     * ExprSingle ( ',' ExprSingle )*
     *
     * @throws XPathException if the expression contains a syntax error
     * @return the Expression object that results from parsing
     */

    public Expression parseExpression() throws XPathException {
        Expression exp = parseExprSingle();
        ArrayList<Expression> list = null;
        while (t.currentToken == Token.COMMA) {
            // An expression containing a comma often contains many, so we accumulate all the
            // subexpressions into a list before creating the Block expression which reduces it to an array
            if (list == null) {
                list = new ArrayList<Expression>(10);
                list.add(exp);
            }
            nextToken();
            Expression next = parseExprSingle();
            setLocation(next);
            list.add(next);
        }
        if (list != null) {
            exp = Block.makeBlock(list);
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse an ExprSingle
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    public Expression parseExprSingle() throws XPathException {
        switch (t.currentToken) {
            case Token.FOR:
            case Token.SOME:
            case Token.EVERY:
                return parseMappingExpression();
            case Token.IF:
                return parseIfExpression();

            default:
                return parseBinaryExpression(parseUnaryExpression(), 4);
        }
    }

    /**
     * Parse a binary expression, using operator precedence parsing. This is used
     * to parse the part of the grammary consisting largely of binary operators
     * distinguished by precedence: from "or expressions" down to "unary expressions".
     * Algorithm for the mainstream binary operators is from Wikipedia article
     * on precedence parsing;  operator precedences are from the XQuery specification
     * appendix B.
     * @param lhs Left-hand side "basic expression"
     * @param
     */

    public Expression parseBinaryExpression(Expression lhs, int minPrecedence) throws XPathException {
        while (getCurrentOperatorPrecedence() >= minPrecedence) {
            int operator = t.currentToken;
            int prec = getCurrentOperatorPrecedence();
            switch (operator) {
                case Token.INSTANCE_OF:
                case Token.TREAT_AS:
                    nextToken();
                    SequenceType seq = parseSequenceType();
                    lhs = makeSequenceTypeExpression(lhs, operator, seq);
                    setLocation(lhs);
                    if (getCurrentOperatorPrecedence() >= prec) {
                        grumble("Left operand of '" + Token.tokens[t.currentToken] + "' needs parentheses");
                    }
                    break;
                case Token.CAST_AS:
                case Token.CASTABLE_AS:
                    nextToken();
                    expect(Token.NAME);
                    AtomicType at = getAtomicType(t.currentTokenValue);
                    if (at == AtomicType.ANY_ATOMIC) {
                        grumble("No value is castable to xs:anyAtomicType", "XPST0080");
                    }
                    nextToken();
                    boolean allowEmpty = (t.currentToken == Token.QMARK);
                    if (allowEmpty) {
                        nextToken();
                    }
                    lhs = makeSingleTypeExpression(lhs, operator, at, allowEmpty);
                    setLocation(lhs);
                    if (getCurrentOperatorPrecedence() >= prec) {
                        grumble("Left operand of '" + Token.tokens[t.currentToken] + "' needs parentheses");
                    }
                    break;
                default:
                    nextToken();
                    Expression rhs = parseUnaryExpression();
                    while (getCurrentOperatorPrecedence() > prec) {
                        rhs = parseBinaryExpression(rhs, getCurrentOperatorPrecedence());
                    }
                    lhs = makeBinaryExpression(lhs, operator, rhs);
                    setLocation(lhs);
            }
        }
        return lhs;
    }

    private int getCurrentOperatorPrecedence() {
        switch (t.currentToken) {
            case Token.OR:
                return 4;
            case Token.AND:
                return 5;
            case Token.FEQ:
            case Token.FNE:
            case Token.FLE:
            case Token.FLT:
            case Token.FGE:
            case Token.FGT:
            case Token.EQUALS:
            case Token.NE:
            case Token.LE:
            case Token.LT:
            case Token.GE:
            case Token.GT:
            case Token.IS:
            case Token.PRECEDES:
            case Token.FOLLOWS:
                return 6;
            case Token.TO:
                return 7;
            case Token.PLUS:
            case Token.MINUS:
                return 8;
            case Token.MULT:
            case Token.DIV:
            case Token.IDIV:
            case Token.MOD:
                return 9;
            case Token.UNION:
                return 10;
            case Token.INTERSECT:
            case Token.EXCEPT:
                return 11;
            case Token.INSTANCE_OF:
                return 12;
            case Token.TREAT_AS:
                return 13;
            case Token.CASTABLE_AS:
                return 14;
            case Token.CAST_AS:
                return 15;
            default:
                return -1;
        }
    }

    private Expression makeBinaryExpression(Expression lhs, int operator, Expression rhs) {
        switch (operator) {
            case Token.OR:
            case Token.AND:
                return new BooleanExpression(lhs, operator, rhs);
            case Token.FEQ:
            case Token.FNE:
            case Token.FLE:
            case Token.FLT:
            case Token.FGE:
            case Token.FGT:
                return new ValueComparison(lhs, operator, rhs);
            case Token.EQUALS:
            case Token.NE:
            case Token.LE:
            case Token.LT:
            case Token.GE:
            case Token.GT:
                return new GeneralComparison(lhs, operator, rhs);
            case Token.IS:
            case Token.PRECEDES:
            case Token.FOLLOWS:
                return new IdentityComparison(lhs, operator, rhs);
            case Token.TO:
                return new RangeExpression(lhs, operator, rhs);
            case Token.PLUS:
            case Token.MINUS:
            case Token.MULT:
            case Token.DIV:
            case Token.IDIV:
            case Token.MOD:
                return new ArithmeticExpression(lhs, operator, rhs);
            case Token.UNION:
            case Token.INTERSECT:
            case Token.EXCEPT:
                return new VennExpression(lhs, operator, rhs);
            default:
                throw new IllegalArgumentException();
        }
    }

    private Expression makeSequenceTypeExpression(Expression lhs, int operator, SequenceType type) {
    switch (operator) {
            case Token.INSTANCE_OF:
                return new InstanceOfExpression(lhs, type);
            case Token.TREAT_AS:
                RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "treat as", 0);
                role.setErrorCode("XPDY0050");
                Expression e = CardinalityChecker.makeCardinalityChecker(lhs, type.getCardinality(), role);
                return new ItemChecker(e, type.getPrimaryType(), role);
            default:
                throw new IllegalArgumentException();
        }

    }

    private StructuredQName makeStructuredQName(String lexicalName, String defaultURI) throws XPathException {
        return StructuredQName.fromLexicalQName(lexicalName, defaultURI, env.getNamespaceResolver());
    }

    private Expression makeSingleTypeExpression(Expression lhs, int operator, AtomicType type, boolean allowEmpty)
    throws XPathException {
        if (type == AtomicType.QNAME && lhs instanceof StringLiteral) {
            try {
                String source = ((StringLiteral) lhs).getStringValue();
                makeStructuredQName(source, "");
                if (operator == Token.CASTABLE_AS) {
                    return new Literal(BooleanValue.TRUE);
                } else {
                    return new Literal(CastExpression.castStringToQName(source, env));
                }
            } catch (XPathException e) {
                if (operator == Token.CASTABLE_AS) {
                    return new Literal(BooleanValue.FALSE);
                } else {
                    grumble(e.getMessage(), e.getErrorCodeQName());
                    return null;
                }
            }
        }
        if (operator == Token.CASTABLE_AS) {
            return new CastableExpression(lhs, type, allowEmpty);
        } else {
            return new CastExpression(lhs, type, allowEmpty);
        }
    }

    /**
     * Parse a mapping expression. This is a common routine that handles
     * XPath 'for' expressions and quantified expressions.
     *
     * <p>Syntax: <br/>
     * (for|some|every) $x in expr (',' $y in expr)* (return|satisfies) expr
     * </p>
     *
     * <p>On entry, the current token indicates whether a for, some, or every
     * expression is expected.</p>
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseMappingExpression() throws XPathException {
        int offset = t.currentTokenStartOffset;
        int operator = t.currentToken;
        List<ForClause> clauseList = new ArrayList<ForClause>(3);
        do {
            ForClause clause = new ForClause();
            clause.offset = offset;
            clause.requiredType = SequenceType.SINGLE_ITEM;
            clauseList.add(clause);
            nextToken();
            skipToken(Token.DOLLAR);
            expect(Token.NAME);
            String var = t.currentTokenValue;

            // declare the range variable
            Assignation v;
            if (operator == Token.FOR) {
                v = new ForExpression();
            } else {
                v = new QuantifiedExpression();
                ((QuantifiedExpression)v).setOperator(operator);
            }
            v.setVariableQName(makeStructuredQName(var, ""));
            clause.rangeVariable = v;
            nextToken();

            // process the "in" clause
            skipToken(Token.IN);
            clause.sequence = parseExprSingle();
            declareRangeVariable(clause.rangeVariable);

        } while (t.currentToken==Token.COMMA);

        // process the "return/satisfies" expression (called the "action")
        if (operator==Token.FOR) {
            skipToken(Token.RETURN);
        } else {
            skipToken(Token.SATISFIES);
        }
        Expression action = parseExprSingle();

        // work back through the list of range variables, fixing up all references
        // to the variables in the inner expression

        final TypeHierarchy th = TypeHierarchy.getInstance();
        for (int i = clauseList.size()-1; i>=0; i--) {
            ForClause fc = clauseList.get(i);
            Assignation exp = fc.rangeVariable;
            setLocation(exp);
            exp.setSequence(fc.sequence);

            // Attempt to give the range variable a more precise type, base on analysis of the
            // "action" expression. This will often be approximate, because variables and function
            // calls in the action expression have not yet been resolved. We rely on the ability
            // of all expressions to return some kind of type information even if this is
            // imprecise.

            SequenceType type = SequenceType.makeSequenceType(
                    fc.sequence.getItemType(), StaticProperty.EXACTLY_ONE);
            fc.rangeVariable.setRequiredType(type);
            exp.setAction(action);

            // for the next outermost "for" clause, the "action" is this ForExpression
            action = exp;
        }

        // undeclare all the range variables

        for (int i = clauseList.size()-1; i>=0; i--) {
            ForClause clause = clauseList.get(i);
            for (int n = 0; n < clause.numberOfRangeVariables(); n++) {
                undeclareRangeVariable();
            }
        }
        //action = makeTracer(offset, action, Location.FOR_EXPRESSION, -1);
        return action;
    }


    /**
     * Parse an IF expression:
     * if '(' expr ')' 'then' expr 'else' expr
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseIfExpression() throws XPathException {
        // left paren already read
        nextToken();
        Expression condition = parseExpression();
        skipToken(Token.RPAR);
        skipToken(Token.THEN);
        Expression thenExp = parseExprSingle();
        skipToken(Token.ELSE);
        Expression elseExp = parseExprSingle();
        Expression ifExp = Choose.makeConditional(condition, thenExp, elseExp);
        setLocation(ifExp);
        return ifExp;
    }

    /**
     * Analyze a token whose expected value is the name of an atomic type,
     * and return the object representing the atomic type.
     * @param qname The lexical QName of the atomic type; alternatively, a Clark name
     * @return The atomic type
     * @throws XPathException if the QName is invalid or if no atomic type of that
     * name exists as a built-in type or a type in an imported schema
     */
    private AtomicType getAtomicType(String qname) throws XPathException {
        StructuredQName name = makeStructuredQName(qname, env.getDefaultElementNamespace());
        if (name.getNamespaceURI().equals(NamespaceConstant.SCHEMA)) {
            BuiltInType t = BuiltInType.getSchemaType(name.getLocalName());
            if (t instanceof AtomicType) {
                return (AtomicType)t;
            }
        }
        grumble("Unknown atomic type " + qname, "XPST0051");
        return null;
    }

    /**
     * Parse the sequence type production.
     * The QName must be the name of a built-in schema-defined data type.
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    public SequenceType parseSequenceType() throws XPathException {
        ItemType primaryType = parseItemType();
        if (primaryType instanceof EmptySequenceTest) {
            // No occurrence indicator allowed
            return SequenceType.makeSequenceType(primaryType, StaticProperty.EMPTY);
        }
        int occurrenceFlag;
        switch (t.currentToken) {
            case Token.STAR:
            case Token.MULT:
                // "*" will be tokenized different ways depending on what precedes it
                occurrenceFlag = StaticProperty.ALLOWS_ZERO_OR_MORE;
                break;
            case Token.PLUS:
                occurrenceFlag = StaticProperty.ALLOWS_ONE_OR_MORE;
                break;
            case Token.QMARK:
                occurrenceFlag = StaticProperty.ALLOWS_ZERO_OR_ONE;
                break;
            default:
                return SequenceType.makeSequenceType(primaryType, StaticProperty.EXACTLY_ONE);
        }
        // Make the tokenizer ignore the occurrence indicator when classifying the next token
        t.currentToken = Token.RPAR;
        nextToken();
        return SequenceType.makeSequenceType(primaryType, occurrenceFlag);
    }

    /**
     * Parse an ItemType within a SequenceType
     * @return the ItemType
     * @throws XPathException on a syntax error
     */

    protected ItemType parseItemType() throws XPathException {
        ItemType primaryType;
        if (t.currentToken == Token.NAME) {
            primaryType = getAtomicType(t.currentTokenValue);
            nextToken();
        } else if (t.currentToken == Token.NODEKIND) {
            if (t.currentTokenValue.equals("item")) {
                nextToken();
                skipToken(Token.RPAR);
                primaryType = AnyItemType.getInstance();
            } else if (t.currentTokenValue.equals("empty-sequence")) {
                nextToken();
                skipToken(Token.RPAR);
                primaryType = EmptySequenceTest.getInstance();
            } else {
                primaryType = parseKindTest();
            }
        } else {
            grumble("Expected type name in SequenceType, found " + Token.tokens[t.currentToken]);
            return null;
        }
        return primaryType;
    }

    /**
     * Parse a UnaryExpr:<br>
     * ('+'|'-')* ValueExpr
     * parsed as ('+'|'-')? UnaryExpr
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseUnaryExpression() throws XPathException {
        Expression exp;
        switch (t.currentToken) {
        case Token.MINUS:
            nextToken();
            exp = new ArithmeticExpression(new Literal(IntegerValue.ZERO),
                                          Token.NEGATE,
                                          parseUnaryExpression());
            break;
        case Token.PLUS:
            nextToken();
            // Unary plus: can't ignore it completely, it might be a type error, or it might
            // force conversion to a number which would affect operations such as "=".
            exp = new ArithmeticExpression(new Literal(IntegerValue.ZERO),
                                          Token.PLUS,
                                          parseUnaryExpression());
            break;
        default:
            exp = parsePathExpression();
        }
        setLocation(exp);
        return exp;
    }

    /**
     * Test whether the current token is one that can start a RelativePathExpression
     *
     * @return the resulting subexpression
     */

    protected boolean atStartOfRelativePath() {
        switch(t.currentToken) {
            case Token.AXIS:
            case Token.AT:
            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
            case Token.NODEKIND:
            case Token.DOT:
            case Token.DOTDOT:
            case Token.FUNCTION:
            case Token.STRING_LITERAL:
            case Token.NUMBER:
            case Token.LPAR:
            case Token.DOLLAR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Test whether the current token is one that is disallowed after a "leading lone slash".
     * These composite tokens have been parsed as operators, but are not allowed after "/" under the
     * rules of erratum E24
     *
     * @return the resulting subexpression
     */

    protected boolean disallowedAtStartOfRelativePath() {
        switch(t.currentToken) {
            // Although these "double keyword" operators can readily be recognized as operators,
            // they are not permitted after leading "/" under the rules of erratum XQ.E24
            case Token.CAST_AS:
            case Token.CASTABLE_AS:
            case Token.INSTANCE_OF:
            case Token.TREAT_AS:
                return true;
            default:
                return false;
        }
    }


    /**
     * Parse a PathExpresssion. This includes "true" path expressions such as A/B/C, and also
     * constructs that may start a path expression such as a variable reference $name or a
     * parenthesed expression (A|B). Numeric and string literals also come under this heading.
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parsePathExpression() throws XPathException {
        switch (t.currentToken) {
        case Token.SLASH:
            nextToken();
            final RootExpression start = new RootExpression();
            setLocation(start);
            if (disallowedAtStartOfRelativePath()) {
                grumble("Operator '" + Token.tokens[t.currentToken] + "' is not allowed after '/'");
            }
            if (atStartOfRelativePath()) {
                final Expression path = parseRemainingPath(start);
                setLocation(path);
                return path;
            } else {
                return start;
            }

        case Token.SLSL:
            // The logic for absolute path expressions changed in 8.4 so that //A/B/C parses to
            // (((root()/descendant-or-self::node())/A)/B)/C rather than
            // (root()/descendant-or-self::node())/(((A)/B)/C) as previously. This is to allow
            // the subsequent //A optimization to kick in.
            nextToken();
            final RootExpression start2 = new RootExpression();
            setLocation(start2);
            final AxisExpression axisExp = new AxisExpression(Axis.DESCENDANT_OR_SELF, null);
            setLocation(axisExp);
            final Expression exp = parseRemainingPath(new SlashExpression(start2, axisExp));
            setLocation(exp);
            return exp;
        default:
            return parseRelativePath();
        }

    }


    /**
     * Parse a relative path (a sequence of steps). Called when the current token immediately
     * follows a separator (/ or //), or an implicit separator (XYZ is equivalent to ./XYZ)
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseRelativePath() throws XPathException {
        Expression exp = parseStepExpression(language == XSLT_PATTERN);
        while (t.currentToken == Token.SLASH ||
                t.currentToken == Token.SLSL ) {
            int op = t.currentToken;
            nextToken();
            Expression next = parseStepExpression(false);
            if (op == Token.SLASH) {
                exp = new SlashExpression(exp, next);
            } else {
                // add implicit descendant-or-self::node() step
                AxisExpression ae = new AxisExpression(Axis.DESCENDANT_OR_SELF, null);
                setLocation(ae);
                SlashExpression se = new SlashExpression(ae, next);
                setLocation(se);
                exp = new SlashExpression(exp, se);
            }
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse the remaining steps of an absolute path expression (one starting in "/" or "//"). Note that the
     * token immediately after the "/" or "//" has already been read, and in the case of "/", it has been confirmed
     * that we have a path expression starting with "/" rather than a standalone "/" expression.
     * @param start the initial implicit expression: root() in the case of "/", root()/descendant-or-self::node in
     * the case of "//"
     * @return the completed path expression
     * @throws XPathException
     */
    protected Expression parseRemainingPath(Expression start) throws XPathException {
            Expression exp = start;
            int op = Token.SLASH;
            while (true) {
                Expression next = parseStepExpression(false);
                if (op == Token.SLASH) {
                    exp = new SlashExpression(exp, next);
                } else {
                    // add implicit descendant-or-self::node() step
                    AxisExpression descOrSelf = new AxisExpression(Axis.DESCENDANT_OR_SELF, null);
                    setLocation(descOrSelf);
                    SlashExpression step = new SlashExpression(descOrSelf, next);
                    setLocation(step);
                    exp = new SlashExpression(exp, step);
                }
                setLocation(exp);
                op = t.currentToken;
                if (op != Token.SLASH && op != Token.SLSL) {
                    break;
                }
                nextToken();
            }
            return exp;
        }


    /**
     * Parse a step (including an optional sequence of predicates)
     * @param firstInPattern true only if we are parsing the first step in a
     * RelativePathPattern in the XSLT Pattern syntax
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseStepExpression(boolean firstInPattern) throws XPathException {
        Expression step = parseBasicStep(firstInPattern);

        // When the filter is applied to an Axis step, the nodes are considered in
        // axis order. In all other cases they are considered in document order
        boolean reverse = (step instanceof AxisExpression) &&
                          !Axis.isForwards[((AxisExpression)step).getAxis()];
                           // &&  ((AxisExpression)step).getAxis() != Axis.SELF;

        while (true) {
            if (t.currentToken == Token.LSQB) {
                nextToken();
                Expression predicate = parsePredicate();
                skipToken(Token.RSQB);
                step = new FilterExpression(step, predicate);
                setLocation(step);
            } else {
                break;
            }
        }
        if (reverse) {
            return SystemFunction.makeSystemFunction("reverse", new Expression[]{step});
        } else {
            return step;
        }
    }

    /**
     * Parse the expression within a predicate. A separate method so it can be overridden
     */

    protected Expression parsePredicate() throws XPathException {
        return parseExpression();
    }

    /**
     * Parse a basic step expression (without the predicates)
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     * @param firstInPattern true only if we are parsing the first step in a
     * RelativePathPattern in the XSLT Pattern syntax
     */

    protected Expression parseBasicStep(boolean firstInPattern) throws XPathException {
        switch(t.currentToken) {
        case Token.DOLLAR:
            return parseVariableReference();

        case Token.LPAR:
            nextToken();
            if (t.currentToken==Token.RPAR) {
                nextToken();
                return new Literal(EmptySequence.getInstance());
            }
            Expression seq = parseExpression();
            skipToken(Token.RPAR);
            return seq;

        case Token.STRING_LITERAL:
            return parseStringLiteral();

        case Token.NUMBER:
            return parseNumericLiteral();

        case Token.FUNCTION:
            return parseFunctionCall();

        case Token.DOT:
            nextToken();
            Expression cie = new ContextItemExpression();
            setLocation(cie);
            return cie;

        case Token.DOTDOT:
            nextToken();
            Expression pne = new ParentNodeExpression();
            setLocation(pne);
            return pne;

        case Token.NODEKIND:
        case Token.NAME:
        case Token.PREFIX:
        case Token.SUFFIX:
        case Token.STAR:
        //case Token.NODEKIND:
            byte defaultAxis = Axis.CHILD;
            if (t.currentToken == Token.NODEKIND &&
                    (t.currentTokenValue.equals("attribute") || t.currentTokenValue.equals("schema-attribute"))) {
                defaultAxis = Axis.ATTRIBUTE;
            } else if (firstInPattern && t.currentToken == Token.NODEKIND && t.currentTokenValue.equals("document-node")) {
                defaultAxis = Axis.SELF;
            }
            NodeTest test = parseNodeTest(Type.ELEMENT);
            if (test instanceof AnyNodeTest) {
                // handles patterns of the form match="node()"
                test = (defaultAxis == Axis.CHILD ? AnyChildNodeTest.getInstance() : NodeKindTest.ATTRIBUTE);
            }
            AxisExpression ae = new AxisExpression(defaultAxis, test);
            setLocation(ae);
            return ae;

        case Token.AT:
            nextToken();
            switch(t.currentToken) {

            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
            case Token.NODEKIND:
                AxisExpression ae2 = new AxisExpression(Axis.ATTRIBUTE, parseNodeTest(Type.ATTRIBUTE));
                setLocation(ae2);
                return ae2;

            default:
                grumble("@ must be followed by a NodeTest");
            }
            break;

        case Token.AXIS:
            byte axis;
            try {
                axis = Axis.getAxisNumber(t.currentTokenValue);
            } catch (XPathException err) {
                grumble(err.getMessage());
                axis = Axis.CHILD; // error recovery
            }
            short principalNodeType = Axis.principalNodeType[axis];
            nextToken();
            switch (t.currentToken) {

            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
            case Token.NODEKIND:
                Expression ax = new AxisExpression(axis, parseNodeTest(principalNodeType));
                setLocation(ax);
                return ax;

            default:
                grumble("Unexpected token " + currentTokenDisplay() + " after axis name");
            }
            break;

        default:
            grumble("Unexpected token " + currentTokenDisplay() + " in path expression");
            //break;
        }
        return null;
    }

    protected Expression parseNumericLiteral() throws XPathException {
        NumericValue number = NumericValue.parseNumber(t.currentTokenValue);
        if (number.isNaN()) {
            grumble("Invalid numeric literal " + Err.wrap(t.currentTokenValue, Err.VALUE));
        }
        nextToken();
        Literal lit = new Literal(number);
        setLocation(lit);
        return lit;
    }

    protected Expression parseStringLiteral() throws XPathException {
        Literal literal = makeStringLiteral(t.currentTokenValue);
        nextToken();
        return literal;
    }

    protected Expression parseVariableReference() throws XPathException {
        nextToken();
        String var = t.currentTokenValue;
        skipToken(Token.NAME);

        StructuredQName vtest = makeStructuredQName(var, "");

        // See if it's a range variable or a variable in the context
        Binding b = findRangeVariable(vtest);
        Expression ref;
        if (b != null) {
            ref = new LocalVariableReference(b);
        } else {
            try {
                ref = env.bindVariable(vtest);
            } catch (XPathException err) {
                if ("XPST0008".equals(err.getErrorCodeLocalPart())) {
                    // Improve the error message
                    grumble("Variable $" + var + " has not been declared", "XPST0008");
                    return null;     // humour the compiler
                } else {
                    throw err;
                }
            }
        }
        setLocation(ref);
        return ref;
    }

    /**
     * Method to make a string literal from a token identified as a string
     * literal. This is trivial in XPath, but in XQuery the method is overridden
     * to identify pseudo-XML character and entity references. Note that the job of handling
     * doubled string delimiters is done by the tokenizer.
     * @param currentTokenValue the token as read (excluding quotation marks)
     * @return The string value of the string literal
     */

    private Literal makeStringLiteral(String currentTokenValue) {
        StringLiteral literal = new StringLiteral(currentTokenValue);
        setLocation(literal);
        return literal;
    }

    /**
     * Parse a NodeTest.
     * One of QName, prefix:*, *:suffix, *, text(), node(), comment(), or
     * processing-instruction(literal?), or element(~,~), attribute(~,~), etc.
     *
     * @throws XPathException if any error is encountered
     * @param nodeType the node type being sought if one is specified
     * @return the resulting NodeTest object
     */

    private NodeTest parseNodeTest(short nodeType) throws XPathException {
        int tok = t.currentToken;
        String tokv = t.currentTokenValue;
        switch (tok) {
        case Token.NAME:
            nextToken();
            StructuredQName nameCode = makeStructuredQName(tokv,
                    (nodeType == Type.ELEMENT ? env.getDefaultElementNamespace() : ""));
            return new NameTest(nodeType, nameCode);

        case Token.PREFIX:
            nextToken();
        	return makeNamespaceTest(nodeType, tokv);

        case Token.SUFFIX:
            nextToken();
            tokv = t.currentTokenValue;
            skipToken(Token.NAME);
        	return makeLocalNameTest(nodeType, tokv);

        case Token.STAR:
            nextToken();
            return NodeKindTest.makeNodeKindTest(nodeType);

        case Token.NODEKIND:
            return parseKindTest();

        default:
            grumble("Unrecognized node test");
            return null;
        }
    }

    /**
     * Parse a KindTest
     * @return the KindTest, expressed as a NodeTest object
     * @throws XPathException to indicate a syntax error
     */

    private NodeTest parseKindTest() throws XPathException {
        String typeName = t.currentTokenValue;
        if (typeName.startsWith("schema-")) {
            grumble(typeName + "() requires an imported schema");
            return null;
        }
        int primaryType = getSystemType(typeName);
        boolean empty = false;
        nextToken();
        if (t.currentToken == Token.RPAR) {
            empty = true;
        }
        NodeTest result;
        switch (primaryType) {
            case Type.ITEM:
                grumble("item() is not allowed in a path expression");
                return null;
            case Type.NODE:
                result = AnyNodeTest.getInstance();
                break;
            case Type.TEXT:
                result = NodeKindTest.TEXT;
                break;
            case Type.COMMENT:
                result = NodeKindTest.COMMENT;
                break;
            case Type.DOCUMENT:
                result = NodeKindTest.DOCUMENT;
                if (!empty) {
                    int innerType = getSystemType(t.currentTokenValue);
                    if (innerType != Type.ELEMENT) {
                        grumble("Argument to document-node() must be an element type");
                    }
                    NodeTest inner = parseKindTest();
                    skipToken(Token.RPAR);
                    return new DocumentNodeTest(inner);
                }
                break;
            case Type.PROCESSING_INSTRUCTION:
                result = NodeKindTest.PROCESSING_INSTRUCTION;
                if (!empty) {
                    StructuredQName piName = null;
                    if (t.currentToken == Token.STRING_LITERAL) {
                        t.currentToken = Token.NAME;
                        t.currentTokenValue = Whitespace.trim(t.currentTokenValue);
                    }
                    if (t.currentToken == Token.NAME && NameChecker.isValidNCName(t.currentTokenValue)) {
                        piName = new StructuredQName("", "", t.currentTokenValue);
                    }
                    if (piName == null) {
                        grumble("Processing instruction name must be an NCName (optionally in quotes)");
                    }
                    nextToken();
                    skipToken(Token.RPAR);
                    return new NameTest(Type.PROCESSING_INSTRUCTION, piName);
                }
                break;

            case Type.ATTRIBUTE:
            case Type.ELEMENT:
                result = NodeKindTest.makeNodeKindTest(primaryType);
                if (!empty) {
                    if (t.currentToken == Token.STAR || t.currentToken == Token.MULT) {
                        // no action
                    } else if (t.currentToken == Token.NAME) {
                        StructuredQName nodeQName = makeStructuredQName(t.currentTokenValue,
                                (primaryType == Type.ELEMENT ? env.getDefaultElementNamespace() : ""));
                        result = new NameTest(primaryType, nodeQName);
                    } else {
                        grumble("Unexpected " + Token.tokens[t.currentToken] + " in SequenceType");
                    }
                    nextToken();
                    if (t.currentToken == Token.COMMA) {
                        nextToken();
                        StructuredQName contentType =
                                makeStructuredQName(t.currentTokenValue, env.getDefaultElementNamespace());
                        BuiltInType schemaType = null;
                        if (contentType.getNamespaceURI().equals(NamespaceConstant.SCHEMA)) {
                            schemaType = BuiltInType.getSchemaType(contentType.getLocalName());
                        }
                        if (schemaType == null) {
                            grumble("Unknown schema type " + t.currentTokenValue);
                        }
                        if (primaryType == Type.ELEMENT &&
                                !(schemaType == AnyType.getInstance() || schemaType == Untyped.getInstance())) {
                            result = EmptySequenceTest.getInstance();
                        }
                        if (primaryType == Type.ATTRIBUTE &&
                                !(schemaType == AtomicType.ANY_ATOMIC || schemaType == AtomicType.UNTYPED_ATOMIC)) {
                            result = EmptySequenceTest.getInstance();
                        }
                        nextToken();
                        if (primaryType == Type.ELEMENT && t.currentToken == Token.QMARK) {
                            nextToken();
                        }
                    }
                    skipToken(Token.RPAR);
                    return result;
                }
                break;
            default:
                // can't happen!
                grumble("Unknown node kind");
                return null;
        }
        if (!empty) {
            grumble("Kind test " + typeName + "() must be empty");
        }
        skipToken(Token.RPAR);
        return result;
    }

    /**
     * Expect a specific token at the current position, and move to the next token
     * @param expected the expected token
     * @throws XPathException if the expected token was not found
     */

    private void skipToken(int expected) throws XPathException {
        expect(expected);
        nextToken();
    }

    /**
     * Get a system type - that is, one whose name is a keyword rather than a QName. This includes the node
     * kinds such as element and attribute, and the generic types node() and item()
     *
     * @param name the name of the system type, for example "element" or "comment"
     * @return the integer constant denoting the type, for example {@link Type#ITEM} or {@link Type#ELEMENT}
     * @throws XPathException if the name is not recognized
     */
    private int getSystemType(String name) throws XPathException {
        if ("item".equals(name))                   return Type.ITEM;
        else if ("document-node".equals(name))     return Type.DOCUMENT;
        else if ("element".equals(name))           return Type.ELEMENT;
        else if ("attribute".equals(name))         return Type.ATTRIBUTE;
        else if ("text".equals(name))              return Type.TEXT;
        else if ("comment".equals(name))           return Type.COMMENT;
        else if ("processing-instruction".equals(name))
                                                   return Type.PROCESSING_INSTRUCTION;
        //else if ("namespace-node".equals(name))    return Type.NAMESPACE;
        else if ("node".equals(name))              return Type.NODE;
        else {
            grumble("Unknown type " + name);
            return -1;
        }
    }

    /**
     * Parse a function call.
     * function-name '(' ( Expression (',' Expression )* )? ')'
     *
     * @throws XPathException if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseFunctionCall() throws XPathException {

        String fname = t.currentTokenValue;
        ArrayList<Expression> args = new ArrayList<Expression>(10);

        StructuredQName functionName =
                makeStructuredQName(fname, env.getDefaultFunctionNamespace());

        // the "(" has already been read by the Tokenizer: now parse the arguments

        nextToken();
        if (t.currentToken!=Token.RPAR) {
            while (true) {
                Expression arg = parseFunctionArgument();
                args.add(arg);
                if (t.currentToken == Token.COMMA) {
                    nextToken();
                } else {
                    break;
                }
            }
            expect(Token.RPAR);
        }
        nextToken();

        Expression[] arguments = new Expression[args.size()];
        args.toArray(arguments);

        Expression fcall;
        try {
            fcall = env.getFunctionLibrary().bind(functionName, arguments, env, defaultContainer);
        } catch (XPathException err) {
            if (err.getErrorCodeQName() == null) {
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
            }
            grumble(err.getMessage(), err.getErrorCodeQName());
            return null;
        }
        if (fcall == null) {
            String msg = "Cannot find a matching " + arguments.length +
                    "-argument function named " + functionName.getClarkName() + "()";
            if (env.isInBackwardsCompatibleMode()) {
                // treat this as a dynamic error to be reported only if the function call is executed
                XPathException err = new XPathException(msg, "XTDE1425");
                ErrorExpression exp = new ErrorExpression(err);
                setLocation(exp);
                return exp;
            }
            grumble(msg, "XPST0017");
            return null;
        }
        //  A QName constructor function must be evaluated now, while we know the namespace context
        if (fcall instanceof CastExpression &&
                fcall.getItemType() == AtomicType.QNAME &&
                arguments[0] instanceof StringLiteral) {
            try {
                AtomicValue av = CastExpression.castStringToQName(((StringLiteral)arguments[0]).getStringValue(),
                        env);
                return new Literal(av);
            } catch (XPathException e) {
                grumble(e.getMessage(), e.getErrorCodeQName());
                return null;
            }
        }
        // There are special rules for certain functions appearing in a pattern
        if (language == XSLT_PATTERN) {
            if (fcall instanceof RegexGroup) {
                return new Literal(EmptySequence.getInstance());
            } else if (fcall instanceof CurrentGroup) {
                grumble("The current-group() function cannot be used in a pattern",
                        "XTSE1060");
                return null;
            } else if (fcall instanceof CurrentGroupingKey) {
                grumble("The current-grouping-key() function cannot be used in a pattern",
                        "XTSE1070");
                return null;
            }
        }
        setLocation(fcall);
        for (Expression argument : arguments) {
            fcall.adoptChildExpression(argument);
        }

        return fcall;

    }

    /**
     * Parse an argument to a function call. Separate method so it can
     * be overridden.
     * @return the argument expression
     * @throws XPathException if there is a syntax error
     */

    protected Expression parseFunctionArgument() throws XPathException {
        return parseExprSingle();
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Routines for handling range variables
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Declare a range variable (record its existence within the parser).
     * A range variable is a variable declared within an expression, as distinct
     * from a variable declared in the context.
     *
     * @param declaration the variable declaration to be added to the stack
     * @throws XPathException if any error is encountered
     */

    public void declareRangeVariable(Binding declaration) throws XPathException {
        rangeVariables.push(declaration);
    }

    /**
     * Note when the most recently declared range variable has gone out of scope
     */

    public void undeclareRangeVariable() {
        rangeVariables.pop();
    }

    /**
     * Locate a range variable with a given name. (By "range variable", we mean a
     * variable declared within the expression where it is used.)
     *
     * @param qName identifies the name of the range variable
     * @return null if not found (this means the variable is probably a
     *     context variable); otherwise the relevant RangeVariable
     */

    protected Binding findRangeVariable(StructuredQName qName) {
        for (int v=rangeVariables.size()-1; v>=0; v--) {
            Binding b = rangeVariables.elementAt(v);
            if (b.getVariableQName().equals(qName)) {
                return b;
            }
        }
        return null;  // not an in-scope range variable
    }


    /**
	 * Make a NamespaceTest (name:*)
	 *
	 * @param nodeType integer code identifying the type of node required
	 * @param prefix the namespace prefix
	 * @throws XPathException if the namespace prefix is not declared
	 * @return the NamespaceTest, a pattern that matches all nodes in this
	 *     namespace
	 */

	public NamespaceTest makeNamespaceTest(short nodeType, String prefix)
			throws XPathException {

        String uri;
        try {
            StructuredQName qn = makeStructuredQName(prefix + ":a", "");
            uri = qn.getNamespaceURI();
        } catch (XPathException e) {
            // env.getURIForPrefix can return a dynamic error
            grumble(e.getMessage(), "XPST0081");
            return null;
        }

        return new NamespaceTest(nodeType, uri);

    }

	/**
	 * Make a LocalNameTest (*:name)
	 *
	 * @param nodeType the kind of node to be matched
	 * @param localName the requred local name
	 * @throws XPathException if the local name is invalid
	 * @return a LocalNameTest, a pattern which matches all nodes of a given
	 *     local name, regardless of namespace
	 */

	public LocalNameTest makeLocalNameTest(short nodeType, String localName)
			throws XPathException {
        if (!NameChecker.isValidNCName(localName)) {
            grumble("Local name [" + localName + "] contains invalid characters");
        }
		return new LocalNameTest(nodeType, localName);
    }

    /**
     * Set location information on an expression. At present only the line number
     * is retained. Needed mainly for XQuery.
     * @param exp the expression whose location information is to be set
     */

    public void setLocation(Expression exp) {
        if (exp.getContainer() == null) {
            exp.setContainer(defaultContainer);
        }
    }

    public static class ForClause {

        public Assignation rangeVariable;
        public Expression sequence;
        public SequenceType requiredType;
        public int offset;

        public int numberOfRangeVariables() {
            return 1;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
