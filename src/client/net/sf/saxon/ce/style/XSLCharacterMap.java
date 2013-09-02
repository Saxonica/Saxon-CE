package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.value.Whitespace;

/**
* An xsl:character-map declaration in the stylesheet. <br>
*/

public class XSLCharacterMap extends StyleElement {

    String use;
                // the value of the use-character-maps attribute, as supplied

    boolean validated = false;
                // set to true once validate() has been called


    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
     * Get the fingerprint of the name of this character map
     * @return the fingerprint value
     */

    public StructuredQName getCharacterMapName() {
        StructuredQName name = getObjectName();
        if (name == null) {
            try {
                return makeQName(getAttributeValue("", "name"));
            } catch (Exception err) {
                // the error will be reported later
                return new StructuredQName("", "", "unnamedCharacterMap_" + hashCode());
            }
        }
        return name;
    }

    /**
     * Validate the attributes on this instruction
     * @throws XPathException
     */

    public void prepareAttributes() throws XPathException {

		String name = null;
		use = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals("name")) {
        		name = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("use-character-maps")) {
        		use = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (name==null) {
            reportAbsence("name");
            name = "unnamedCharacterMap_" + hashCode();
        }

        try {
            setObjectName(makeQName(name));
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
            name = "unnamedCharacterMap_" + hashCode();
            setObjectName(new StructuredQName("", "", name));
        } catch (XPathException err) {
            compileError(err.getMessage(), "XTSE0020");
            name = "unnamedCharacterMap_" + hashCode();
            setObjectName(new StructuredQName("", "", name));
        }

    }

    public void validate(Declaration decl) throws XPathException {

        if (validated) return;

        // check that this is a top-level declaration

        checkTopLevel(null);

        // check that the only children are xsl:output-character elements

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            Item child = kids.next();
            if (child == null) {
                break;
            }
            if (!(child instanceof XSLOutputCharacter)) {
                compileError("Only xsl:output-character is allowed within xsl:character-map", "XTSE0010");
            }
        }

        validated = true;
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.