package client.net.sf.saxon.ce.style;

import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.lib.StandardURIChecker;
import client.net.sf.saxon.ce.lib.Validation;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.Whitespace;


/**
 * An xsl:element element in the stylesheet. <br>
 */

public class XSLElement extends StyleElement {

    private Expression elementName;
    private Expression namespace = null;
    private String use;
    private AttributeSet[] attributeSets = null;
    private boolean inheritNamespaces = true;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        AttributeCollection atts = getAttributeList();

        String nameAtt = null;
        String namespaceAtt = null;
        String validationAtt = null;
        String typeAtt = null;
        String inheritAtt = null;

        for (int a = 0; a < atts.getLength(); a++) {
            StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
            if (f.equals(StandardNames.NAME)) {
                nameAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.NAMESPACE)) {
                namespaceAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.VALIDATION)) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.TYPE)) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.INHERIT_NAMESPACES)) {
                inheritAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.USE_ATTRIBUTE_SETS)) {
                use = atts.getValue(a);
            } else {
                checkUnknownAttribute(qn);
            }
        }

        if (nameAtt == null) {
            reportAbsence("name");
        } else {
            elementName = makeAttributeValueTemplate(nameAtt);
            if (elementName instanceof StringLiteral) {
                if (!NameChecker.isQName(((StringLiteral)elementName).getStringValue())) {
                    compileError("Element name " +
                            Err.wrap(((StringLiteral)elementName).getStringValue(), Err.ELEMENT) +
                            " is not a valid QName", "XTDE0820");
                    // to prevent duplicate error messages:
                    elementName = new StringLiteral("saxon-error-element");
                }
            }
        }

        if (namespaceAtt != null) {
            namespace = makeAttributeValueTemplate(namespaceAtt);
            if (namespace instanceof StringLiteral) {
                if (!StandardURIChecker.getInstance().isValidURI(((StringLiteral)namespace).getStringValue())) {
                    compileError("The value of the namespace attribute must be a valid URI", "XTDE0835");
                }
            }
        }

        if (validationAtt != null && Validation.getCode(validationAtt) != Validation.STRIP) {
            compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
        }
        if (typeAtt!=null) {
            compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
        }

        if (inheritAtt != null) {
            if (inheritAtt.equals("yes")) {
                inheritNamespaces = true;
            } else if (inheritAtt.equals("no")) {
                inheritNamespaces = false;
            } else {
                compileError("The @inherit-namespaces attribute has permitted values (yes, no)", "XTSE0020");
            }
        }
    }

    public void validate(Declaration decl) throws XPathException {
        if (use != null) {
            // find any referenced attribute sets
            attributeSets = getAttributeSets(use, null);
        }
        elementName = typeCheck(elementName);
        namespace = typeCheck(namespace);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        NamespaceResolver nsContext = null;

        // deal specially with the case where the element name is known statically

        if (elementName instanceof StringLiteral) {
            CharSequence qName = ((StringLiteral)elementName).getStringValue();

            String[] parts;
            try {
                parts = NameChecker.getQNameParts(qName);
            } catch (QNameException e) {
                compileError("Invalid element name: " + qName, "XTDE0820");
                return null;
            }

            String nsuri = null;
            if (namespace instanceof StringLiteral) {
                nsuri = ((StringLiteral)namespace).getStringValue();
                if (nsuri.length() == 0) {
                    parts[0] = "";
                }
            } else if (namespace == null) {
                nsuri = getURIForPrefix(parts[0], true);
                if (nsuri == null) {
                    undeclaredNamespaceError(parts[0], "XTDE0830");
                }
            }
            if (nsuri != null) {
                // Local name and namespace are both known statically: generate a FixedElement instruction
                StructuredQName nameCode = new StructuredQName(parts[0], nsuri, parts[1]);
                FixedElement inst = new FixedElement(nameCode, null, inheritNamespaces);
                inst.setBaseURI(getBaseURI());
                if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                	inst.AddTraceProperty("name", elementName);
                }
                return compileContentExpression(exec, decl, inst);
            }
        } else {
            // if the namespace URI must be deduced at run-time from the element name
            // prefix, we need to save the namespace context of the instruction

            if (namespace == null) {
                nsContext = this;
            }
        }

        ComputedElement inst = new ComputedElement(elementName, namespace, nsContext, inheritNamespaces);
        return compileContentExpression(exec, decl, inst);
    }

    private Expression compileContentExpression(Executable exec, Declaration decl, ElementCreator inst) throws XPathException {
        Expression content = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));

        if (attributeSets != null) {
            UseAttributeSets use = new UseAttributeSets(attributeSets);
            if (content == null) {
                content = use;
            } else {
                content = Block.makeBlock(use, content);
                content.setSourceLocator(this);
            }
        }
        if (content == null) {
            content = new Literal(EmptySequence.getInstance());
        }
        inst.setContentExpression(content);
        return inst;
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
