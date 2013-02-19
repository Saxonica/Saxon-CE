package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.event.Stripper;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.trans.StripSpaceRules;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.tree.util.StringTokenizer;

/**
* An xsl:preserve-space or xsl:strip-space elements in stylesheet. <br>
*/

public class XSLPreserveSpace extends StyleElement {

    private String elements;

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

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.ELEMENTS)) {
        		elements = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (elements==null) {
            reportAbsence("elements");
            elements="*";   // for error recovery
        }
    }

    public void validate(Declaration decl) throws XPathException {
        checkEmpty();
        checkTopLevel(null);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException
    {
        Template preserve =
                (getFingerprint() == StandardNames.XSL_PRESERVE_SPACE ? Stripper.PRESERVE : Stripper.STRIP);
        StripSpaceRules stripperRules = getPrincipalStylesheetModule().getStripperRules();

        // elements is a space-separated list of element names

        StringTokenizer st = new StringTokenizer(elements, " \t\n\r", false);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            NodeTest nt;
            if (s.equals("*")) {
                nt = NodeKindTest.ELEMENT;
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());

            } else if (s.endsWith(":*")) {
                if (s.length()==2) {
                    compileError("No prefix before ':*'");
                }
                String prefix = s.substring(0, s.length()-2);
                String uri = getURIForPrefix(prefix, false);
                nt = new NamespaceTest(getNamePool(), Type.ELEMENT, uri);
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());

            } else if (s.startsWith("*:")) {
                if (s.length()==2) {
                    compileError("No local name after '*:'");
                }
                String localname = s.substring(2);
                nt = new LocalNameTest(getNamePool(), Type.ELEMENT, localname);
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());

            } else {
                String prefix;
                String localName;
                String uri;
                try {
                    String[] parts = NameChecker.getQNameParts(s);
                    prefix = parts[0];
                    if (parts[0].equals("")) {
                        uri = getDefaultXPathNamespace();
                    } else {
                        uri = getURIForPrefix(prefix, false);
                        if (uri == null) {
                            undeclaredNamespaceError(prefix, "XTSE0280");
                            return null;
                        }
                    }
                    localName = parts[1];
                } catch (QNameException err) {
                    compileError("Element name " + s + " is not a valid QName", "XTSE0280");
                    return null;
                }
                NamePool target = getNamePool();
                int nameCode = target.allocate("", uri, localName);
                nt = new NameTest(Type.ELEMENT, nameCode, getNamePool());
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());
            }

        }
        return null;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
