package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.functions.DocumentFn;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.DocumentURI;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.DocumentImpl;
import client.net.sf.saxon.ce.tree.linked.ElementImpl;
import client.net.sf.saxon.ce.tree.util.URI;


/**
* Class to represent xsl:include or xsl:import element in the stylesheet. <br>
* The xsl:include and xsl:import elements have mandatory attribute href
*/

public class XSLGeneralIncorporate extends StyleElement {

    private String href;

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
        href = (String)checkAttribute("href", "w1");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        validateInstruction();
    }

    public void validateInstruction() throws XPathException {
        checkEmpty();
        checkTopLevel(getLocalPart().equals("import") ? "XTSE0190" : "XTSE0170");
    }

    /**
     * Get the included or imported stylesheet module
     * @param importer the module that requested the include or import (used to check for cycles)
     * @param precedence the import precedence to be allocated to the included or imported module
     * @return the xsl:stylesheet element at the root of the included/imported module
     * @throws XPathException if any failure occurs
     */

    public StylesheetModule getIncludedStylesheet(StylesheetModule importer, int precedence)
                 throws XPathException {

        if (href==null) {
            // error already reported
            return null;
        }

        //checkEmpty();
        //checkTopLevel((this instanceof XSLInclude ? "XTSE0170" : "XTSE0190"));

        try {
            PrincipalStylesheetModule psm = importer.getPrincipalStylesheetModule();
            Executable pss = psm.getExecutable();
            XSLStylesheet includedSheet;
            StylesheetModule incModule;

            DocumentURI key = DocumentFn.computeDocumentKey(href, getBaseURI());
            includedSheet = psm.getStylesheetDocument(key);
            if (includedSheet != null) {
                // we already have the stylesheet document in cache; but we need to create a new module,
                // because the import precedence might be different. See test impincl30.
                incModule = new StylesheetModule(includedSheet, precedence);
                incModule.setImporter(importer);

            } else {

                //System.err.println("GeneralIncorporate: href=" + href + " base=" + getBaseURI());
                String relative = href;
                String fragment = null;
                int hash = relative.indexOf('#');
                if (hash == 0 || relative.length() == 0) {
                    reportCycle();
                    return null;
                } else if (hash == relative.length() - 1) {
                    relative = relative.substring(0, hash);
                } else if (hash > 0) {
                    if (hash+1 < relative.length()) {
                        fragment = relative.substring(hash+1);
                    }
                    relative = relative.substring(0, hash);
                }

                String source;
                try {
                    URI base = new URI(getBaseURI());
                    URI abs = base.resolve(relative);
                    source = abs.toString();
                } catch (URI.URISyntaxException e) {
                    throw new XPathException(e);
                }

                // check for recursion

                StylesheetModule anc = importer;

                if (source != null) {
                    while(anc!=null) {
                        if (source.equals(anc.getSourceElement().getSystemId())) {
                            reportCycle();
                            return null;
                        }
                        anc = anc.getImporter();
                    }
                }

                DocumentInfo rawDoc = getConfiguration().buildDocument(source);
                getConfiguration().getDocumentPool().add(rawDoc, key);
                DocumentImpl includedDoc = pss.loadStylesheetModule(rawDoc);

                // allow the included document to use "Literal Result Element as Stylesheet" syntax

                ElementImpl outermost = includedDoc.getDocumentElement();

                if (outermost instanceof LiteralResultElement) {
                    includedDoc = ((LiteralResultElement)outermost)
                            .makeStylesheet(getPreparedStylesheet());
                    outermost = includedDoc.getDocumentElement();
                }

                if (!(outermost instanceof XSLStylesheet)) {
                    compileError("Included document " + href + " is not a stylesheet", "XTSE0165");
                    return null;
                }
                includedSheet = (XSLStylesheet)outermost;
                includedSheet.setPrincipalStylesheetModule(psm);
                psm.putStylesheetDocument(key, includedSheet);

                incModule = new StylesheetModule(includedSheet, precedence);
                incModule.setImporter(importer);
                Declaration decl = new Declaration(incModule, includedSheet);
                includedSheet.validate(decl);

                if (includedSheet.validationError!=null) {
                    if (reportingCircumstances == REPORT_ALWAYS) {
                        includedSheet.compileError(includedSheet.validationError);
                    } else if (includedSheet.reportingCircumstances == REPORT_UNLESS_FORWARDS_COMPATIBLE
                        // not sure if this can still happen
                                  /*&& !incSheet.forwardsCompatibleModeIsEnabled()*/) {
                        includedSheet.compileError(includedSheet.validationError);
                    }
                }
            }

            incModule.spliceIncludes();          // resolve any nested imports and includes;

            // Check the consistency of input-type-annotations
            //assert thisSheet != null;
            importer.setInputTypeAnnotations(includedSheet.getInputTypeAnnotationsAttribute() |
                    incModule.getInputTypeAnnotations());

            return incModule;

        } catch (XPathException err) {
            err.setErrorCode("XTSE0165");
            err.setIsStaticError(true);
            compileError(err);
            return null;
        }
    }

    private void reportCycle() throws XPathException {
        compileError("A stylesheet cannot " + getLocalPart() + " itself",
                        (getLocalPart().equals("include") ? "XTSE0180" : "XTSE0210"));
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
        // no action. The node will never be compiled, because it replaces itself
        // by the contents of the included file.
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
