package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.Concat;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
* This class represents an attribute value template. The class allows an AVT to be parsed, and
* can construct an Expression that returns the effective value of the AVT.
*
* This is an abstract class that is never instantiated, it contains static methods only.
*/

public abstract class AttributeValueTemplate {

    private AttributeValueTemplate() {}


    /**
     * Static factory method to create an AVT from an XSLT string representation.
    */

    public static Expression make(String avt,
                                  SourceLocator sourceLocator,
                                  StaticContext env) throws XPathException {

        List components = new ArrayList(5);

        int i0, i1, i8, i9;
        int len = avt.length();
        int last = 0;
        ExpressionVisitor visitor = ExpressionVisitor.make(env, null);
        while (last < len) {

            i0 = avt.indexOf("{", last);
            i1 = avt.indexOf("{{", last);
            i8 = avt.indexOf("}", last);
            i9 = avt.indexOf("}}", last);

            if ((i0 < 0 || len < i0) && (i8 < 0 || len < i8)) {   // found end of string
                addStringComponent(components, avt, last, len);
                break;
            } else if (i8 >= 0 && (i0 < 0 || i8 < i0)) {             // found a "}"
                if (i8 != i9) {                        // a "}" that isn't a "}}"
                    XPathException err = new XPathException("Closing curly brace in attribute value template \"" + avt.substring(0, len) + "\" must be doubled");
                    err.setErrorCode("XTSE0370");
                    err.setIsStaticError(true);
                    throw err;
                }
                addStringComponent(components, avt, last, i8 + 1);
                last = i8 + 2;
            } else if (i1 >= 0 && i1 == i0) {              // found a doubled "{{"
                addStringComponent(components, avt, last, i1 + 1);
                last = i1 + 2;
            } else if (i0 >= 0) {                        // found a single "{"
                if (i0 > last) {
                    addStringComponent(components, avt, last, i0);
                }
                Expression exp;
                ExpressionParser parser = new ExpressionParser();
                parser.setDefaultContainer(((ExpressionContext)env).getStyleElement());

                parser.setLanguage(ExpressionParser.XPATH);
                exp = parser.parse(avt, i0 + 1, Token.RCURLY, env);
                exp = visitor.simplify(exp);
                last = parser.getTokenizer().currentTokenStartOffset + 1;

                if (env.isInBackwardsCompatibleMode()) {
                    components.add(makeFirstItem(exp));
                } else {
                    components.add(visitor.simplify(
                            XSLLeafNodeConstructor.makeSimpleContentConstructor(
                                    exp,
                                    new StringLiteral(StringValue.SINGLE_SPACE))));
                }

            } else {
                throw new IllegalStateException("Internal error parsing AVT");
            }
        }

        // is it empty?

        if (components.size() == 0) {
            return new StringLiteral(StringValue.EMPTY_STRING);
        }

        // is it a single component?

        if (components.size() == 1) {
            return visitor.simplify((Expression) components.get(0));
        }

        // otherwise, return an expression that concatenates the components
        
        Expression[] args = new Expression[components.size()];
        components.toArray(args);
        Concat fn = (Concat) SystemFunction.makeSystemFunction("concat", args);
        fn.setSourceLocator(sourceLocator);
        return visitor.simplify(fn);

    }

    private static void addStringComponent(List components, String avt, int start, int end) {
        if (start < end) {
            components.add(new StringLiteral(avt.substring(start, end)));
        }
    }

    /**
    * Make an expression that extracts the first item of a sequence, after atomization
    */

    public static Expression makeFirstItem(Expression exp) {
        final TypeHierarchy th = TypeHierarchy.getInstance();
        if ((!(exp.getItemType() instanceof AtomicType))) {
            exp = new Atomizer(exp);
        }
        if (Cardinality.allowsMany(exp.getCardinality())) {
            exp = new FirstItemExpression(exp);
        }
        if (!th.isSubType(exp.getItemType(), AtomicType.STRING)) {
            exp = new AtomicSequenceConverter(exp, AtomicType.STRING);
        }
        return exp;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
