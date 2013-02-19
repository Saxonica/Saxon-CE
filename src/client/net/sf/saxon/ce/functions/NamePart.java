package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.DocumentPool;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AnyURIValue;
import client.net.sf.saxon.ce.value.QNameValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
* This class supports the name(), local-name(), and namespace-uri() functions
* from XPath 1.0, and also the XSLT generate-id() function
*/

public class NamePart extends SystemFunction {

    public NamePart(int operation) {
        this.operation = operation;
    }

    public NamePart newInstance() {
        return new NamePart(operation);
    }

    public static final int NAME = 0;
    public static final int LOCAL_NAME = 1;
    public static final int NAMESPACE_URI = 2;
    public static final int GENERATE_ID = 3;
    public static final int DOCUMENT_URI = 4;
    public static final int NODE_NAME = 6;

    /**
    * Simplify and validate.
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault();
        return simplifyArguments(visitor);
    }

    /**
     * Determine the special properties of this expression. The generate-id()
     * function is a special case: it is considered creative if its operand
     * is creative, so that generate-id(f()) is not taken out of a loop
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (operation == GENERATE_ID) {
            return p & ~StaticProperty.NON_CREATIVE;
        } else {
            return p;
        }
    }

    @Override
    public int computeDependencies() {
        return super.computeDependencies();
    }

    /**
    * Evaluate the function in a string context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        if (node==null) {
            // Effect of supplying an empty sequence as the argument differs depending on the function
            if (operation == NODE_NAME || operation == DOCUMENT_URI ) {
                return null;
            } else if (operation == NAMESPACE_URI) {
                return AnyURIValue.EMPTY_URI;
            } else {
                return StringValue.EMPTY_STRING;
            }
        }

        String s;
        switch (operation) {
            case NAME:
                s = node.getDisplayName();
                break;
            case LOCAL_NAME:
                s = node.getLocalPart();
                break;
            case NAMESPACE_URI:
                String uri = node.getURI();
                s = (uri==null ? "" : uri);
                        // null should no longer be returned, but the spec has changed, so it's
                        // better to be defensive
                return new AnyURIValue(s);

            case GENERATE_ID:
                FastStringBuffer buffer = new FastStringBuffer(FastStringBuffer.TINY);
                node.generateId(buffer);
                buffer.condense();
                return new StringValue(buffer);

            case DOCUMENT_URI:
                // If the node is in the document pool, get the URI under which it is registered.
                // Otherwise, return its systemId. 
                return getDocumentURI(node, c);
            case NODE_NAME:
                int nc = node.getNameCode();
                if (nc == -1) {
                    return null;
                }
                return new QNameValue(node.getNamePool(), nc);
            default:
                throw new UnsupportedOperationException("Unknown name operation");
        }
        return new StringValue(s);
    }

    public static AnyURIValue getDocumentURI(NodeInfo node, XPathContext c) {
        if (node.getNodeKind() == Type.DOCUMENT) {
            DocumentPool pool = c.getController().getDocumentPool();
            String docURI = pool.getDocumentURI(node);
            if (docURI == null) {
                docURI = node.getSystemId();
            }
            if (docURI == null) {
                return null;
            } else if ("".equals(docURI)) {
                return null;
            } else {
                return new AnyURIValue(docURI);
            }
        } else {
            return null;
        }
    }

    /**
     * Test whether an expression is a call on the generate-id() function
     * @param exp the expression to be tested
     * @return true if exp is a call on generate-id(), else false
     */

    public static boolean isGenerateIdFunction(Expression exp) {
        return ((exp instanceof NamePart) && ((NamePart)exp).operation == GENERATE_ID);
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
