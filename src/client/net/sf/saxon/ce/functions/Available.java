package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.PreparedStylesheet;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.SchemaType;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;
import client.net.sf.saxon.ce.value.NumericValue;
import client.net.sf.saxon.ce.value.StringValue;

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

    private NamespaceResolver nsContext;
    private transient boolean checked = false;



    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        // the second time checkArguments is called, it's a global check so the static context is inaccurate
        if (checked) {
            return;
        }
        checked = true;
        super.checkArguments(visitor);
        if (!(argument[0] instanceof Literal &&
                (argument.length==1 || argument[1] instanceof Literal))) {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
        }
    }

    /**
    * preEvaluate: this method uses the static context to do early evaluation of the function
    * if the argument is known (which is the normal case)
     * @param visitor the expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        String lexicalQName = ((Literal)argument[0]).getValue().getStringValue();
        StaticContext env = visitor.getStaticContext();
        boolean b = false;
        Configuration config = visitor.getConfiguration();
        switch(operation) {
            case ELEMENT_AVAILABLE:
                b = env.isElementAvailable(lexicalQName);
                break;
            case FUNCTION_AVAILABLE:
                long arity = -1;
                if (argument.length == 2) {
                    arity = ((NumericValue)argument[1].evaluateItem(env.makeEarlyEvaluationContext())).intValue();
                }
                try {
                    String[] parts = NameChecker.getQNameParts(lexicalQName);
                    String prefix = parts[0];
                    String uri;
                    if (prefix.length() == 0) {
                        uri = env.getDefaultFunctionNamespace();
                    } else {
                        uri = env.getURIForPrefix(prefix);
                    }
                    StructuredQName functionName = new StructuredQName(prefix, uri, parts[1]);
                    b = (env.getFunctionLibrary().hasFunctionSignature(functionName, (int)arity));
                } catch (QNameException e) {
                    XPathException err = new XPathException(e.getMessage());
                    err.setErrorCode("XTDE1400");
                    throw err;
                } catch (XPathException e2) {
                    if ("XTDE0290".equals(e2.getErrorCodeLocalPart())) {
                        e2.setErrorCode("XTDE1400");
                    }
                    throw e2;
                }
                break;
            case TYPE_AVAILABLE:
                try {
                    String[] parts = NameChecker.getQNameParts(lexicalQName);
                    String prefix = parts[0];
                    String uri;
                    if (prefix.length() == 0) {
                        uri = env.getDefaultElementNamespace();
                    } else {
                        uri = env.getURIForPrefix(prefix);
                    }

                    int fingerprint = config.getNamePool().allocate(prefix, uri, parts[1]) & 0xfffff;
                    SchemaType type = config.getSchemaType(fingerprint);
                    b = type instanceof BuiltInAtomicType;
                } catch (QNameException e) {
                    XPathException err = new XPathException(e.getMessage());
                    err.setErrorCode("XTDE1425");
                    throw err;
                }
        }
        return Literal.makeLiteral(BooleanValue.get(b));
    }

    /**
     * Run-time evaluation. This is the only thing in the spec that requires information
     * about in-scope functions to be available at run-time.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        long arity = -1;
        if (argument.length == 2) {
            arity = ((NumericValue)argument[1].evaluateItem(context)).intValue();
        }
        StringValue nameValue = (StringValue)av1;
        String lexicalName = nameValue.getStringValue();
        StructuredQName qName;
        try {
            if (lexicalName.indexOf(':') < 0) {
                // we're in XSLT, where the default namespace for functions can't be changed
                String uri = (operation == FUNCTION_AVAILABLE
                        ? NamespaceConstant.FN
                        : nsContext.getURIForPrefix("", true));
                qName = new StructuredQName("", uri, lexicalName);
            } else {
                qName = StructuredQName.fromLexicalQName(lexicalName,
                    false,
                    nsContext);
            }
        } catch (XPathException e) {
            dynamicError(e.getMessage(), badQNameCode(), context);
            return null;
        }

        boolean b = false;
        switch(operation) {
            case ELEMENT_AVAILABLE:
                b = isElementAvailable(qName.getNamespaceURI(), qName.getLocalName(), context);
                break;
            case FUNCTION_AVAILABLE:
                final FunctionLibrary lib = context.getController().getExecutable().getFunctionLibrary();
                b = (lib.hasFunctionSignature(qName, (int)arity));
                break;
            case TYPE_AVAILABLE:
                final int fp = context.getNamePool().allocate(
                        qName.getPrefix(), qName.getNamespaceURI(), qName.getLocalName()) & 0xfffff;
                SchemaType type = context.getConfiguration().getSchemaType(fp);
                b = (type != null);

        }
        return BooleanValue.get(b);

    }

    private String badQNameCode() {
        switch (operation) {
            case FUNCTION_AVAILABLE:
                return "XTDE1400";
            case TYPE_AVAILABLE:
                return "XTDE1428";
            case ELEMENT_AVAILABLE:
                return "XTDE1440";
            default:
                return null;
        }
    }

    /**
     * Determine at run-time whether a particular instruction is available. Returns true
     * only in the case of XSLT instructions and Saxon extension instructions; returns false
     * for user-defined extension instructions
     * @param uri the namespace URI of the element
     * @param localname the local part of the element name
     * @param context the XPath evaluation context
     * @return true if the instruction is available, in the sense of the XSLT element-available() function
    */

    private boolean isElementAvailable(String uri, String localname, XPathContext context) {

        // This is horribly inefficient. But hopefully it's hardly ever executed, because there
        // is very little point calling element-available() with a dynamically-constructed argument.
        // And the inefficiency is only incurred once, on the first call.

        // Note: this requires the compile-time classes to be available at run-time; it will need
        // changing if we ever want to build a run-time JAR file.

        try {
            PreparedStylesheet pss = (PreparedStylesheet)context.getController().getExecutable();
            return pss.getStyleNodeFactory().isElementAvailable(uri, localname);
        } catch (Exception err) {
            //err.printStackTrace();
            return false;
        }
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
