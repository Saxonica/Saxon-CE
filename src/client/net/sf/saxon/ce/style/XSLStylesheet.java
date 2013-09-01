package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.PreparedStylesheet;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.Validation;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.RuleManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.Whitespace;

/**
 * An xsl:stylesheet or xsl:transform element in the stylesheet. <br>
 * Note this element represents a stylesheet module, not necessarily
 * the whole stylesheet. However, much of the functionality (and the fields)
 * are relevant only to the top-level module.
 */

public class XSLStylesheet extends StyleElement {

    PreparedStylesheet exec;


    // the PrincipalStylesheetModule object
    private PrincipalStylesheetModule principalStylesheetModule;

    public static final int ANNOTATION_UNSPECIFIED = 0;
    public static final int ANNOTATION_STRIP = 1;
    public static final int ANNOTATION_PRESERVE = 2;



    // default validation
    private int defaultValidation = Validation.STRIP;

    // default mode (XSLT 3.0 only). Null means the unnamed mode is the default.
    private StructuredQName defaultMode = null;

    /**
     * Get the owning PreparedStylesheet object.
     * @return the owning PreparedStylesheet object. Exceptionally returns null during early construction.
     */

    public PreparedStylesheet getPreparedStylesheet() {
        return (principalStylesheetModule==null ? null : principalStylesheetModule.getPreparedStylesheet());
    }

    public void setPrincipalStylesheetModule(PrincipalStylesheetModule module) {
        this.principalStylesheetModule = module;
        this.exec = module.getPreparedStylesheet();
    }

    public PrincipalStylesheetModule getPrincipalStylesheetModule() {
        return principalStylesheetModule;
    }

    /**
     * Get the run-time Executable object
     */

    public PreparedStylesheet getExecutable() {
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

        String inputTypeAnnotationsAtt = null;
        AttributeCollection atts = getAttributeList();
        for (int a = 0; a < atts.getLength(); a++) {

            StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
            if (f.equals(StandardNames.VERSION)) {
                // already processed
            } else if (f.equals(StandardNames.ID)) {
                //
            } else if (f.equals(StandardNames.EXTENSION_ELEMENT_PREFIXES)) {
                //
            } else if (f.equals(StandardNames.EXCLUDE_RESULT_PREFIXES)) {
                //
            } else if (f.equals(StandardNames.DEFAULT_VALIDATION)) {
                String val = Whitespace.trim(atts.getValue(a));
                defaultValidation = Validation.getCode(val);
                if (defaultValidation == Validation.INVALID) {
                    compileError("Invalid value for default-validation attribute. " +
                            "Permitted values are (strict, lax, preserve, strip)", "XTSE0020");
                } else if (defaultValidation != Validation.STRIP) {
                    defaultValidation = Validation.STRIP;
                    compileError("default-validation='" + val + "' requires a schema-aware processor",
                            "XTSE1660");
                }
            } else if (f.equals(StandardNames.INPUT_TYPE_ANNOTATIONS)) {
                inputTypeAnnotationsAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.DEFAULT_MODE)) {
                String val = Whitespace.trim(atts.getValue(a));
                if (!val.equals("#unnamed")) {
                    try {
                        defaultMode = makeQName(atts.getValue(a));
                    } catch (NamespaceException err) {
                        throw new XPathException(err.getMessage(), "XTST0030");
                    }
                }
            } else {
                checkUnknownAttribute(qn);
            }
        }
        if (version == null) {
            reportAbsence("version");
        }

        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
                //setInputTypeAnnotations(ANNOTATION_STRIP);
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
                //setInputTypeAnnotations(ANNOTATION_PRESERVE);
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
                //
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                             "Permitted values are (strip, preserve, unspecified)", "XTSE0020");
            }
        }

    }


    /**
     * Get the value of the default validation attribute
     * @return the value of the default-validation attribute, as a constant such
     * as {@link Validation#STRIP}
     */

    public int getDefaultValidation() {
        return defaultValidation;
    }


    /**
     * Get the value of the input-type-annotations attribute, for this module alone.
     * The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     * @return the value if the input-type-annotations attribute in this stylesheet module
     */

    public int getInputTypeAnnotationsAttribute() throws XPathException {
        String inputTypeAnnotationsAtt = getAttributeValue("", StandardNames.INPUT_TYPE_ANNOTATIONS);
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

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) break;
            if (curr.getNodeKind() == Type.TEXT ||
                    (curr instanceof StyleElement && ((StyleElement)curr).isDeclaration()) ||
                    curr instanceof DataElement) {
                // all is well
            } else if (!NamespaceConstant.XSLT.equals(curr.getURI()) && !"".equals(curr.getURI())) {
                // elements in other namespaces are allowed and ignored
            } else if (curr instanceof AbsentExtensionElement && ((StyleElement)curr).forwardsCompatibleModeIsEnabled()) {
                // this is OK: an unknown XSLT element is allowed in forwards compatibility mode
            } else if (NamespaceConstant.XSLT.equals(curr.getURI())) {
                ((StyleElement)curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName(), "XTSE0010");
            } else {
                ((StyleElement)curr).compileError("Element " + curr.getDisplayName() +
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
        AxisIterator iter = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo node = (NodeInfo)iter.next();
            if (node == null) {
                break;
            }
            if (node instanceof StyleElement) {
                try {
                    ((StyleElement) node).processAllAttributes();
                } catch (XPathException err) {
                    ((StyleElement) node).compileError(err);
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
