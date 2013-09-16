package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.event.Stripper;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.om.InscopeNamespaceResolver;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.trans.StripSpaceRules;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.Whitespace;

/**
* An xsl:preserve-space or xsl:strip-space elements in stylesheet. <br>
*/

public class XSLPreserveSpace extends StyleElement {

    private String elements = "*";

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
        elements = (String)checkAttribute("elements", "s1");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        checkEmpty();
        checkTopLevel(null);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException
    {
        Template preserve =
                (getLocalPart().equals("preserve-space") ? Stripper.PRESERVE : Stripper.STRIP);
        StripSpaceRules stripperRules = getExecutable().getStripperRules();

        // elements is a space-separated list of element names

        NamespaceResolver resolver = new InscopeNamespaceResolver(this);
        for (String s : Whitespace.tokenize(elements)) {
            NodeTest nt;
            if (s.equals("*")) {
                nt = NodeKindTest.ELEMENT;
                stripperRules.addRule(nt, preserve, decl.getModule());

            } else if (s.endsWith(":*")) {
                if (s.length()==2) {
                    compileError("No prefix before ':*'");
                }
                String prefix = s.substring(0, s.length()-2);
                String uri = resolver.getURIForPrefix(prefix, false);
                nt = new NamespaceTest(Type.ELEMENT, uri);
                stripperRules.addRule(nt, preserve, decl.getModule());

            } else if (s.startsWith("*:")) {
                if (s.length()==2) {
                    compileError("No local name after '*:'");
                }
                String localname = s.substring(2);
                nt = new LocalNameTest(Type.ELEMENT, localname);
                stripperRules.addRule(nt, preserve, decl.getModule());

            } else {
                StructuredQName qn;
                try {
                    qn = StructuredQName.fromLexicalQName(s, getDefaultXPathNamespace(), resolver);
                } catch (XPathException err) {
                    compileError("Element name " + s + " is not a valid QName", "XTSE0280");
                    return null;
                }
                nt = new NameTest(Type.ELEMENT, qn);
                stripperRules.addRule(nt, preserve, decl.getModule());
            }

        }
        return null;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
