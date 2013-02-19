package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.ElementImpl;
import client.net.sf.saxon.ce.tree.linked.NodeFactory;
import client.net.sf.saxon.ce.value.DecimalValue;


/**
  * Class StyleNodeFactory. <br>
  * A Factory for nodes in the stylesheet tree. <br>
  * Currently only allows Element nodes to be user-constructed.
  * @author Michael H. Kay
  */

public class StyleNodeFactory implements NodeFactory {


    protected Configuration config;
    protected NamePool namePool;

    /**
     * Create the node factory for representing an XSLT stylesheet as a tree structure
     * @param config the Saxon configuration
     */

    public StyleNodeFactory(Configuration config) {
		this.config = config;
        namePool = config.getNamePool();
    }

    /**
    * Create an Element node. Note, if there is an error detected while constructing
    * the Element, we add the element anyway, and return success, but flag the element
    * with a validation error. This allows us to report more than
    * one error from a single compilation.
    * @param nameCode The element name
     * @param typeCode
     * @param attlist the attribute list
     */

    public ElementImpl makeElementNode(
            NodeInfo parent,
            int nameCode,
            int typeCode, AttributeCollection attlist,
            NamespaceBinding[] namespaces,
            int namespacesUsed,
            PipelineConfiguration pipe,
            String baseURI,
            int sequence)
    {
        boolean toplevel = (parent instanceof XSLStylesheet);

        if (parent instanceof DataElement) {
            DataElement d = new DataElement();
            d.setNamespaceDeclarations(namespaces, namespacesUsed);
            d.initialise(nameCode, attlist, parent, sequence);
            d.setLocation(baseURI);
            return d;
        }

    	int f = nameCode&0xfffff;

    	// Try first to make an XSLT element

    	StyleElement e = makeXSLElement(f);

		if (e != null) {  // recognized as an XSLT element

            e.setNamespaceDeclarations(namespaces, namespacesUsed);
            e.initialise(nameCode, attlist, parent, sequence);
            e.setLocation(baseURI);
            // We're not catching multiple errors in the following attributes, but catching each of the
            // exceptions helps to ensure we don't report spurious errors through not processing some
            // of the attributes when others are faulty.
            try {
	            e.processExtensionElementAttribute("");
            } catch (XPathException err) {
	            e.setValidationError(err, StyleElement.REPORT_ALWAYS);
	        }
            try {
	            e.processExcludedNamespaces("");
            } catch (XPathException err) {
	            e.setValidationError(err, StyleElement.REPORT_ALWAYS);
	        }
            try {
	            e.processVersionAttribute("");
            } catch (XPathException err) {
	            e.setValidationError(err, StyleElement.REPORT_ALWAYS);
	        }
	        e.processDefaultXPathNamespaceAttribute("");

            return e;

        }

        String uriCode = namePool.getURI(nameCode);

        if (parent instanceof XSLStylesheet && !uriCode.isEmpty() && !uriCode.equals(NamespaceConstant.XSLT)) {
            DataElement d = new DataElement();
            d.setNamespaceDeclarations(namespaces, namespacesUsed);
            d.initialise(nameCode, attlist, parent, sequence);
            d.setLocation(baseURI);
            return d;

        } else {   // not recognized as an XSLT element, not top-level

            String localname = namePool.getLocalName(nameCode);
            StyleElement temp = null;

            // Detect a misspelt XSLT declaration

            if (uriCode.equals(NamespaceConstant.XSLT) &&
                    (parent instanceof XSLStylesheet) &&
                    ((XSLStylesheet)parent).getEffectiveVersion().compareTo(DecimalValue.TWO) <= 0 ) {
                temp = new AbsentExtensionElement();
                temp.setValidationError(new XPathException("Unknown top-level XSLT declaration"),
                       StyleElement.REPORT_UNLESS_FORWARDS_COMPATIBLE );
            }

            StyleElement assumedElement = new LiteralResultElement();

            // We can't work out the final class of the node until we've examined its attributes
            // such as version and extension-element-prefixes; but we can have a good guess, and
            // change it later if need be.

            if (temp==null) {
                temp = new LiteralResultElement();
            }

            temp.setNamespaceDeclarations(namespaces, namespacesUsed);

            try {
                temp.initialise(nameCode, attlist, parent, sequence);
                temp.setLocation(baseURI);
                temp.processStandardAttributes(NamespaceConstant.XSLT);
            } catch (XPathException err) {
                temp.setValidationError(err, StyleElement.REPORT_ALWAYS);
            }

            // Now we work out what class of element we really wanted, and change it if necessary

            XPathException reason;
            StyleElement actualElement = null;

            if (uriCode.equals(NamespaceConstant.XSLT)) {
                reason = new XPathException("Unknown XSLT element: " + localname);
                reason.setErrorCode("XTSE0010");
                reason.setIsStaticError(true);
                actualElement = new AbsentExtensionElement();
                temp.setValidationError(reason, StyleElement.REPORT_UNLESS_FALLBACK_AVAILABLE);

            } else if (temp.isExtensionNamespace(uriCode) && !toplevel) {

                String uri = namePool.getURI(nameCode);
                if (NamespaceConstant.IXSL.equals(uri)) {
                    if (localname.equals("set-attribute")) {
                        actualElement = new IXSLSetAttribute();
                    } else if (localname.equals("remove-attribute")) {
                    	actualElement = new IXSLRemoveAttribute();
                    } else if (localname.equals("schedule-action")) {
                        actualElement = new IXSLScheduleAction();
                    } else if (localname.equals("set-property")) {
                    	actualElement = new IXSLSetProperty();
                    }
                }

                if (actualElement == null) {

                    // if we can't instantiate an extension element, we don't give up
                    // immediately, because there might be an xsl:fallback defined. We
                    // create a surrogate element called AbsentExtensionElement, and
                    // save the reason for failure just in case there is no xsl:fallback

                    actualElement = new AbsentExtensionElement();
                    XPathException se = new XPathException("Unknown extension instruction", temp);
                    se.setErrorCode("XTDE1450");
                    reason = se;
                    temp.setValidationError(reason, StyleElement.REPORT_IF_INSTANTIATED);
                }

            } else {
                actualElement = new LiteralResultElement();
            }

            StyleElement node;
            if (actualElement.getClass().equals(assumedElement.getClass())) {
                node = temp;    // the original element will do the job
            } else {
                node = actualElement;
                node.substituteFor(temp);   // replace temporary node with the new one
            }
            return node;
        }
    }

