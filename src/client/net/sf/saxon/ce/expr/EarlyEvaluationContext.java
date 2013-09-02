package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.instruct.LocalParam;
import client.net.sf.saxon.ce.expr.instruct.ParameterSet;
import client.net.sf.saxon.ce.expr.sort.GroupIterator;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.regex.RegexIterator;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.NoDynamicContextException;
import client.net.sf.saxon.ce.trans.Rule;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.DateTimeValue;

/**
 * This class is an implementation of XPathContext used when evaluating constant sub-expressions at
 * compile time.
 */

public class EarlyEvaluationContext implements XPathContext {

    private Configuration config;

    /**
     * Create an early evaluation context, used for evaluating constant expressions at compile time
     * @param config the Saxon configuration
     */

    public EarlyEvaluationContext(Configuration config) {
        this.config = config;
    }

    /**
     * Set a new output destination, supplying the output format details. <BR>
     * Note that it is the caller's responsibility to close the Writer after use.
     *
     * @param isFinal true if the destination is a final result tree
     *                (either the principal output or a secondary result tree); false if
     *                it is a temporary tree, xsl:attribute, etc.
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs; and
     *          specifically, if an attempt is made to switch to a final output
     *          destination while writing a temporary tree or sequence
     */

    public void changeOutputDestination(Receiver receiver, boolean isFinal
    ) throws XPathException {
        notAllowed();
    }

    /**
     * Get the value of a local variable, identified by its slot number
     */

    public ValueRepresentation evaluateLocalVariable(int slotnumber) {
        notAllowed();
        return null;
    }

    /**
     * Get the calling XPathContext (the next one down the stack). This will be null if unknown, or
     * if the bottom of the stack has been reached.
     */

    public XPathContext getCaller() {
        return null;
    }

    /**
     * Get the Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the context item
     *
     * @return the context item, or null if the context item is undefined
     */

    public Item getContextItem() {
        return null;
    }

    /**
     * Get the context position (the position of the context item)
     *
     * @return the context position (starting at one)
     * @throws XPathException
     *          if the context position is undefined
     */

    public int getContextPosition() throws XPathException {
        XPathException err = new XPathException("The context position is undefined");
        err.setErrorCode("FONC0001");
        throw err;
    }

    /**
     * Get the Controller. May return null when running outside XSLT or XQuery
     */

    public Controller getController() {
        return null;
    }

    /**
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     *
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator() {
        notAllowed();
        return null;
    }

    /**
     * Get the current iterator.
     * This encapsulates the context item, context position, and context size.
     *
     * @return the current iterator, or null if there is no current iterator
     *         (which means the context item, position, and size are undefined).
     */

    public SequenceIterator getCurrentIterator() {
        return null;
    }

    /**
     * Get the current mode.
     *
     * @return the current mode
     */

    public Mode getCurrentMode() {
        notAllowed();
        return null;
    }

    /**
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     *
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator() {
        return null;
    }

    /**
     * Get the current template. This is used to support xsl:apply-imports
     *
     * @return the current template
     */

    public Rule getCurrentTemplateRule() {
        return null;
    }

    /**
     * Get the context size (the position of the last item in the current node list)
     *
     * @return the context size
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if the context position is undefined
     */

    public int getLast() throws XPathException {
        XPathException err = new XPathException("The context item is undefined");
        err.setErrorCode("XPDY0002");
        throw err;
    }

    /**
     * Get the local (non-tunnel) parameters that were passed to the current function or template
     *
     * @return a ParameterSet containing the local parameters
     */

    public ParameterSet getLocalParameters() {
        notAllowed();
        return null;
    }

    /**
     * Get the Receiver to which output is currently being written.
     *
     * @return the current Receiver
     */
    public SequenceReceiver getReceiver() {
        notAllowed();
        return null;
    }

    /**
     * Get a reference to the local stack frame for variables. Note that it's
     * the caller's job to make a local copy of this. This is used for creating
     * a Closure containing a retained copy of the variables for delayed evaluation.
     *
     * @return array of variables.
     */

