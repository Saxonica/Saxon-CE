package client.net.sf.saxon.ce.style;
import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.ComputedAttribute;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.FixedAttribute;
import client.net.sf.saxon.ce.lib.StandardURIChecker;
import client.net.sf.saxon.ce.lib.Validation;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

/**
* xsl:attribute element in stylesheet. <br>
*/

public class XSLAttribute extends XSLLeafNodeConstructor {

    private Expression attributeName;
    private Expression separator;
    private Expression namespace = null;

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String nameAtt = null;
		String namespaceAtt = null;
        String selectAtt = null;
        String separatorAtt = null;
		String validationAtt = null;
		String typeAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals("name")) {
        		nameAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("namespace")) {
        		namespaceAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("select")) {
        		selectAtt = atts.getValue(a);
        	} else if (f.equals("separator")) {
        		separatorAtt = atts.getValue(a);
        	} else if (f.equals("validation")) {
        		validationAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("type")) {
        		typeAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
            return;
        }
        attributeName = makeAttributeValueTemplate(nameAtt);
        if (attributeName instanceof StringLiteral) {
            if (nameAtt.equals("xmlns")) {
                if (namespace==null) {
                    invalidAttributeName("Invalid attribute name: xmlns");
                }
            }
            if (nameAtt.startsWith("xmlns:")) {
                if (namespaceAtt == null) {
                    invalidAttributeName("Invalid attribute name: " + Err.wrap(nameAtt));
                } else {
                    // ignore the prefix "xmlns"
                    nameAtt = nameAtt.substring(6);
                    attributeName = new StringLiteral(nameAtt);
                }
            }
        }


        if (namespaceAtt!=null) {
            namespace = makeAttributeValueTemplate(namespaceAtt);
            if (namespace instanceof StringLiteral) {
                if (!StandardURIChecker.getInstance().isValidURI(((StringLiteral)namespace).getStringValue())) {
                    compileError("The value of the namespace attribute must be a valid URI", "XTDE0865");
                }
            }
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

        if (separatorAtt == null) {
            if (selectAtt == null) {
                separator = new StringLiteral(StringValue.EMPTY_STRING);
            } else {
                separator = new StringLiteral(StringValue.SINGLE_SPACE);
            }
        } else {
            separator = makeAttributeValueTemplate(separatorAtt);
        }

        if (validationAtt!=null && Validation.getCode(validationAtt) != Validation.STRIP) {
            compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
        }

        if (typeAtt!=null) {
            compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
        }

    }

    private void invalidAttributeName(String message) throws XPathException {
            compileError(message, "XTDE0850");
            // prevent a duplicate error message...
            attributeName = new StringLiteral("saxon-error-attribute");
//        }
    }

    public void validate(Declaration decl) throws XPathException {
        attributeName = typeCheck(attributeName);
        namespace = typeCheck(namespace);
        select = typeCheck(select);
        separator = typeCheck(separator);
        super.validate(decl);
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0840";
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        NamespaceResolver nsContext = null;

        // deal specially with the case where the attribute name is known statically

        if (attributeName instanceof StringLiteral) {
            String qName = Whitespace.trim(((StringLiteral)attributeName).getStringValue());
            String[] parts;
            try {
                parts = NameChecker.getQNameParts(qName);
            } catch (QNameException e) {
                // This can't happen, because of previous checks
                return null;
            }

            if (namespace==null) {
                String nsuri = "";
                if (!parts[0].equals("")) {
                    nsuri = getURIForPrefix(parts[0], false);
                    if (nsuri == null) {
                        undeclaredNamespaceError(parts[0], "XTSE0280");
                        return null;
                    }
                }
                StructuredQName nameCode = new StructuredQName(parts[0], nsuri, parts[1]);
                FixedAttribute inst = new FixedAttribute(nameCode);
                inst.setContainer(this);     // temporarily
                compileContent(exec, decl, inst, separator);
                if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                	inst.AddTraceProperty("name", attributeName);
                }
                return inst;
            } else if (namespace instanceof StringLiteral) {
                String nsuri = ((StringLiteral)namespace).getStringValue();
                if (nsuri.equals("")) {
                    parts[0] = "";
                } else if (parts[0].equals("")) {
                    // Need to choose an arbitrary prefix
                    // First see if the requested namespace is declared in the stylesheet
                    AxisIterator iter = iterateAxis(Axis.NAMESPACE);
                    while (true) {
                        NodeInfo ns = (NodeInfo)iter.next();
                        if (ns == null) {
                            break;
                        }
                        if (ns.getStringValue().equals(nsuri)) {
                            parts[0] = ns.getLocalPart();
                            break;
                        }
                    }

                    // Otherwise choose something arbitrary. This will get changed
                    // if it clashes with another attribute
                    if (parts[0].equals("")) {
                        parts[0] = "ns0";
                    }
                }
                StructuredQName nameCode = new StructuredQName(parts[0], nsuri, parts[1]);
                FixedAttribute inst = new FixedAttribute(nameCode);
                compileContent(exec, decl, inst, separator);
                if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                	inst.AddTraceProperty("name", attributeName);
                }
                return inst;
            }
        } else {
            // if the namespace URI must be deduced at run-time from the attribute name
            // prefix, we need to save the namespace context of the instruction

            if (namespace==null) {
                nsContext = this;
            }
        }

        ComputedAttribute inst = new ComputedAttribute( attributeName, namespace, nsContext);
        compileContent(exec, decl, inst, separator);
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	inst.AddTraceProperty("name", attributeName);
        }
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
