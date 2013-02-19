package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Implement the XSLT current() function
 */

public class Current extends SystemFunction {

    public Current newInstance() {
        return new Current();
    }
    
    /**
     * The name of the Current function
     */ 
    
    public static final StructuredQName FN_CURRENT =
            new StructuredQName("", NamespaceConstant.FN, "current");

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        return StaticProperty.CONTEXT_DOCUMENT_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.ORDERED_NODESET |
                StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        throw new AssertionError("current() function should have been rewritten at compile time");
        // We rely on the expression being statically rewritten so that current() is promoted to the top level.
        //return c.getContextItem();
        //return c.getCurrentStylesheetItem();
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CURRENT_ITEM | StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
        // the expression will be replaced by a local variable, so record the dependency now
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
