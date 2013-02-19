package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* This element is a surrogate for an extension element (or indeed an xsl element)
* for which no implementation is available.
*/

public class AbsentExtensionElement extends StyleElement {

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Process the attributes of this element and all its children
     */

    public void processAllAttributes() throws XPathException {
        if (isTopLevel() && forwardsCompatibleModeIsEnabled()) {
            // do nothing
        } else {
            super.processAllAttributes();
        }
    }

    public void prepareAttributes() throws XPathException {
    }

    /**
     * Recursive walk through the stylesheet to validate all nodes
     * @param decl
     */

    public void validateSubtree(Declaration decl) throws XPathException {
        if (isTopLevel() && forwardsCompatibleModeIsEnabled()) {
            // do nothing
        } else {
            super.validateSubtree(decl);
        }
    }

    public void validate(Declaration decl) throws XPathException {
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        if (isTopLevel()) {
            return null;
        }

        // if there are fallback children, compile the code for the fallback elements

        if (validationError==null) {
            validationError = new XPathException("Unknown instruction");
        }
        return fallbackProcessing(exec, decl, this);
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
