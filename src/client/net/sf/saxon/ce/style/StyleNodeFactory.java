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
 *
 * @author Michael H. Kay
 */

public class StyleNodeFactory implements NodeFactory {


    protected Configuration config;
    protected NamePool namePool;

    /**
     * Create the node factory for representing an XSLT stylesheet as a tree structure
     *
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
     *
     * @param nameCode The element name
     * @param attlist  the attribute list
     */

    public ElementImpl makeElementNode(
            NodeInfo parent,
            StructuredQName nameCode,
            AttributeCollection attlist,
            NamespaceBinding[] namespaces,
            int namespacesUsed,
            PipelineConfiguration pipe,
            String baseURI,
            int sequence) {
        boolean toplevel = (parent instanceof XSLStylesheet);

        if (parent instanceof DataElement) {
            DataElement d = new DataElement();
            d.setNamespaceDeclarations(namespaces, namespacesUsed);
            d.initialise(nameCode, attlist, parent, sequence);
            d.setLocation(baseURI);
            return d;
        }


        // Try first to make an XSLT element

        StyleElement e = null;
        if (nameCode.getNamespaceURI().equals(NamespaceConstant.XSLT)) {
            e = makeXSLElement(nameCode.getLocalName());
        }

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

        String uriCode = nameCode.getNamespaceURI();

        if (parent instanceof XSLStylesheet && !uriCode.isEmpty() && !uriCode.equals(NamespaceConstant.XSLT)) {
            DataElement d = new DataElement();
            d.setNamespaceDeclarations(namespaces, namespacesUsed);
            d.initialise(nameCode, attlist, parent, sequence);
            d.setLocation(baseURI);
            return d;

        } else {   // not recognized as an XSLT element, not top-level

            String localname = nameCode.getLocalName();
            StyleElement temp = null;

            // Detect a misspelt XSLT declaration

            if (uriCode.equals(NamespaceConstant.XSLT) &&
                    (parent instanceof XSLStylesheet) &&
                    ((XSLStylesheet) parent).getEffectiveVersion().compareTo(DecimalValue.TWO) <= 0) {
                temp = new AbsentExtensionElement();
                temp.setValidationError(new XPathException("Unknown top-level XSLT declaration"),
                        StyleElement.REPORT_UNLESS_FORWARDS_COMPATIBLE);
            }

            StyleElement assumedElement = new LiteralResultElement();

            // We can't work out the final class of the node until we've examined its attributes
            // such as version and extension-element-prefixes; but we can have a good guess, and
            // change it later if need be.

            if (temp == null) {
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

                String uri = nameCode.getNamespaceURI();
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
     *
     * @param localName the node name
     * @return the constructed element node
     */

    protected StyleElement makeXSLElement(String localName) {
        switch (localName.charAt(0)) {
            case 'a':
                if (localName.equals("analyze-string")) {
                    return new XSLAnalyzeString();
                } else if (localName.equals("apply-imports")) {
                    return new XSLApplyImports();
                } else if (localName.equals("apply-templates")) {
                    return new XSLApplyTemplates();
                } else if (localName.equals("attribute")) {
                    return new XSLAttribute();
                } else if (localName.equals("attribute-set")) {
                    return new XSLAttributeSet();
                }
            case 'c':
                if (localName.equals("call-template")) {
                    return new XSLCallTemplate();
                } else if (localName.equals("character-map")) {
                    return new XSLCharacterMap();
                } else if (localName.equals("choose")) {
                    return new XSLChoose();
                } else if (localName.equals("comment")) {
                    return new XSLComment();
                } else if (localName.equals("copy")) {
                    return new XSLCopy();
                } else if (localName.equals("copy-of")) {
                    return new XSLCopyOf();
                }
            case 'd':
                if (localName.equals("decimal-format")) {
                    return new XSLDecimalFormat();
                } else if (localName.equals("document")) {
                    return new XSLDocument();
                }
            case 'e':
                if (localName.equals("element")) {
                    return new XSLElement();
                }
            case 'f':
                if (localName.equals("fallback")) {
                    return new XSLFallback();
                } else if (localName.equals("for-each")) {
                    return new XSLForEach();
                } else if (localName.equals("for-each-group")) {
                    return new XSLForEachGroup();
                } else if (localName.equals("function")) {
                    return new XSLFunction();
                }
            case 'i':
                if (localName.equals("if")) {
                    return new XSLIf();
                } else if (localName.equals("import")) {
                    return new XSLImport();
                } else if (localName.equals("import-schema")) {
                    return new XSLImportSchema();
                } else if (localName.equals("include")) {
                    return new XSLInclude();
                }
            case 'k':
                if (localName.equals("key")) {
                    return new XSLKey();
                }
            case 'm':
                if (localName.equals("matching-substring")) {
                    return new XSLMatchingSubstring();
                } else if (localName.equals("message")) {
                    return new XSLMessage();
                }
            case 'n':
                if (localName.equals("next-match")) {
                    return new XSLNextMatch();
                } else if (localName.equals("non-matching-substring")) {
                    return new XSLMatchingSubstring();    //sic
                } else if (localName.equals("number")) {
                    return new XSLNumber();
                } else if (localName.equals("namespace")) {
                    return new XSLNamespace();
                } else if (localName.equals("namespace-alias")) {
                    return new XSLNamespaceAlias();
                }
            case 'o':
                if (localName.equals("otherwise")) {
                    return new XSLOtherwise();
                } else if (localName.equals("output")) {
                    return new XSLOutput();
                } else if (localName.equals("output-character")) {
                    return new XSLOutputCharacter();
                }
            case 'p':
                if (localName.equals("param")) {
                    return new XSLParam();
                } else if (localName.equals("perform-sort")) {
                    return new XSLPerformSort();
                } else if (localName.equals("preserve-space")) {
                    return new XSLPreserveSpace();
                } else if (localName.equals("processing-instruction")) {
                    return new XSLProcessingInstruction();
                }
            case 'r':
                if (localName.equals("result-document")) {
                    return new XSLResultDocument();
                }
            case 's':
                if (localName.equals("sequence")) {
                    return new XSLSequence();
                } else if (localName.equals("sort")) {
                    return new XSLSort();
                } else if (localName.equals("strip-space")) {
                    return new XSLPreserveSpace();
                } else if (localName.equals("stylesheet")) {
                    return new XSLStylesheet();
                }
            case 't':
                if (localName.equals("template")) {
                    return new XSLTemplate();
                } else if (localName.equals("text")) {
                    return new XSLText();
                } else if (localName.equals("transform")) {
                    return new XSLStylesheet();
                }
            case 'v':
                if (localName.equals("value-of")) {
                    return new XSLValueOf();
                } else if (localName.equals("variable")) {
                    return new XSLVariable();
                }
            case 'w':
                if (localName.equals("with-param")) {
                    return new XSLWithParam();
                } else if (localName.equals("when")) {
                    return new XSLWhen();
                }
            default:
                return null;
        }
    }

    /**
     * Method to support the element-available() function
     *
     * @param uri       the namespace URI
     * @param localName the local Name
     * @return true if an extension element of this name is recognized
     */

    public boolean isElementAvailable(String uri, String localName) {
        if (uri.equals(NamespaceConstant.XSLT)) {
            StyleElement e = makeXSLElement(localName);
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
