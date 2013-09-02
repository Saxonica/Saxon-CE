package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.Controller.APIcommand;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper.DocType;
import client.net.sf.saxon.ce.dom.HTMLNodeWrapper;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.FunctionLibrary;
import client.net.sf.saxon.ce.functions.ResolveURI;
import client.net.sf.saxon.ce.js.JSObjectValue;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.style.StyleElement;
import client.net.sf.saxon.ce.sxpath.AbstractStaticContext;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.trans.update.DeleteAction;
import client.net.sf.saxon.ce.trans.update.InsertAction;
import client.net.sf.saxon.ce.trans.update.PendingUpdateList;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.tree.iter.SingleNodeIterator;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

//import client.net.sf.saxon.ce.tree.util.URI.URISyntaxException;


/**
 * The compiled form of an xsl:result-document element in the stylesheet.
 * <p/>
 * The xsl:result-document element takes an attribute href="filename". The filename will
 * often contain parameters, e.g. {position()} to ensure that a different file is produced
 * for each element instance.
 * <p/>
 * There is a further attribute "format" which determines the format of the
 * output file, it identifies the name of an xsl:output element containing the output
 * format details. In addition, individual serialization properties may be specified as attributes.
 * These are attribute value templates, so they may need to be computed at run-time.
 */

public class ResultDocument extends Instruction  {

    private Expression href;
    private Expression methodExpression;
    private Expression content;
    private NamespaceResolver nsResolver;
    private Logger logger = Logger.getLogger("Xstl20Processor");

    public final static int APPEND_CONTENT = 0;
    public final static int REPLACE_CONTENT = 1;

    /**
     * Create a result-document instruction
     * @param href                    href attribute of instruction
     * @param methodExpression        format attribute of instruction
     * @param baseURI                 base URI of the instruction
     * @param nsResolver              namespace resolver
     */

