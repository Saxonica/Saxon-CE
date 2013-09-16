package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.SaxonceApi;
import client.net.sf.saxon.ce.client.HTTPHandler;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.ResolveURI;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.IntegerValue;
import com.google.gwt.dom.client.Node;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.user.client.Timer;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The compiled form of an ixsl:schedule-action instruction in the stylesheet.
 */

public class ScheduleExecution extends Instruction {

    private CallTemplate call;
    private Expression wait;
    private Expression href;
    private String staticBaseURI;
    private static Logger logger = Logger.getLogger("ScheduleExecution");

    /**
     * Create a schedule-execution instruction
     *
     * @param call the instruction to be executed
     * @param wait expression giving the amount of time to wait, in milliseconds
     */

    public ScheduleExecution(CallTemplate call, Expression wait, Expression href) {
        this.call = call;
        this.wait = wait;
        this.href = href;
        adoptChildExpression(call);
        adoptChildExpression(wait);
        adoptChildExpression(href);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @param visitor an expression visitor
     * @return the simplified expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        call = (CallTemplate) visitor.simplify(call);
        wait = visitor.simplify(wait);
        href = visitor.simplify(href);
        staticBaseURI = visitor.getStaticContext().getBaseURI();
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        call = (CallTemplate) visitor.typeCheck(call, contextItemType);
        wait = visitor.typeCheck(wait, contextItemType);
        href = visitor.typeCheck(href, contextItemType);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        call = (CallTemplate) visitor.optimize(call, contextItemType);
        wait = visitor.optimize(wait, contextItemType);
        href = visitor.optimize(href, contextItemType);
        return this;
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        call = (CallTemplate) doPromotion(call, offer);
        wait = doPromotion(wait, offer);
        href = doPromotion(href, offer);
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction. This is empty: the set-attribute instruction
     *         returns nothing.
     */

    public ItemType getItemType() {
        return EmptySequenceTest.getInstance();
    }


    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(call, wait, href);
    }


    public TailCall processLeavingTail(final XPathContext context) throws XPathException {
        // Evaluate the expressions before creating the template package, because the template package clears all variables!
        int time = 1;
        String hrefVal = null;
        if (href != null) {
            hrefVal = href.evaluateAsString(context).toString();
        } else if (wait != null) {
            time = ((IntegerValue)wait.evaluateItem(context)).intValue();
        }
        final CallTemplate.CallTemplatePackage pack = (CallTemplate.CallTemplatePackage) call.processLeavingTail(context);
        if (href != null) {
            URI abs;
            try {
                abs = ResolveURI.makeAbsolute(hrefVal, staticBaseURI);
            } catch (URI.URISyntaxException e) {
                throw new XPathException("Cannot resolve URI " + hrefVal, e);
            }
            final String uri = abs.toString();

            logger.log(Level.FINE, "Aynchronous GET for: " + abs);
            final HTTPHandler hr = new HTTPHandler();

            hr.doGet(uri, new RequestCallback() {

                public void onError(Request request, Throwable exception) {
                    //hr.setErrorMessage(exception.getMessage());
                    String msg = "HTTP Error " + exception.getMessage() + " for URI " + uri;
                    logger.log(Level.SEVERE, msg);
                    if (SaxonceApi.doThrowJsExceptions()) {
                        throw new RuntimeException(exception.getMessage());
                    }
                }

                public void onResponseReceived(Request request, Response response) {
                    int statusCode = response.getStatusCode();
                    if (statusCode == 200) {
                        Logger.getLogger("ResponseReceived").fine("GET Ok for: " + uri);
                        Node responseNode;
                        try {
                            responseNode = (Node) XMLDOM.parseXML(response.getText());
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Failed to parse XML: " + e.getMessage());
                            if (SaxonceApi.doThrowJsExceptions()) {
                                throw new RuntimeException(e.getMessage());
                            }
                            return;
                        }
                        DocumentInfo doc = context.getConfiguration().wrapXMLDocument(responseNode, uri);
                        UnfailingIterator focus = SingletonIterator.makeIterator(doc);
                        focus.next();
                        XPathContext c2 = context.newMinorContext();
                        c2.setCurrentIterator(focus);
                        pack.setEvaluationContext(c2);
                        try {
                            TailCall tc = pack.processLeavingTail();
                            while (tc != null) {
                                tc = tc.processLeavingTail();
                            }
                            context.getController().getPendingUpdateList().apply(context);
                        } catch (XPathException e) {
                            logger.log(Level.SEVERE, "In async document processing: " + e.getMessage());
                            if (SaxonceApi.doThrowJsExceptions()) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }


                    } else if (statusCode < 400) {
                        // transient
                    } else {
                        String msg = "HTTP Error " + statusCode + " " + response.getStatusText() + " for URI " + uri;
                        logger.log(Level.SEVERE, msg);
                    }
                } // ends inner method
            }); // ends doGet method call
        } else {
            Timer t = new Timer() {
                public void run() {
                    //Window.setTitle("Timer fired " + serial++);
                    boolean success = false;
                    logger.fine("processing ixsl:schedule-action");
                    if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                        LogController.openTraceListener();
                    }
                    try {
                        TailCall tc = pack.processLeavingTail();
                        while (tc != null) {
                            tc = tc.processLeavingTail();
                        }
                        context.getController().getPendingUpdateList().apply(context);
                        success = true;
                    } catch (Exception err) {
                        logger.log(Level.SEVERE, "In delayed event: " + err.getMessage());
                        if (SaxonceApi.doThrowJsExceptions()) {
                            throw new RuntimeException(err.getMessage());
                        }
                    }
                    if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                        LogController.closeTraceListener(success);
                    }
                }
            };
            //Window.setTitle("Timer started " + serial + " (time " + time + "ms)");
            t.schedule(time);
        }

        return null;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
