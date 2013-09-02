package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.value.Whitespace;
import client.net.sf.saxon.ce.type.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * A stylesheet module represents a module of a stylesheet. It is possible for two modules
 * to share the same stylesheet tree in the case where two includes or imports reference
 * the same URI; in this case the two modules will typically have a different import precedence.
 */
public class StylesheetModule {

    private XSLStylesheet sourceElement;
    private int precedence;
    private int minImportPrecedence;
    private StylesheetModule importer;
    boolean wasIncluded;


    // the value of the inputTypeAnnotations attribute on this module, combined with the values
    // on all imported/included modules. This is a combination of the bit-significant values
    // ANNOTATION_STRIP and ANNOTATION_PRESERVE.
    private int inputTypeAnnotations = 0;

    // A list of all the declarations in the stylesheet and its descendants, in increasing precedence order
    protected List<Declaration> topLevel = new ArrayList<Declaration>();

    public StylesheetModule(XSLStylesheet sourceElement, int precedence) {
        this.sourceElement = sourceElement;
        this.precedence = precedence;
    }

    public void setImporter(StylesheetModule importer) {
        this.importer = importer;
    }

    public StylesheetModule getImporter() {
        return importer;
    }

    public PrincipalStylesheetModule getPrincipalStylesheetModule() {
        return importer.getPrincipalStylesheetModule();
    }

    public XSLStylesheet getSourceElement() {
        return sourceElement;
    }

    public int getPrecedence() {
        return (wasIncluded ? importer.getPrecedence() : precedence);    }

    /**
     * Indicate that this stylesheet was included (by its "importer") using an xsl:include
     * statement as distinct from xsl:import
     */

    public void setWasIncluded() {
        wasIncluded = true;
    }

    /**
     * Set the minimum import precedence of this module, that is, the lowest import precedence of the modules
     * that it imports. This information is used to decide which template rules are eligible for consideration
     * by xsl:apply-imports
     * @param min the minimum import precedence
     */

    public void setMinImportPrecedence(int min) {
        this.minImportPrecedence = min;
    }

    /**
     * Get the minimum import precedence of this module, that is, the lowest import precedence of the modules
     * that it imports. This information is used to decide which template rules are eligible for consideration
     * by xsl:apply-imports
     * @return the minimum import precedence
     */

    public int getMinImportPrecedence() {
        return this.minImportPrecedence;
    }

    /**
     * Process xsl:include and xsl:import elements.
     */

    public void spliceIncludes() throws XPathException {

        boolean foundNonImport = false;
        topLevel = new ArrayList<Declaration>(50);
        minImportPrecedence = precedence;
        StyleElement previousElement = sourceElement;

        AxisIterator kids = sourceElement.iterateAxis(Axis.CHILD);

        while (true) {
            NodeInfo child = (NodeInfo) kids.next();
            if (child == null) {
                break;
            }
            if (child.getNodeKind() == Type.TEXT) {
                // in an embedded stylesheet, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValue())) {
                    previousElement.compileError(
                            "No character data is allowed between top-level elements", "XTSE0120");
                }

            } else if (child instanceof DataElement) {
                foundNonImport = true;
            } else {
                previousElement = (StyleElement) child;
                if (child instanceof XSLGeneralIncorporate) {
                    XSLGeneralIncorporate xslinc = (XSLGeneralIncorporate) child;
                    xslinc.processAttributes();

                    if (xslinc.isImport()) {
                        if (foundNonImport) {
                            xslinc.compileError("xsl:import elements must come first", "XTSE0200");
                        }
                    } else {
                        foundNonImport = true;
                    }

                    // get the included stylesheet. This follows the URL, builds a tree, and splices
                    // in any indirectly-included stylesheets.

                    xslinc.validateInstruction();
                    int errors = sourceElement.getPreparedStylesheet().getErrorCount();
                    StylesheetModule inc =
                            xslinc.getIncludedStylesheet(this, precedence);
                    if (inc == null) {
                        return;  // error has been reported
                    }
                    errors = sourceElement.getPreparedStylesheet().getErrorCount() - errors;
                    if (errors > 0) {
                        xslinc.compileError("Reported " + errors  + (errors==1 ? " error" : " errors") +
                                " in " + (xslinc.isImport() ? "imported" : "included") +
                                " stylesheet module", "XTSE0165");
                    }

                    // after processing the imported stylesheet and any others it brought in,
                    // adjust the import precedence of this stylesheet if necessary

                    if (xslinc.isImport()) {
                        precedence = inc.getPrecedence() + 1;
                    } else {
                        precedence = inc.getPrecedence();
                        inc.setMinImportPrecedence(minImportPrecedence);
                        inc.setWasIncluded();
                    }

                    // copy the top-level elements of the included stylesheet into the top level of this
                    // stylesheet. Normally we add these elements at the end, in order, but if the precedence
                    // of an element is less than the precedence of the previous element, we promote it.
                    // This implements the requirement in the spec that when xsl:include is used to
                    // include a stylesheet, any xsl:import elements in the included document are moved
                    // up in the including document to after any xsl:import elements in the including
                    // document.

                    List<Declaration> incchildren = inc.topLevel;
                    for (int j = 0; j < incchildren.size(); j++) {
                        Declaration decl = incchildren.get(j);
                        //StyleElement elem = decl.getSourceElement();
                        int last = topLevel.size() - 1;
                        if (last < 0 || decl.getPrecedence() >= topLevel.get(last).getPrecedence()) {
                            topLevel.add(decl);
                        } else {
                            while (last >= 0 && decl.getPrecedence() < topLevel.get(last).getPrecedence()) {
                                last--;
                            }
                            topLevel.add(last + 1, decl);
                        }
                    }
                } else {
                    foundNonImport = true;
                    Declaration decl = new Declaration(this, (StyleElement)child);
                    topLevel.add(decl);
                }
            }
        }
    }

    /**
     * Get the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link XSLStylesheet#ANNOTATION_STRIP} and {@link XSLStylesheet#ANNOTATION_PRESERVE}
     * @return the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules
     */

    public int getInputTypeAnnotations() {
        return inputTypeAnnotations;
    }

    /**
     * Set the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link XSLStylesheet#ANNOTATION_STRIP} and {@link XSLStylesheet#ANNOTATION_PRESERVE}
     * @param annotations the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules.
     */

    public void setInputTypeAnnotations(int annotations) throws XPathException {
        inputTypeAnnotations |= annotations;
        if (inputTypeAnnotations == (XSLStylesheet.ANNOTATION_STRIP | XSLStylesheet.ANNOTATION_PRESERVE)) {
            getPrincipalStylesheetModule().compileError(
                    "One stylesheet module specifies input-type-annotations='strip', " +
                    "another specifies input-type-annotations='preserve'", "XTSE0265");
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


