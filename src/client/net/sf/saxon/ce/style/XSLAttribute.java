package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.ComputedAttribute;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.FixedAttribute;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.logging.client.LogConfiguration;

/**
* xsl:attribute element in stylesheet. <br>
*/

public class XSLAttribute extends XSLLeafNodeConstructor {

    private Expression attributeName;
    private Expression separator;
    private Expression namespace;

    public void prepareAttributes() throws XPathException {

//		AttributeCollection atts = getAttributeList();
//
//		String nameAtt = null;
//		String namespaceAtt = null;
//        String selectAtt = null;
//        String separatorAtt = null;
//		String validationAtt = null;
//		String typeAtt = null;
//
//		for (int a=0; a<atts.getLength(); a++) {
//			StructuredQName qn = atts.getStructuredQName(a);
//            String f = qn.getClarkName();
//			if (f.equals("name")) {
//        		nameAtt = Whitespace.trim(atts.getValue(a));
//        	} else if (f.equals("namespace")) {
//        		namespaceAtt = Whitespace.trim(atts.getValue(a));
//        	} else if (f.equals("select")) {
//        		selectAtt = atts.getValue(a);
//        	} else if (f.equals("separator")) {
//        		separatorAtt = atts.getValue(a);
//        	} else if (f.equals("validation")) {
//        		validationAtt = Whitespace.trim(atts.getValue(a));
//        	} else if (f.equals("type")) {
//        		typeAtt = Whitespace.trim(atts.getValue(a));
//        	} else {
//        		checkUnknownAttribute(qn);
//        	}
//        }

        attributeName = (Expression)checkAttribute("name", "a1");
        namespace = (Expression)checkAttribute("namespace", "a");
        select = (Expression)checkAttribute("select", "e");
        separator = (Expression)checkAttribute("separator", "a");
        checkAttribute("validation", "v");
        checkAttribute("type", "t");
        checkForUnknownAttributes();


        if (separator == null) {
            if (select == null) {
                separator = new StringLiteral(StringValue.EMPTY_STRING);
            } else {
                separator = new StringLiteral(StringValue.SINGLE_SPACE);
            }
        }

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

        NamespaceResolver resolver = new InscopeNamespaceResolver(this);
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
                    nsuri = resolver.getURIForPrefix(parts[0], false);
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
                    UnfailingIterator iter = iterateAxis(Axis.NAMESPACE);
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
        }

        ComputedAttribute inst = new ComputedAttribute( attributeName, namespace, resolver);
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
