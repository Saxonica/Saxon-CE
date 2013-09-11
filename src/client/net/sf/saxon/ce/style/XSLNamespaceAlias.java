package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.InscopeNamespaceResolver;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.trans.XPathException;


/**
* An xsl:namespace-alias element in the stylesheet. <br>
*/

public class XSLNamespaceAlias extends StyleElement {

    private String stylesheetURI;
    private NamespaceBinding resultNamespaceBinding;

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

	    String stylesheetPrefix = (String)checkAttribute("stylesheet-prefix", "w1");
	    String resultPrefix = (String)checkAttribute("result-prefix", "w1");
        checkForUnknownAttributes();

        if (stylesheetPrefix.equals("#default")) {
            stylesheetPrefix="";
        }
        if (resultPrefix.equals("#default")) {
            resultPrefix="";
        }
        NamespaceResolver resolver = new InscopeNamespaceResolver(this);
        stylesheetURI = resolver.getURIForPrefix(stylesheetPrefix, true);
        if (stylesheetURI == null) {
            compileError("stylesheet-prefix " + stylesheetPrefix + " has not been declared", "XTSE0812");
            // recovery action
            stylesheetURI = "";
            resultNamespaceBinding = NamespaceBinding.DEFAULT_UNDECLARATION;
            return;
        }
        String resultURI = resolver.getURIForPrefix(resultPrefix, true);
        if (resultURI == null) {
            compileError("result-prefix " + resultPrefix + " has not been declared", "XTSE0812");
            // recovery action
            stylesheetURI = "";
            resultURI = "";
        }
        resultNamespaceBinding = new NamespaceBinding(resultPrefix, resultURI);
    }

    public void validate(Declaration decl) throws XPathException {
        checkTopLevel(null);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        top.addNamespaceAlias(decl);
    }

    public String getStylesheetURI() {
        return stylesheetURI;
    }

    public NamespaceBinding getResultNamespaceBinding() {
        return resultNamespaceBinding;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
