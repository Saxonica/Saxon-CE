package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* An instruction that creates an element node whose name is known statically.
 * Used for literal results elements in XSLT, for direct element constructors
 * in XQuery, and for xsl:element in cases where the name and namespace are
 * known statically.
*/

public class FixedElement extends ElementCreator {

    private StructuredQName nameCode;
    protected NamespaceBinding[] namespaceCodes = null;

    /**
     * Create an instruction that creates a new element node
     * @param nameCode Represents the name of the element node
     * @param namespaceCodes List of namespaces to be added to the element node.
     *                       May be null if none are required.
     * @param inheritNamespaces true if the children of this element are to inherit its namespaces
     */
    public FixedElement(StructuredQName nameCode,
                        NamespaceBinding[] namespaceCodes,
                        boolean inheritNamespaces) {
        this.nameCode = nameCode;
        this.namespaceCodes = namespaceCodes;
        this.inheritNamespaces = inheritNamespaces;
    }

    /**
     * Callback from the superclass ElementCreator to get the nameCode
     * for the element name
     *
     * @param context The evaluation context (not used)
     * @param copiedNode
     * @return the name code for the element name
     */

    public StructuredQName getNameCode(XPathContext context, NodeInfo copiedNode) {
        return nameCode;
    }

    public String getNewBaseURI(XPathContext context, NodeInfo copiedNode) {
        return getBaseURI();
    }


    /**
     * Callback from the superclass ElementCreator to output the namespace nodes
     * @param context The evaluation context (not used)
     * @param out The receiver to handle the output
     * @param nameCode
     * @param copiedNode
     */

    protected void outputNamespaceNodes(XPathContext context, Receiver out, StructuredQName nameCode, NodeInfo copiedNode)
    throws XPathException {
        if (namespaceCodes != null) {
            for (int i=0; i<namespaceCodes.length; i++) {
                out.namespace(namespaceCodes[i], 0);
            }
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
