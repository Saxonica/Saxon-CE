package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.AttributeCreator;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.SetAttribute;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Extension element for Saxon client edition: ixsl:set-attribute
 */
public class IXSLSetAttribute extends XSLAttribute {

    @Override
    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        // We compile the instruction exactly as if it were an xsl:attribute instruction, to create
        // an AttributeCreator instruction; then we wrap this in a SetAttribute instruction to modify its effect
        Expression constructor = super.compile(exec, decl);
        return new SetAttribute((AttributeCreator)constructor, SetAttribute.SET);
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.