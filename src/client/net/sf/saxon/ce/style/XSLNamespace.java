package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.NamespaceConstructor;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.StringValue;

/**
* An xsl:namespace element in the stylesheet. (XSLT 2.0)
*/

public class XSLNamespace extends XSLLeafNodeConstructor {

    Expression name;

    public void prepareAttributes() throws XPathException {
        name = prepareAttributesNameAndSelect();
    }
    
    public void validate(Declaration decl) throws XPathException {
        name = typeCheck(name);
        select = typeCheck(select);
        int countChildren = 0;
        NodeInfo firstChild = null;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLFallback) {
                continue;
            }
            if (select != null) {
                String errorCode = getErrorCodeForSelectPlusContent();
                compileError("An " + getDisplayName() + " element with a select attribute must be empty", errorCode);
            }
            countChildren++;
            if (firstChild == null) {
                firstChild = child;
            } else {
                break;
            }
        }

        if (select == null) {
            if (countChildren == 0) {
                // there are no child nodes and no select attribute
                select = new StringLiteral(StringValue.EMPTY_STRING);
            } else if (countChildren == 1) {
                // there is exactly one child node
                if (firstChild.getNodeKind() == Type.TEXT) {
                    // it is a text node: optimize for this case
                    select = new StringLiteral(firstChild.getStringValue());
                }
            }
        }
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0910";
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        NamespaceConstructor inst = new NamespaceConstructor(name);
        compileContent(exec, decl, inst, new StringLiteral(StringValue.SINGLE_SPACE));
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
