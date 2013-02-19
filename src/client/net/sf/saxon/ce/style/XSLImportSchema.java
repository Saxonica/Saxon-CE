package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;


/**
* Compile-time representation of an xsl:import-schema declaration
 * in a stylesheet
*/

public class XSLImportSchema extends StyleElement {

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }    

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
        String namespace = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
            if (f.equals(StandardNames.SCHEMA_LOCATION)) {
        		//
            } else if (f.equals(StandardNames.NAMESPACE)) {
                namespace = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if ("".equals(namespace)) {
            compileError("The zero-length string is not a valid namespace URI. "+
                    "For a schema with no namespace, omit the namespace attribute");
        }
    }

    public void validate(Declaration decl) throws XPathException {
        checkTopLevel(null);
        compileError("This XSLT processor is not schema-aware");
    }


    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
        // No action. The effect of import-schema is compile-time only
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