    public ResultDocument(Expression href,
                          Expression methodExpression,      // AVT defining the output format
                          String baseURI,
                          NamespaceResolver nsResolver) {
        this.href = href;
        this.methodExpression = methodExpression;
        this.nsResolver = nsResolver;
        adoptChildExpression(href);
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	this.AddTraceProperty("href", href);
        }
    }

    /**
     * Set the expression that constructs the content
     * @param content the expression defining the content of the result document
     */

    public void setContentExpression(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @param visitor an expression visitor
     * @return the simplified expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        content = visitor.simplify(content);
        href = visitor.simplify(href);
        return this;
    }
    
    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.typeCheck(content, contextItemType);
        adoptChildExpression(content);
        if (href != null) {
            href = visitor.typeCheck(href, contextItemType);
            adoptChildExpression(href);
        }
        if (methodExpression != null) {
            methodExpression = visitor.typeCheck(methodExpression, contextItemType);
            adoptChildExpression(methodExpression);
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.optimize(content, contextItemType);
        adoptChildExpression(content);
        if (href != null) {
            href = visitor.optimize(href, contextItemType);
            adoptChildExpression(href);
        }
        if (methodExpression != null) {
            methodExpression = visitor.optimize(methodExpression, contextItemType);
            adoptChildExpression(methodExpression);
        }
        return this;
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        content = doPromotion(content, offer);
        if (href != null) {
            href = doPromotion(href, offer);
        }
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @param th the type hierarchy cache
     * @return the static item type of the instruction. This is empty: the result-document instruction
     *         returns nothing.
     */

    public ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }


    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        list.add(content);
        if (href != null) {
            list.add(href);
        }
        if (methodExpression != null) {
            list.add(methodExpression);
        }
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (content == original) {
            content = replacement;
            found = true;
        }
        if (href == original) {
            href = replacement;
            found = true;
        }
        return found;
    }
    
    /**
     * Return a valid URI - event if base URI is not set
     */
    public static String getValidAbsoluteURI(Controller controller, String href) throws XPathException {
    	String baseURI = (controller.getBaseOutputURI() != null && controller.getBaseOutputURI().length() > 0)?
    			controller.getBaseOutputURI() : Document.get().getURL();
        try {
            return ResolveURI.makeAbsolute(href, baseURI).toString();
     	} catch (URI.URISyntaxException ue) {
     		throw new XPathException(ue.getMessage());
     	}
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        final APIcommand command = controller.getApiCommand();
        XPathContext c2 = context.newMinorContext();
        
        int action = APPEND_CONTENT;
        if (methodExpression != null) {
            String method = methodExpression.evaluateAsString(context).toString();
            StructuredQName methodQ;
            if (method.indexOf(':') >= 0)  {
                methodQ = StructuredQName.fromLexicalQName(method, false, nsResolver);
            } else {
                methodQ = new StructuredQName("", "", method);
            }
            if ("replace-content".equals(methodQ.getLocalName())) {
                // TODO: check the namespace URI is NamespaceConstant.IXSL
                action = REPLACE_CONTENT;
            }
        }

        String hrefValue = null;
        if (href != null) {
        	hrefValue = href.evaluateAsString(context).toString();
        } else if (command == APIcommand.UPDATE_HTML) {
        	throw new XPathException("html update - no href value for result-document instruction");        	
        } else {
        	hrefValue = "result" + (controller.getResultDocumentCount() + 1);
        }
        NodeInfo target = null;
        Node targetNode = null;
        String contextNodeName = "";
        String absURI = "";
        if (command == APIcommand.TRANSFORM_TO_DOCUMENT) {
        	absURI = getValidAbsoluteURI(controller, hrefValue);
        	targetNode = XMLDOM.createDocument(absURI);
        } else if (command == APIcommand.TRANSFORM_TO_FRAGMENT || command == APIcommand.TRANSFORM_TO_HTML_FRAGMENT){
        	absURI = getValidAbsoluteURI(controller, hrefValue);
        	targetNode = HTMLDocumentWrapper.createDocumentFragment((Document)controller.getTargetNode());
        } else if (hrefValue.startsWith("#")) {
            hrefValue = hrefValue.substring(1);
            targetNode = ((Document)controller.getTargetNode()).getElementById(hrefValue); // com.google.gwt.dom.client.Document.get().getElementById(hrefValue);
        } else if (hrefValue.startsWith("?select=")) {
            String select = hrefValue.substring(8);
            AbstractStaticContext env = new AbstractStaticContext() {
                public String getURIForPrefix(String prefix) throws XPathException {
                    return null;
                }

                public Expression bindVariable(StructuredQName qName) throws XPathException {
                    return null;
                }

                public NamespaceResolver getNamespaceResolver() {
                    return null;
                }
                //override getFunctionLibrary to return that loaded for the prepared stylesheet
                public FunctionLibrary getFunctionLibrary() {
                	return controller.getPreparedStylesheet().getFunctionLibrary();
                }

            };
            ExpressionVisitor visitor = new ExpressionVisitor();
            visitor.setConfiguration(context.getConfiguration());
            visitor.setExecutable(new Executable(context.getConfiguration()));
            visitor.setStaticContext(env);
            env.setConfiguration(context.getConfiguration());
            Container container = (StyleElement)getSourceLocator();
            Expression expr = null;
            try {
            expr = ExpressionTool.make(select, env, container, 0, Token.EOF, getSourceLocator());
            } catch (Exception e) {
            	// occurs if expression contains references to variables etc. within the dynamic context
            	throw new XPathException("Error on evaluating (in static context) result-document href: " + hrefValue);
            }
            expr = visitor.typeCheck(expr, NodeKindTest.DOCUMENT);
            XPathContext c3 = context.newCleanContext();
            //context for ?select expression is the html page if an external node is the context
            Document page = (Document)controller.getTargetNode(); //com.google.gwt.dom.client.Document.get();
            Item cItem = context.getContextItem();
            
            NodeInfo currentContextItem;
            if (cItem instanceof JSObjectValue){
            	currentContextItem = null;
            } else {
            	currentContextItem = (NodeInfo)cItem;
            }
            
            boolean useCurrentContext;
            if (currentContextItem == null) {
            	useCurrentContext = false;
            } else {
            	useCurrentContext = (currentContextItem.getBaseURI().equals(page.getURL()));
            }
            
            NodeInfo contextItem;

            if (useCurrentContext) {
            	contextItem = currentContextItem;
            	if (LogConfiguration.loggingIsEnabled() && contextItem.getNodeKind() == Type.ELEMENT) {
            		contextNodeName = contextItem.getDisplayName();
            	}
            } else {
            	contextItem = new HTMLDocumentWrapper(page, page.getURL(), context.getConfiguration(), DocType.UNKNOWN);
            }
            if (LogConfiguration.loggingIsEnabled()) {
            	contextNodeName = (contextNodeName.equals("")? "" : " context node: " + contextNodeName);
            }

            AxisIterator iterator = SingleNodeIterator.makeIterator(contextItem);
            iterator.next(); // position on the single item
            c3.setCurrentIterator(iterator);
            SequenceIterator iter = expr.iterate(c3);
            Item resultItem = iter.next();
            
            if (resultItem == null) {} // do nothing
            else if (!(resultItem instanceof NodeInfo)) {
            	throw new XPathException("non-node returned by result-document href: " + hrefValue);
            } else {
            	target = (NodeInfo)resultItem;
            	targetNode = (com.google.gwt.dom.client.Node)((HTMLNodeWrapper)target).getUnderlyingNode();
            }
        }
        else if (command == APIcommand.UPDATE_HTML) {
        	throw new XPathException("expected '?select=' or '#' at start of result-document href, found: " + hrefValue);
        }
        if (targetNode == null) {
        	logger.warning("result-document target not found for href: " + hrefValue + contextNodeName);
        	return null;
        } else {
        	logger.fine("processing result-document for href: " + hrefValue + contextNodeName);
        }

        //checkAcceptableUri(context, absoluteResultURI.toString());
        //IFrameElement container = Document.get().createIFrameElement();
        
        Node container = null;
        if (command == APIcommand.UPDATE_HTML) {
            container = HTMLDocumentWrapper.createDocumentFragment((Document)controller.getTargetNode());
        } else {
            addResultDocument(context, new DocumentURI(absURI), (Document)targetNode);
        	container = targetNode;
        }

        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        
        Receiver out = controller.openResult(pipe, c2, container, action);

        try {
            content.process(c2);
            out.endDocument();
        } catch (XPathException err) {
            err.setXPathContext(context);
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
        controller.closeResult(out, c2);
        
        if (command == APIcommand.UPDATE_HTML){
	        PendingUpdateList list = controller.getPendingUpdateList();
	        if (action == REPLACE_CONTENT && command == APIcommand.UPDATE_HTML) {
	        	int existingChildren = targetNode.getChildCount();
	            for (int i=0; i<existingChildren; i++) {
	                Node child = targetNode.getChild(i);
	                list.add(new DeleteAction(child));
	            }
	        }
	
	        list.add(new InsertAction(container, targetNode, InsertAction.LAST));
        }
        //controller.setResultTree(absoluteResultURI.toString(), root);
        return null;
    }

    private void addResultDocument(XPathContext context, DocumentURI documentKey, Document doc) throws XPathException {
        Controller controller = context.getController();

            if (controller.getDocumentPool().find(documentKey.toString()) != null) {
                dynamicError("Cannot write to a URI that has already been read: " + documentKey.toString(), "XTRE1500", context);
            }
            
            if (!controller.checkUniqueOutputDestination(documentKey)) {
                dynamicError("Cannot write more than one result document to the same URI: " + documentKey.toString(),"XTDE1490" ,context);
            } else {
            	controller.addToResultDocumentPool(documentKey, doc);
                //controller.addUnavailableOutputDestination(documentKey);
            }
        //controller.setThereHasBeenAnExplicitResultDocument();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.