package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper;
import client.net.sf.saxon.ce.dom.Sanitizer;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.sort.DocumentOrderIterator;
import client.net.sf.saxon.ce.expr.sort.GlobalOrderComparer;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.StripSpaceRules;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.Cardinality;


/**
 * Implements the XSLT document() function
 */

public class DocumentFn extends SystemFunction {

    public DocumentFn newInstance() {
        return new DocumentFn();
    }


    private String expressionBaseURI = null;

    /**
    * Method called during static type checking
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            // only do this once. The second call supplies an env pointing to the containing
            // xsl:template, which has a different base URI (and in a simplified stylesheet, has no base URI)
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
            argument[0] = ExpressionTool.unsorted(visitor.getConfiguration(), argument[0], false);
        }
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        Expression expression = argument[0];
        if (Cardinality.allowsMany(expression.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        // may have to revise this if the argument can be a list-valued element or attribute
    }

    /**
     * Get the base URI from the static context
     * @return the base URI
     */

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.PEER_NODESET |
                StaticProperty.NON_CREATIVE;
        // Declaring it as a peer node-set expression avoids sorting of expressions such as
        // document(XXX)/a/b/c
        // The document() function might appear to be creative: but it isn't, because multiple calls
        // with the same arguments will produce identical results.
    }

    /**
    * preEvaluate:
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }


    /**
    * iterate() handles evaluation of the function:
    * it returns a sequence of Document nodes
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        int numArgs = argument.length;

        SequenceIterator hrefSequence = argument[0].iterate(context);
        String baseURI = null;
        if (numArgs==2) {
            // we can trust the type checking: it must be a node
            NodeInfo base = (NodeInfo)argument[1].evaluateItem(context);
            baseURI = base.getBaseURI();
        }

        DocumentMappingFunction map = new DocumentMappingFunction(context);
        map.baseURI = baseURI;
        map.stylesheetURI = expressionBaseURI;
        map.locator = getSourceLocator();

        ItemMappingIterator iter = new ItemMappingIterator(hrefSequence, map);

        Expression expression = argument[0];
        if (Cardinality.allowsMany(expression.getCardinality())) {
            return new DocumentOrderIterator(iter, GlobalOrderComparer.getInstance());
            // this is to make sure we eliminate duplicates: two href's might be the same
        } else {
            return iter;
        }
    }

    private static class DocumentMappingFunction implements ItemMappingFunction {

        public String baseURI;
        public String stylesheetURI;
        public SourceLocator locator;
        public XPathContext context;

        public DocumentMappingFunction(XPathContext context) {
            this.context = context;
        }

        public Item mapItem(Item item) throws XPathException {
            String b = baseURI;
            if (b==null) {
                if (item instanceof NodeInfo) {
                    b = ((NodeInfo)item).getBaseURI();
                } else {
                    b = stylesheetURI;
                }
            }
            return makeDoc(item.getStringValue(), b, context, locator);
        }
    }

    /**
     * Supporting routine to load one external document given a URI (href) and a baseURI. This is used
     * in the normal case when a document is loaded at run-time (that is, when a Controller is available)
     * @param href the relative URI
     * @param baseURI the base URI
     * @param c the dynamic XPath context
     * @param locator used to identify the location of the instruction in event of error
     * @return the root of the constructed document, or the selected element within the document
     * if a fragment identifier was supplied
    */

    public static NodeInfo makeDoc(String href, String baseURI, XPathContext c, SourceLocator locator)
            throws XPathException {

        Configuration config = c.getConfiguration();

        // If the href contains a fragment identifier, strip it out now
        //System.err.println("Entering makeDoc " + href);
        int hash = href.indexOf('#');

        String fragmentId = null;
        if (hash>=0) {
            if (hash==href.length()-1) {
                // # sign at end - just ignore it
                href = href.substring(0, hash);
            } else {
                fragmentId = href.substring(hash+1);
                href = href.substring(0, hash);
                if (!NameChecker.isValidNCName(fragmentId)) {
                    // Don't report recoverable error XTRE1160
                    fragmentId = null;
                }
            }
        }

        Controller controller = c.getController();

        // Resolve relative URI
        DocumentURI documentKey = computeDocumentKey(href, baseURI);

        // see if the document is already loaded

        DocumentInfo doc = config.getGlobalDocumentPool().find(documentKey);
        if (doc != null) {
            return doc;
        }

        DocumentPool pool = controller.getDocumentPool();
        doc = pool.find(documentKey);
        if (doc != null) {
            return getFragment(doc, fragmentId, c);
        }

        // check that the document was not written by this transformation

        if (!controller.checkUniqueOutputDestination(documentKey)) {
            pool.markUnavailable(documentKey);
            throw new XPathException(
                    "Cannot read a document that was written during the same transformation: " + documentKey, "XTRE1500");
        }

        try {

            if (pool.isMarkedUnavailable(documentKey)) {
                throw new XPathException(
                        "Document has been marked not available: " + documentKey, "FODC0002");
            }

            DocumentInfo newdoc = config.buildDocument(documentKey.toString());
            if (newdoc instanceof HTMLDocumentWrapper) {
                StripSpaceRules rules = c.getController().getExecutable().getStripperRules();
                if (rules.isStripping()) {
                    new Sanitizer(rules).sanitize((HTMLDocumentWrapper)newdoc);
                }
            }
            controller.registerDocument(newdoc, documentKey);
            controller.addUnavailableOutputDestination(documentKey);
            return getFragment(newdoc, fragmentId, c);

        } catch (XPathException err) {
            pool.markUnavailable(documentKey);
//            err.maybeSetLocation(locator);
//            String code = (err.getCause() instanceof URI.URISyntaxException) ? "FODC0005" : "FODC0002";
//            err.maybeSetErrorCode(code);
//            controller.recoverableError(err);
            return null;
        }
    }

    /**
     * Compute a document key (an absolute URI that can be used to see if a document is already loaded)
     * @param href the relative URI
     * @param baseURI the base URI
     * @return a unique key for the document
     */

    public static DocumentURI computeDocumentKey(String href, String baseURI) {
        String documentKey;
        try {
            documentKey = ResolveURI.makeAbsolute(href, baseURI).toString();
        } catch (URI.URISyntaxException e) {
            documentKey = baseURI + "/../" + href;
        }
        return new DocumentURI(documentKey);
    }


    /**
     * Resolve the fragment identifier within a URI Reference.
     * Only "bare names" XPointers are recognized, that is, a fragment identifier
     * that matches an ID attribute value within the target document.
     * @param doc the document node
     * @param fragmentId the fragment identifier (an ID value within the document). This will already
     *
     * @param context the XPath dynamic context
     * @return the element within the supplied document that matches the
     * given id value; or null if no such element is found.
    */

    private static NodeInfo getFragment(DocumentInfo doc, String fragmentId, XPathContext context)
    throws XPathException {
        // TODO: we only support one kind of fragment identifier. The rules say
        // that the interpretation of the fragment identifier depends on media type,
        // but we aren't getting the media type from the URIResolver.
        if (fragmentId==null) {
            return doc;
        }
        // skip check that fragmentId is valid: this is a recoverable error
        return doc.selectID(fragmentId);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