    public StackFrame getStackFrame() {
        notAllowed();
        return null;
    }

    /**
     * Get the tunnel parameters that were passed to the current function or template. This includes all
     * active tunnel parameters whether the current template uses them or not.
     *
     * @return a ParameterSet containing the tunnel parameters
     */

    public ParameterSet getTunnelParameters() {
        notAllowed();
        return null;
    }

    /**
     * Determine whether the context position is the same as the context size
     * that is, whether position()=last()
     */

    public boolean isAtLast() throws XPathException {
        XPathException err = new XPathException("The context item is undefined");
        err.setErrorCode("XPDY0002");
        throw err;
    }

    /**
     * Construct a new context without copying (used for the context in a function call)
     */

    public XPathContextMajor newCleanContext() {
        notAllowed();
        return null;
    }

    /**
     * Construct a new context as a copy of another. The new context is effectively added
     * to the top of a stack, and contains a pointer to the previous context
     */

    public XPathContextMajor newContext() {
        Controller controller = new Controller(config);
        return controller.newXPathContext();
//        notAllowed();
//        return null;
    }

    /**
     * Construct a new minor context. A minor context can only hold new values of the focus
     * (currentIterator) and current output destination.
     */

    public XPathContextMinor newMinorContext() {
        return newContext().newMinorContext();
//        notAllowed();
//        return null;
    }

    /**
     * Set the calling XPathContext
     */

    public void setCaller(XPathContext caller) {
        // no-op
    }

    /**
     * Set a new sequence iterator.
     */

    public void setCurrentIterator(SequenceIterator iter) {
        notAllowed();
    }

    /**
     * Set the value of a local variable, identified by its slot number
     */

    public void setLocalVariable(int slotnumber, ValueRepresentation value) {
        notAllowed();
    }

    /**
     * Change the Receiver to which output is written
     */

    public void setReceiver(SequenceReceiver receiver) {
        notAllowed();
    }

    /**
     * Set the receiver to which output is to be written, marking it as a temporary (non-final)
     * output destination.
     *
     * @param out The SequenceOutputter to be used
     */

    public void setTemporaryReceiver(SequenceReceiver out) {
        notAllowed();
    }

    /**
     * Use local parameter. This is called when a local xsl:param element is processed.
     * If a parameter of the relevant name was supplied, it is bound to the xsl:param element.
     * Otherwise the method returns false, so the xsl:param default will be evaluated
     *
     * @param qName The fingerprint of the parameter name
     * @param binding     The XSLParam element to bind its value to
     * @param isTunnel    True if a tunnel parameter is required, else false
     * @return true if a parameter of this name was supplied, false if not
     */

    public int useLocalParameter(StructuredQName qName, LocalParam binding, boolean isTunnel) throws XPathException {
        return ParameterSet.NOT_SUPPLIED;
    }

    /**
     * Get the current date and time. This implementation always throws a
     * NoDynamicContextException.
     * @return the current date and time. All calls within a single query or transformation
     * will return the same value
     */

    public DateTimeValue getCurrentDateTime() throws NoDynamicContextException {
        throw new NoDynamicContextException("current-dateTime");
    }

    /**
     * Get the implicit timezone, as a positive or negative offset from UTC in minutes.
     * The range is -14hours to +14hours. This implementation always throws a
     * NoDynamicContextException.
     * @return the implicit timezone, as an offset from UTC in minutes
     */

    public int getImplicitTimezone() throws NoDynamicContextException{
        throw new NoDynamicContextException("implicit-timezone");
    }


    /**
     * Get the current exception
     * @return the current exception, or null if there is none defined
     */

    public XPathException getCurrentException() {
        return null;
    }

    /**
     * Throw an error for operations that aren't supported when doing early evaluation of constant
     * subexpressions
     */

    private void notAllowed() {
        throw new UnsupportedOperationException("Internal error: early evaluation of subexpression with no context");
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

