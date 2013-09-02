package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.PreparedStylesheet;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.TraceExpression;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.Validation;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trace.Location;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.DocumentImpl;
import client.net.sf.saxon.ce.tree.linked.LinkedTreeBuilder;
import client.net.sf.saxon.ce.tree.util.NamespaceIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
* This class represents a literal result element in the style sheet
* (typically an HTML element to be output). <br>
* It is also used to represent unknown top-level elements, which are ignored.
*/

public class LiteralResultElement extends StyleElement {

    private StructuredQName resultNameCode;
    private StructuredQName[] attributeNames;
    private Expression[] attributeValues;
    private int numberOfAttributes;
    private boolean toplevel;
    private List<NamespaceBinding> namespaceCodes = new ArrayList<NamespaceBinding>();
    private AttributeSet[] attributeSets;
    private boolean inheritNamespaces = true;

    /**
    * Determine whether this type of element is allowed to contain a sequence constructor
    * @return true: yes, it may contain a sequence constructor
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Specify that this is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Process the attribute list
    */

    public void prepareAttributes() throws XPathException {

        // Process the values of all attributes. At this stage we deal with attribute
        // values (especially AVTs), but we do not apply namespace aliasing to the
        // attribute names.

        AttributeCollection atts = getAttributeList();
        int num = atts.getLength();

        if (num == 0) {
            numberOfAttributes = 0;
        } else {
            attributeNames = new StructuredQName[num];
            attributeValues = new Expression[num];
            numberOfAttributes = 0;

            for (int i=0; i<num; i++) {

                StructuredQName qn = atts.getStructuredQName(i);
                String uri = qn.getNamespaceURI();
                String local = qn.getLocalName();

                if (uri.equals(NamespaceConstant.XSLT)) {

                    if (local.equals("use-attribute-sets")) {
                        // deal with this later
                    } else if (local.equals("default-collation")) {
                    	// already dealt with
                    } else if (local.equals("extension-element-prefixes")) {
                    	// already dealt with
                    } else if (local.equals("exclude-result-prefixes")) {
                    	// already dealt with
                    } else if (local.equals("version")) {
                        // already dealt with
                    } else if (local.equals("xpath-default-namespace")) {
                        // already dealt with
                    } else if (local.equals("type")) {
                        // deal with this later
                    } else if (local.equals("use-when")) {
                        // already dealt with
                    } else if (local.equals("validation")) {
                        // deal with this later
                    } else if (local.equals("inherit-namespaces")) {
                        String inheritAtt = atts.getValue(i);
                        if (inheritAtt.equals("yes")) {
                            inheritNamespaces = true;
                        } else if (inheritAtt.equals("no")) {
                            inheritNamespaces = false;
                        } else {
                            compileError("The xsl:inherit-namespaces attribute has permitted values (yes, no)", "XTSE0020");
                        }
                    } else {
                        compileError("Unknown XSL attribute " + qn.getDisplayName(), "XTSE0805");
                    }
                } else {
                    attributeNames[numberOfAttributes] = qn;
                    Expression exp = makeAttributeValueTemplate(atts.getValue(i));
                    attributeValues[numberOfAttributes] = exp;
                    numberOfAttributes++;
                }
            }

            // now shorten the arrays if necessary. This is necessary if there are [xsl:]-prefixed
            // attributes that weren't copied into the arrays.

            if (numberOfAttributes < attributeNames.length) {

                StructuredQName[] attributeNames2 = new StructuredQName[numberOfAttributes];
                System.arraycopy(attributeNames, 0, attributeNames2, 0, numberOfAttributes);
                attributeNames = attributeNames2;

                Expression[] attributeValues2 = new Expression[numberOfAttributes];
                System.arraycopy(attributeValues, 0, attributeValues2, 0, numberOfAttributes);
                attributeValues = attributeValues2;
            }
        }
    }

    /**
    * Validate that this node is OK
     * @param decl
     */

