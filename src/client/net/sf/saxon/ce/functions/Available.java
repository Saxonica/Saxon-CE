package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.style.StyleNodeFactory;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.NumericValue;

/**
* This class supports the XSLT element-available and function-available functions.
*/

public class Available extends SystemFunction {

    public Available(int operation) {
        this.operation = operation;
    }

    public Available newInstance() {
        return new Available(operation);
    }

    public static final int ELEMENT_AVAILABLE = 0;
    public static final int FUNCTION_AVAILABLE = 1;
    public static final int TYPE_AVAILABLE = 2;

    private StaticContext env;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        // the second time checkArguments is called, it's a global check so the static context is inaccurate
        if (env == null) {
            env = visitor.getStaticContext();
        }
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */
    @Override
    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        String lexicalQName = argument[0].evaluateAsString(context).toString();
        switch(operation) {
            case ELEMENT_AVAILABLE: {
                StructuredQName qName = StructuredQName.fromLexicalQName(
                        lexicalQName, env.getDefaultElementNamespace(), env.getNamespaceResolver());
                return new StyleNodeFactory(context.getConfiguration()).isElementAvailable(
                        qName.getNamespaceURI(), qName.getLocalName());
            }
            case FUNCTION_AVAILABLE: {
                long arity = -1;
                if (argument.length == 2) {
                    arity = ((NumericValue)argument[1].evaluateItem(context)).intValue();
                }
                try {
                    StructuredQName qName = StructuredQName.fromLexicalQName(
                        lexicalQName, env.getDefaultFunctionNamespace(), env.getNamespaceResolver());
                    return (env.getFunctionLibrary().hasFunctionSignature(qName, (int)arity));
                } catch (XPathException e2) {
                    e2.setErrorCode("XTDE1400");
                    throw e2;
                }
            }
            case TYPE_AVAILABLE: {
                try {
                    StructuredQName qName = StructuredQName.fromLexicalQName(
                        lexicalQName, env.getDefaultElementNamespace(), env.getNamespaceResolver());
                    return qName.getNamespaceURI().equals(NamespaceConstant.SCHEMA) &&
                            AtomicType.isRecognizedName(qName.getLocalName());
                } catch (XPathException e) {
                    e.setErrorCode("XTDE1425");
                    throw e;
                }
            }
            default:
                return false;
        }
    }

    /**
     * Run-time evaluation. This is the only thing in the spec that requires information
     * about in-scope functions to be available at run-time.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));

    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
