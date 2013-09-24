package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.Id;
import client.net.sf.saxon.ce.functions.KeyFn;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Parser for XSLT patterns. This is created by overriding selected parts of the standard ExpressionParser.
 */

public class PatternParser extends ExpressionParser {

    int inPredicate = 0;

    /**
     * Parse a string representing an XSLT pattern
     * @param pattern the pattern expressed as a String
     * @param env     the static context for the pattern
     * @return a Pattern object representing the result of parsing
     * @throws XPathException if the pattern contains a syntax error
     */

    public Pattern parsePattern(String pattern, StaticContext env) throws XPathException {
        this.env = env;
        language = XSLT_PATTERN;
        Expression exp = parse(pattern, 0, Token.EOF, env);
        exp.setContainer(defaultContainer);
        ExpressionVisitor visitor = ExpressionVisitor.make(env, exp.getExecutable());
        return Pattern.fromExpression(exp.simplify(visitor), env.getConfiguration());
    }


    public Expression parseExpression() throws XPathException {
        if (inPredicate > 0) {
            return super.parseExpression();
        } else {
            // TODO - disallow "union" as synonym for "|" in patterns
            return parseBinaryExpression(parsePathExpression(), 10);
        }
    }

//    protected Expression parseUnionExpression() throws XPathException {
//        if (inPredicate > 0) {
//            return parseBinaryExpression(parse10);
//        } else {
//            Expression exp = parsePathExpression();
//            while (t.currentToken == Token.UNION) {
//                if (t.currentTokenValue.equals("union") && !env.getXPathLanguageLevel().equals(DecimalValue.THREE)) {
//                    grumble("Union operator in an XSLT 2.0 pattern must be written as '|'");
//                }
//                nextToken();
//                exp = new VennExpression(exp, Token.UNION, parsePathExpression());
//                setLocation(exp);
//            }
//            return exp;
//        }
//    }

    /**
     * Parse a basic step expression (without the predicates)
     * @param firstInPattern true only if we are parsing the first step in a
     *                       RelativePathPattern in the XSLT Pattern syntax
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    protected Expression parseBasicStep(boolean firstInPattern) throws XPathException {
        if (inPredicate > 0) {
            return super.parseBasicStep(firstInPattern);
        } else {
            switch (t.currentToken) {
                case Token.LPAR:
                case Token.STRING_LITERAL:
                case Token.NUMBER:
                    grumble("Token " + currentTokenDisplay() + " not allowed here in an XSLT pattern");
                    return null;
                case Token.FUNCTION:
                    if (!firstInPattern) {
                        grumble("In an XSLT pattern, a function call is allowed only as the first step in a path");
                    }
                    return super.parseBasicStep(firstInPattern);
                default:
                    return super.parseBasicStep(firstInPattern);

            }
        }
    }

    protected Expression parsePredicate() throws XPathException {
        ++inPredicate;
        Expression exp = parseExpression();
        --inPredicate;
        return exp;
    }

    protected Expression parseFunctionCall() throws XPathException {
        Expression fn = super.parseFunctionCall();
        if (inPredicate > 0) {
            return fn;
        } else {
            if (fn instanceof Id) {
                // Only one argument allowed, which must be a string literal or variable reference
                if (((Id)fn).getNumberOfArguments() != 1) {
                    grumble("id() in an XSLT 2.0 pattern must have only one argument");
                } else {
                    Expression arg = ((Id)fn).getArguments()[0];
                    if (!(arg instanceof VariableReference || arg instanceof StringLiteral)) {
                         grumble("Argument to id() in a pattern must be a variable reference or string literal");
                    }
                }
            } else if (fn instanceof KeyFn) {
                // Only two arguments allowed
                if (((KeyFn)fn).getNumberOfArguments() != 2) {
                    grumble("key() in an XSLT 2.0 pattern must have exactly two arguments");
                } else {
                    Expression arg0 = ((KeyFn)fn).getArguments()[0];
                    if (!(arg0 instanceof StringLiteral)) {
                         grumble("First argument to key() in an XSLT 2.0 pattern must be a string literal");
                    }
                    Expression arg1 = ((KeyFn)fn).getArguments()[1];
                    if (!(arg1 instanceof VariableReference || arg1 instanceof Literal)) {
                         grumble("Second argument to id() in an XSLT 2.0 pattern must be a variable reference or literal");
                    }
                }
            } else if (fn instanceof IXSLFunction) {
            	return fn; //for ixsl:window() pattern match
            } else {
                grumble("The " + fn.toString() + " function is not allowed at the head of a pattern");
            }
        }
        return fn;
    }

    protected Expression parseFunctionArgument() throws XPathException {
        if (inPredicate > 0) {
            return super.parseFunctionArgument();
        } else {
            switch(t.currentToken) {
                case Token.DOLLAR:
                    return parseVariableReference();

                case Token.STRING_LITERAL:
                    return parseStringLiteral();

                case Token.NUMBER:
                    return parseNumericLiteral();

                default:
                    grumble("A function argument in an XSLT pattern must be a variable reference or literal");
                    return null;
            }
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