    public void validate(Declaration decl) throws XPathException {

        toplevel = (getParent() instanceof XSLStylesheet);

        resultNameCode = getNodeName();

        String elementURI = getURI();

        if (toplevel) {
            // A top-level element can never be a "real" literal result element,
            // but this class gets used for unknown elements found at the top level

            if (elementURI.isEmpty()) {
                compileError("Top level elements must have a non-null namespace URI", "XTSE0130");
            }
        } else {

            // Build the list of output namespace nodes. Note we no longer optimize this list.
            // See comments in the 9.1 source code for some history of this decision.
            Iterator<NamespaceBinding> inscope = NamespaceIterator.iterateNamespaces(this);
            while (inscope.hasNext()) {
                namespaceCodes.add(inscope.next());
            }

            // Spec bug 5857: if there is no other binding for the default namespace, add an undeclaration
            String defaultNamespace = getURIForPrefix("", true);
            if (defaultNamespace.isEmpty()) {
                namespaceCodes.add(NamespaceBinding.DEFAULT_UNDECLARATION);
            }

            // apply any aliases required to create the list of output namespaces

            PrincipalStylesheetModule sheet = getPrincipalStylesheetModule();

            if (sheet.hasNamespaceAliases()) {
                for (int i=0; i<namespaceCodes.size(); i++) {
                	// System.err.println("Examining namespace " + namespaceCodes[i]);
                    String suri = namespaceCodes.get(i).getURI();
                    NamespaceBinding ncode = sheet.getNamespaceAlias(suri);
                    if (ncode != null && !ncode.getURI().equals(suri)) {
                        // apply the namespace alias. Change in 7.3: use the prefix associated
                        // with the new namespace, not the old prefix.
                        namespaceCodes.set(i, ncode);
                    }
                }

                // determine if there is an alias for the namespace of the element name

                NamespaceBinding elementAlias = sheet.getNamespaceAlias(elementURI);
                if (elementAlias != null && !elementAlias.getURI().equals(elementURI)) {
                    resultNameCode = new StructuredQName(elementAlias.getPrefix(),
                                                       elementAlias.getURI(),
                                                       getLocalPart());
                }
            }
            // deal with special attributes

            String useAttSets = Navigator.getAttributeValue(this, NamespaceConstant.XSLT, "use-attribute-sets");
            if (useAttSets != null) {
                attributeSets = getAttributeSets(useAttSets, null);
            }

            String type = Navigator.getAttributeValue(this, NamespaceConstant.XSLT, "type");
            if (type != null) {
                compileError("The xsl:type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            }

            String validate = Navigator.getAttributeValue(this, NamespaceConstant.XSLT, "validation");
            if (validate != null && Validation.getCode(validate)!= Validation.STRIP) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
            }

            // establish the names to be used for all the output attributes;
            // also type-check the AVT expressions

            if (numberOfAttributes > 0) {

                for (int i=0; i<numberOfAttributes; i++) {

                    StructuredQName anameCode = attributeNames[i];
                    StructuredQName alias = anameCode;
                    String attURI = anameCode.getNamespaceURI();

                    if (!attURI.isEmpty()) {	// attribute has a namespace prefix
                        NamespaceBinding newNSCode = sheet.getNamespaceAlias(attURI);
                        if ((newNSCode != null && !newNSCode.getURI().equals(attURI))) {
                            alias = new StructuredQName( newNSCode.getPrefix(),
                                                       newNSCode.getURI(),
                                                       getAttributeList().getLocalName(i));
                        }
                    }

                    attributeNames[i] = alias;
  	                attributeValues[i] = typeCheck(attributeValues[i]);
                }
            }

            // remove any namespaces that are on the exclude-result-prefixes list.
            // The namespace is excluded even if it is the namespace of the element or an attribute,
            // though in that case namespace fixup will reinstate it.

            for (int n=namespaceCodes.size()-1; n>=0; n--) {
                String uri = namespaceCodes.get(n).getURI();
                if (isExcludedNamespace(uri) && !sheet.isAliasResultNamespace(uri)) {
                    namespaceCodes.remove(n);
                }
            }
        }
    }

    /**
    * Validate the children of this node, recursively. Overridden for top-level
    * data elements.
     * @param decl
     */

    protected void validateChildren(Declaration decl) throws XPathException {
        if (!toplevel) {
            super.validateChildren(decl);
        }
    }

	/**
	* Compile code to process the literal result element at runtime
	*/

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        // top level elements in the stylesheet are ignored
        if (toplevel) return null;

        NamespaceBinding[] bindings = namespaceCodes.toArray(new NamespaceBinding[namespaceCodes.size()]);
        FixedElement inst = new FixedElement(resultNameCode, bindings, inheritNamespaces);

        inst.setBaseURI(getBaseURI());
        Expression content = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));

        if (numberOfAttributes > 0) {
            for (int i=attributeNames.length - 1; i>=0; i--) {
                FixedAttribute att = new FixedAttribute(attributeNames[i]);
                try {
                    att.setSelect(attributeValues[i], exec.getConfiguration());
                } catch (XPathException err) {
                    compileError(err);
                }
                att.setSourceLocator(this);
                Expression exp = att;
                if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                    TraceExpression trace = new TraceExpression(exp);
                    trace.setNamespaceResolver(this);
                    trace.setConstructType(Location.LITERAL_RESULT_ATTRIBUTE);
                    trace.setSourceLocator(this);
                    trace.setObjectName(attributeNames[i]);
                    exp = trace;
                }

                if (content == null) {
                    content = exp;
                } else {
                    content = Block.makeBlock(exp, content);
                    content.setSourceLocator(this);
                }
            }
        }

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
            content = Literal.makeEmptySequence();
        }
        inst.setContentExpression(content);
        return inst;
    }

    /**
     * Make a top-level literal result element into a stylesheet. This implements
     * the "Simplified Stylesheet" facility.
     * @param pss the PreparedStylesheet (the compiled stylesheet as provided)
     * @return the reconstructed stylesheet with an xsl:stylesheet and xsl:template element added
    */

    public DocumentImpl makeStylesheet(PreparedStylesheet pss)
            throws XPathException {

        // the implementation grafts the LRE node onto a containing xsl:template and
        // xsl:stylesheet

		StyleNodeFactory nodeFactory = pss.getStyleNodeFactory();
        String xslPrefix = getPrefixForURI(NamespaceConstant.XSLT);
        if (xslPrefix==null) {
            String message;
            if (getLocalPart().equals("stylesheet") || getLocalPart().equals("transform")) {
                if (getPrefixForURI(NamespaceConstant.MICROSOFT_XSL) != null) {
                    message = "Saxon is not able to process Microsoft's WD-xsl dialect";
                } else {
                    message = "Namespace for stylesheet element should be " + NamespaceConstant.XSLT;
                }
            } else {
                message = "The supplied file does not appear to be a stylesheet";
            }
            XPathException err = new XPathException(message);
            err.setLocator(this);
            err.setErrorCode("XTSE0150");
            err.setIsStaticError(true);
            pss.reportError(err);
            throw err;

        }

        // check there is an xsl:version attribute (it's mandatory), and copy
        // it to the new xsl:stylesheet element

        String version = Navigator.getAttributeValue(this, NamespaceConstant.XSLT, "version");
        if (version==null) {
            XPathException err = new XPathException("Simplified stylesheet: xsl:version attribute is missing");
            err.setErrorCode("XTSE0150");
            err.setIsStaticError(true);
            err.setLocator(this);
            pss.reportError(err);
            throw err;
        }

        try {
            DocumentImpl oldRoot = (DocumentImpl)getDocumentRoot();
            LinkedTreeBuilder builder = new LinkedTreeBuilder();
            builder.setPipelineConfiguration(pss.getConfiguration().makePipelineConfiguration());
            builder.setNodeFactory(nodeFactory);
            builder.setSystemId(this.getSystemId());

            builder.open();
            builder.startDocument();

            StructuredQName st = new StructuredQName("xsl", NamespaceConstant.XSLT, "stylesheet");
            builder.startElement(st, 0);
            builder.namespace(new NamespaceBinding("xsl", NamespaceConstant.XSLT), 0);
            builder.attribute(new StructuredQName("", "", "version"), version);
            builder.startContent();

            StructuredQName te = new StructuredQName("xsl", NamespaceConstant.XSLT, "template");
            builder.startElement(te, 0);
            builder.attribute(new StructuredQName("", "", "match"), "/");
            builder.startContent();

            builder.graftElement(this);

            builder.endElement();
            builder.endElement();
            builder.endDocument();
            builder.close();

            return (DocumentImpl)builder.getCurrentRoot();
        } catch (XPathException err) {
            //TransformerConfigurationException e = new TransformerConfigurationException(err);
            err.setLocator(this);
            throw err;
        }

    }


}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
