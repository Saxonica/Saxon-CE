package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.ProxyReceiver;
import client.net.sf.saxon.ce.event.StartTagBuffer;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.DateTimeValue;

import java.util.Stack;

/**
 * This is a filter inserted into the input pipeline for processing stylesheet modules, whose
 * task is to evaluate use-when expressions and discard those parts of the stylesheet module
 * for which the use-when attribute evaluates to false.
 */

public class UseWhenFilter extends ProxyReceiver {

    private StartTagBuffer startTag;
    private NamespaceResolver nsResolver;
    private int depthOfHole = 0;
    private boolean emptyStylesheetElement = false;
    private Stack<String> defaultNamespaceStack = new Stack<String>();
    private DateTimeValue currentDateTime = DateTimeValue.getCurrentDateTime(null);

    /**
     * Create a UseWhenFilter
     * @param startTag a preceding filter on the pipeline that buffers the attributes of a start tag
     */

    public UseWhenFilter(StartTagBuffer startTag, NamespaceResolver resolver) {
        this.startTag = startTag;
        this.nsResolver = resolver;
    }

    /**
     * Start of document
     */

    public void open() throws XPathException {
        nextReceiver.open();
    }

    /**
     * Notify the start of an element.
     *
     * @param qName    integer code identifying the name of the element within the name pool.
     * @param properties  bit-significant properties of the element node
     */

    public void startElement(StructuredQName qName, int properties) throws XPathException {
        defaultNamespaceStack.push(startTag.getAttribute("", "xpath-default-namespace"));
        if (emptyStylesheetElement) {
            depthOfHole++;
            return;
        }
        if (depthOfHole == 0) {
            String useWhen;
            String uriCode = qName.getNamespaceURI();
            if (uriCode.equals(NamespaceConstant.XSLT)) {
                useWhen = startTag.getAttribute("", "use-when");
            } else {
                useWhen = startTag.getAttribute(NamespaceConstant.XSLT, "use-when");
            }
            if (useWhen != null) {
                Expression expr = null;
                try {
                    SourceLocator loc = new SourceLocator() {
                        public String getSystemId() {
                            return UseWhenFilter.this.getSystemId();
                        }

                        public String getLocation() {
                            return "use-when expression in " + getSystemId();
                        }
                    };
                    UseWhenStaticContext staticContext =
                            new UseWhenStaticContext(getConfiguration(), nsResolver, loc);
                    expr = prepareUseWhen(useWhen, staticContext, loc);
                    boolean b = evaluateUseWhen(expr, staticContext);
                    if (!b) {
                        String local = qName.getLocalName();
                        if (qName.getNamespaceURI().equals(NamespaceConstant.XSLT) &&
                                (local.equals("stylesheet") || local.equals("transform"))) {
                            emptyStylesheetElement = true;
                        } else {
                            depthOfHole = 1;
                            return;
                        }
                    }
                } catch (XPathException e) {
                    XPathException err = new XPathException("Error in use-when expression. " + e.getMessage());
                    err.setLocator(expr.getSourceLocator());
                    err.setErrorCodeQName(e.getErrorCodeQName());
                    getPipelineConfiguration().getErrorListener().error(err);
                    err.setHasBeenReported(true);
                    throw err;
                }
            }
            nextReceiver.startElement(qName, properties);
        } else {
            depthOfHole++;
        }
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param nsBinding an integer: the top half is a prefix code, the bottom half a URI code.
     *                      These may be translated into an actual prefix and URI using the name pool. A prefix code of
     *                      zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     *                      a URI of "", that is, a namespace undeclaration.
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */

    public void namespace(NamespaceBinding nsBinding, int properties) throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.namespace(nsBinding, properties);
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(StructuredQName nameCode, CharSequence value) throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.attribute(nameCode, value);
        }
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.startContent();
        }
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        defaultNamespaceStack.pop();
        if (depthOfHole > 0) {
            depthOfHole--;
        } else {
            nextReceiver.endElement();
        }
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars) throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.characters(chars);
        }
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data) {
        // these are ignored in a stylesheet
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars) throws XPathException {
        // these are ignored in a stylesheet
    }

    /**
     * Evaluate a use-when attribute
     * @param expression the expression to be evaluated
     * @param sourceLocator identifies the location of the expression in case error need to be reported
     * @return the effective boolean value of the result of evaluating the expression
     */

    public Expression prepareUseWhen(String expression, UseWhenStaticContext staticContext, SourceLocator sourceLocator) throws XPathException {
        // TODO: The following doesn't take account of xml:base attributes
        staticContext.setBaseURI(sourceLocator.getSystemId());
        staticContext.setDefaultElementNamespace(NamespaceConstant.NULL);
        for (int i=defaultNamespaceStack.size()-1; i>=0; i--) {
            String uri = (String)defaultNamespaceStack.get(i);
            if (uri != null) {
                staticContext.setDefaultElementNamespace(uri);
                break;
            }
        }
        Expression expr = ExpressionTool.make(expression, staticContext,
                staticContext, 0, Token.EOF, sourceLocator);
        expr.setContainer(staticContext);
        return expr;
    }

    public boolean evaluateUseWhen(Expression expr, UseWhenStaticContext staticContext) throws XPathException {
        ItemType contextItemType = Type.ITEM_TYPE;
        ExpressionVisitor visitor = ExpressionVisitor.make(staticContext, staticContext.getExecutable());
        expr = visitor.typeCheck(expr, contextItemType);
        int slots = ExpressionTool.allocateSlots(expr, 0);
        Controller controller = new Controller(getConfiguration());
        // TODO ensure calls on doc() are unsuccessful
        controller.setCurrentDateTime(currentDateTime);
                // this is to ensure that all use-when expressions in a module use the same date and time
        XPathContext dynamicContext = controller.newXPathContext();
        dynamicContext = dynamicContext.newCleanContext();
        dynamicContext.openStackFrame(slots);
        return expr.effectiveBooleanValue(dynamicContext);
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