	/**
	 * Make an XSL element node
     * @param f the fingerprint of the node name
     * @return the constructed element node
	*/

	protected StyleElement makeXSLElement(int f) {
        switch (f) {
		case StandardNames.XSL_ANALYZE_STRING:
            return new XSLAnalyzeString();
		case StandardNames.XSL_APPLY_IMPORTS:
            return new XSLApplyImports();
		case StandardNames.XSL_APPLY_TEMPLATES:
            return new XSLApplyTemplates();
		case StandardNames.XSL_ATTRIBUTE:
            return new XSLAttribute();
		case StandardNames.XSL_ATTRIBUTE_SET:
            return new XSLAttributeSet();
		case StandardNames.XSL_CALL_TEMPLATE:
            return new XSLCallTemplate();
		case StandardNames.XSL_CHARACTER_MAP:
            return new XSLCharacterMap();
		case StandardNames.XSL_CHOOSE:
            return new XSLChoose();
		case StandardNames.XSL_COMMENT:
            return new XSLComment();
		case StandardNames.XSL_COPY:
            return new XSLCopy();
		case StandardNames.XSL_COPY_OF:
            return new XSLCopyOf();
		case StandardNames.XSL_DECIMAL_FORMAT:
            return new XSLDecimalFormat();
		case StandardNames.XSL_DOCUMENT:
            return new XSLDocument();
		case StandardNames.XSL_ELEMENT:
            return new XSLElement();
		case StandardNames.XSL_FALLBACK:
            return new XSLFallback();
		case StandardNames.XSL_FOR_EACH:
            return new XSLForEach();
		case StandardNames.XSL_FOR_EACH_GROUP:
            return new XSLForEachGroup();
		case StandardNames.XSL_FUNCTION:
            return new XSLFunction();
		case StandardNames.XSL_IF:
            return new XSLIf();
		case StandardNames.XSL_IMPORT:
            return new XSLImport();
		case StandardNames.XSL_IMPORT_SCHEMA:
            return new XSLImportSchema();
		case StandardNames.XSL_INCLUDE:
            return new XSLInclude();
		case StandardNames.XSL_KEY:
            return new XSLKey();
		case StandardNames.XSL_MATCHING_SUBSTRING:
            return new XSLMatchingSubstring();
		case StandardNames.XSL_MESSAGE:
            return new XSLMessage();
		case StandardNames.XSL_NEXT_MATCH:
            return new XSLNextMatch();
		case StandardNames.XSL_NON_MATCHING_SUBSTRING:
            return new XSLMatchingSubstring();	//sic
		case StandardNames.XSL_NUMBER:
            return new XSLNumber();
		case StandardNames.XSL_NAMESPACE:
            return new XSLNamespace();
		case StandardNames.XSL_NAMESPACE_ALIAS:
            return new XSLNamespaceAlias();
		case StandardNames.XSL_OTHERWISE:
            return new XSLOtherwise();
		case StandardNames.XSL_OUTPUT:
            return new XSLOutput();
		case StandardNames.XSL_OUTPUT_CHARACTER:
            return new XSLOutputCharacter();
		case StandardNames.XSL_PARAM:
            return new XSLParam();
		case StandardNames.XSL_PERFORM_SORT:
            return new XSLPerformSort();
		case StandardNames.XSL_PRESERVE_SPACE:
            return new XSLPreserveSpace();
		case StandardNames.XSL_PROCESSING_INSTRUCTION:
            return new XSLProcessingInstruction();
		case StandardNames.XSL_RESULT_DOCUMENT:
            return new XSLResultDocument();
		case StandardNames.XSL_SEQUENCE:
            return new XSLSequence();
		case StandardNames.XSL_SORT:
            return new XSLSort();
		case StandardNames.XSL_STRIP_SPACE:
            return new XSLPreserveSpace();
		case StandardNames.XSL_STYLESHEET:
            return new XSLStylesheet();
		case StandardNames.XSL_TEMPLATE:
            return new XSLTemplate();
		case StandardNames.XSL_TEXT:
            return new XSLText();
		case StandardNames.XSL_TRANSFORM:
            return new XSLStylesheet();
		case StandardNames.XSL_VALUE_OF:
            return new XSLValueOf();
		case StandardNames.XSL_VARIABLE:
            return new XSLVariable();
		case StandardNames.XSL_WITH_PARAM:
            return new XSLWithParam();
		case StandardNames.XSL_WHEN:
            return new XSLWhen();
        default:
            return null;
        }
	}

    /**
     * Method to support the element-available() function
     * @param uri the namespace URI
     * @param localName the local Name
     * @return true if an extension element of this name is recognized
    */

    public boolean isElementAvailable(String uri, String localName) {
    	int fingerprint = namePool.getFingerprint(uri, localName);
    	if (uri.equals(NamespaceConstant.XSLT)) {
    		if (fingerprint == -1) {
                return false; 	// all names are pre-registered
            }
    		StyleElement e = makeXSLElement(fingerprint);
    		if (e != null) {
                return e.isInstruction();
            }
    	}
        return false;
    }




}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
