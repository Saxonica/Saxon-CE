package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.RuleManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.NodeImpl;
import client.net.sf.saxon.ce.type.Type;

/**
 * An xsl:stylesheet or xsl:transform element in the stylesheet. <br>
 * Note this element represents a stylesheet module, not necessarily
 * the whole stylesheet. However, much of the functionality (and the fields)
 * are relevant only to the top-level module.
 */

public class XSLStylesheet extends StyleElement {

    Executable exec;


    // the PrincipalStylesheetModule object
    private PrincipalStylesheetModule principalStylesheetModule;

    public static final int ANNOTATION_UNSPECIFIED = 0;
    public static final int ANNOTATION_STRIP = 1;
    public static final int ANNOTATION_PRESERVE = 2;


    // default mode (XSLT 3.0 only). Null means the unnamed mode is the default.
    private StructuredQName defaultMode = null;

    /**
     * Get the owning PreparedStylesheet object.
     * @return the owning PreparedStylesheet object. Exceptionally returns null during early construction.
     */

    public Executable getPreparedStylesheet() {
        return (principalStylesheetModule==null ? null : principalStylesheetModule.getExecutable());
    }

    public void setPrincipalStylesheetModule(PrincipalStylesheetModule module) {
        this.principalStylesheetModule = module;
        this.exec = module.getExecutable();
    }

    public PrincipalStylesheetModule getPrincipalStylesheetModule() {
        return principalStylesheetModule;
    }

    /**
     * Get the run-time Executable object
     */

    public Executable getExecutable() {
        return exec;
    }

    protected boolean mayContainParam(String attName) {
        return true;
    }

    /**
     * Get the RuleManager which handles template rules
     * @return the template rule manager
     */

    public RuleManager getRuleManager() {
        return exec.getRuleManager();
    }

    /**
     * Get the default mode (XSLT 3.0 feature)
     * @return the default mode name for this stylesheet module. A return value of null indicates either that
     * no default mode was specified, or that default-mode="#unnamed" was specified.
     */

    public StructuredQName getDefaultMode() {
        return defaultMode;
    }

    /**
     * Prepare the attributes on the stylesheet element
     */

    public void prepareAttributes() throws XPathException {
        checkAttribute("version", "s1");
        checkAttribute("id", "s");
        checkAttribute("extension-element-prefixes", "s");
        checkAttribute("exclude-result-prefixes", "s");
        checkAttribute("default-validation", "v");
        String inputTypeAnnotationsAtt = (String)checkAttribute("input-type-annotations", "w");
        checkForUnknownAttributes();

        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                             "Permitted values are (strip, preserve, unspecified)", "XTSE0020");
            }
        }

    }

    /**
     * Get the value of the input-type-annotations attribute, for this module alone.
     * The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     * @return the value if the input-type-annotations attribute in this stylesheet module
     */

    public int getInputTypeAnnotationsAttribute() throws XPathException {
        String inputTypeAnnotationsAtt = getAttributeValue("", "input-type-annotations");
        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
                return ANNOTATION_STRIP;
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
                return ANNOTATION_PRESERVE;
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
                return ANNOTATION_UNSPECIFIED;
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                             "Permitted values are (strip, preserve, unspecified)", "XTSE0020");
            }
        }
        return -1;
    }





    /**
     * Validate this element
     * @param decl
     */

    public void validate(Declaration decl) throws XPathException {
        if (validationError != null) {
            compileError(validationError);
        }
        if (getParent().getNodeKind() != Type.DOCUMENT) {
            compileError(getDisplayName() + " must be the outermost element", "XTSE0010");
        }

        for (NodeImpl child: allChildren()) {
            if (child.getNodeKind() == Type.TEXT ||
                    (child instanceof StyleElement && ((StyleElement)child).isDeclaration()) ||
                    child instanceof DataElement) {
                // all is well
            } else if (!NamespaceConstant.XSLT.equals(child.getURI()) && !"".equals(child.getURI())) {
                // elements in other namespaces are allowed and ignored
            } else if (child instanceof AbsentExtensionElement && ((StyleElement)child).forwardsCompatibleModeIsEnabled()) {
                // this is OK: an unknown XSLT element is allowed in forwards compatibility mode
            } else if (NamespaceConstant.XSLT.equals(child.getURI())) {
                ((StyleElement)child).compileError("Element " + child.getDisplayName() +
                        " must not appear directly within " + getDisplayName(), "XTSE0010");
            } else {
                ((StyleElement)child).compileError("Element " + child.getDisplayName() +
                        " must not appear directly within " + getDisplayName() +
                        " because it is not in a namespace", "XTSE0130");
            }
        }
    }







    /**
     * Process the attributes of every node in the stylesheet
     */

    public void processAllAttributes() throws XPathException {
        processDefaultCollationAttribute("");
        prepareAttributes();
        for (NodeImpl child: allChildren()) {
            if (child instanceof StyleElement) {
                try {
                    ((StyleElement) child).processAllAttributes();
                } catch (XPathException err) {
                    ((StyleElement) child).compileError(err);
                }
            }
        }
    }




    /**
     * Dummy compile() method to satisfy the interface
     */

    public Expression compile(Executable exec, Declaration decl) {
        return null;
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
