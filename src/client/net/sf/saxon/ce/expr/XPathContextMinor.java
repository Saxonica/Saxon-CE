package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.*;
import client.net.sf.saxon.ce.expr.instruct.LocalParam;
import client.net.sf.saxon.ce.expr.instruct.ParameterSet;
import client.net.sf.saxon.ce.expr.sort.GroupIterator;
import client.net.sf.saxon.ce.functions.Count;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.regex.RegexIterator;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.Rule;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.DateTimeValue;

/**
 * This class represents a minor change in the dynamic context in which an XPath expression is evaluated:
 * a "major context" object allows all aspects of the dynamic context to change, whereas
 * a "minor context" only allows changes to the focus and the destination for push output.
*/

public class XPathContextMinor implements XPathContext {

    Controller controller;
    SequenceIterator currentIterator;
    LastValue last = null;
    SequenceReceiver currentReceiver;
    boolean isTemporaryDestination = false;
    XPathContext caller = null;
    protected Sequence[] stackFrame;
    Object origin = null;
    XPathException currentException;

    /**
    * Private Constructor
    */

    protected XPathContextMinor() {
    }

    /**
    * Construct a new context as a copy of another. The new context is effectively added
    * to the top of a stack, and contains a pointer to the previous context
    */

    public XPathContextMajor newContext() {
        return XPathContextMajor.newContext(this);
    }

    /**
    * Construct a new context as a copy of another. The new context is effectively added
    * to the top of a stack, and contains a pointer to the previous context
    */
    
    public XPathContextMinor newMinorContext() {
        XPathContextMinor c = new XPathContextMinor();
        c.controller = controller;
        c.caller = this;
        c.currentIterator = currentIterator;
        c.currentReceiver = currentReceiver;
        c.last = last;
        c.isTemporaryDestination = isTemporaryDestination;
        c.stackFrame = stackFrame;
        c.currentException = currentException;
        return c;
    }

    /**
     * Set the calling XPathContext
     */

    public void setCaller(XPathContext caller) {
        this.caller = caller;
    }

    /**
    * Construct a new context without copying (used for the context in a function call)
    */

    public XPathContextMajor newCleanContext() {
        XPathContextMajor c = new XPathContextMajor(getController());
        c.setCaller(this);
        return c;
    }

    /**
     * Get the local parameters for the current template call.
     * @return the supplied parameters
     */

    public ParameterSet getLocalParameters() {
        return getCaller().getLocalParameters();
    }

    /**
     * Get the tunnel parameters for the current template call.
     * @return the supplied tunnel parameters
     */

    public ParameterSet getTunnelParameters() {
        return getCaller().getTunnelParameters();
    }

    /**
    * Get the Controller. May return null when running outside XSLT or XQuery
    */

    public final Controller getController() {
        return controller;
    }

    /**
     * Get the Configuration
     */

    public final Configuration getConfiguration() {
        return controller.getConfiguration();
    }

    /**
     * Get the calling XPathContext (the next one down the stack). This will be null if unknown, or
     * if the bottom of the stack has been reached.
     */

    public final XPathContext getCaller() {
        return caller;
    }

    /**
    * Set a new sequence iterator.
    */

    public void setCurrentIterator(SequenceIterator iter) {
        currentIterator = iter;
        last = new LastValue(-1);
    }

    /**
     * Get the current iterator.
     * This encapsulates the context item, context position, and context size.
     * @return the current iterator, or null if there is no current iterator
     * (which means the context item, position, and size are undefined).
    */

    public final SequenceIterator getCurrentIterator() {
        return currentIterator;
    }

    /**
     * Get the context position (the position of the context item)
     * @return the context position (starting at one)
     * @throws XPathException if the context position is undefined
    */

    public final int getContextPosition() throws XPathException {
        if (currentIterator==null) {
            throw new XPathException("The context position is currently undefined", "FONC0001");
        }
        return currentIterator.position();
    }

    /**
    * Get the context item
     * @return the context item, or null if the context item is undefined
    */

    public final Item getContextItem() {
        if (currentIterator==null) {
            return null;
        }
        return currentIterator.current();
    }

    /**
     * Get the context size (the position of the last item in the current node list)
     * @return the context size
     * @throws XPathException if the context position is undefined
     */

    public final int getLast() throws XPathException {
        if (currentIterator == null) {
            throw new XPathException("The context size is currently undefined", "FONC0001");
        }
        if (last.value >= 0) {
            return last.value;
        }
        if (currentIterator instanceof LastPositionFinder) {
            int count = ((LastPositionFinder)currentIterator).getLastPosition();
            if (count != -1) {
                return (last.value = count);
            }
        }
        return (last.value = Count.count(currentIterator.getAnother()));
    }

    /**
    * Determine whether the context position is the same as the context size
    * that is, whether position()=last()
    */

    public final boolean isAtLast() throws XPathException {
        return getContextPosition() == getLast();
    }

    /**
     * Get a reference to the local stack frame for variables. Note that it's
     * the caller's job to make a local copy of this. This is used for creating
     * a Closure containing a retained copy of the variables for delayed evaluation.
     * @return array of variables.
     */

    public Sequence[] getStackFrame() {
        return stackFrame;
    }


    /**
     * Get the value of a local variable, identified by its slot number
     */

    public Sequence evaluateLocalVariable(int slotnumber) {
        return stackFrame[slotnumber];
    }

    /**
     * Set the value of a local variable, identified by its slot number
     */

    public void setLocalVariable(int slotnumber, Sequence value) {
        try {
            stackFrame[slotnumber] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new AssertionError("Internal error: invalid slot number for local variable " +
                    (slotnumber == -999 ? "(No slot allocated)" : "(" + slotnumber + ")"));
        }
    }

    /**
     * Set a new output destination, supplying the output format details. <BR>
     * This affects all further output until resetOutputDestination() is called. Note that
     * it is the caller's responsibility to close the Writer after use.
     *
     * @exception XPathException if any dynamic error occurs; and
     *     specifically, if an attempt is made to switch to a final output
     *     destination while writing a temporary tree or sequence @param isFinal true if the destination is a final result tree
     *     (either the principal output or a secondary result tree); false if
     */

    public void changeOutputDestination(Receiver receiver,
                                        boolean isFinal
    )
    throws XPathException {
        if (isFinal && isTemporaryDestination) {
            XPathException err = new XPathException("Cannot switch to a final result destination while writing a temporary tree");
            err.setErrorCode("XTDE1480");
            throw err;
        }
        if (!isFinal) {
            isTemporaryDestination = true;
        }
        PipelineConfiguration pipe = receiver.getPipelineConfiguration();
        ComplexContentOutputter out = new ComplexContentOutputter();
        out.setPipelineConfiguration(pipe);

		// add a filter to remove duplicate namespaces

        NamespaceReducer ne = new NamespaceReducer();
        ne.setUnderlyingReceiver(receiver);
        ne.setPipelineConfiguration(pipe);

        out.setReceiver(receiver);

        currentReceiver = out;
    }

    /**
     * Set the output destination to write to a sequence. <BR>
     * This affects all further output until resetOutputDestination() is called.
     *
     * @param out The SequenceReceiver to be used
     */

    public void setTemporaryReceiver(SequenceReceiver out) {
        isTemporaryDestination = true;
        currentReceiver = out;
    }

    /**
     * Change the Receiver to which output is written
     */

    public void setReceiver(SequenceReceiver receiver) {
        currentReceiver = receiver;
    }

    /**
     * Get the Receiver to which output is currently being written.
     *
     * @return the current Receiver
     */
    public final SequenceReceiver getReceiver() {
        return currentReceiver;
    }

    /**
    * Use local parameter. This is called when a local xsl:param element is processed.
    * If a parameter of the relevant name was supplied, it is bound to the xsl:param element.
    * Otherwise the method returns false, so the xsl:param default will be evaluated
    * @param qName    The fingerprint of the parameter name
    * @param binding        The XSLParam element to bind its value to
    * @param isTunnel      True if a tunnel parameter is required, else false
    * @return ParameterSet.NOT_SUPPLIED, ParameterSet.SUPPLIED, or ParameterSet.SUPPLIED_AND_CHECKED
    */

    public int useLocalParameter(StructuredQName qName,
                                     LocalParam binding,
                                     boolean isTunnel) throws XPathException {
        return getCaller().useLocalParameter(qName, binding, isTunnel);
    }

    /**
     * Get the current mode.
     * @return the current mode
     */

    public Mode getCurrentMode() {
        return getCaller().getCurrentMode();
    }

    /**
     * Get the current template. This is used to support xsl:apply-imports
     *
     * @return the current template
     */

    public Rule getCurrentTemplateRule() {
        return getCaller().getCurrentTemplateRule();
    }

    /**
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator() {
        return getCaller().getCurrentGroupIterator();
    }

    /**
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator() {
        return getCaller().getCurrentRegexIterator();
    }

   /**
     * Get the current date and time for this query or transformation.
     * All calls during one transformation return the same answer.
     *
     * @return Get the current date and time. This will deliver the same value
     *      for repeated calls within the same transformation
     */

    public DateTimeValue getCurrentDateTime() {
        return controller.getCurrentDateTime();
    }

    /**
     * Get the implicit timezone, as a positive or negative offset from UTC in minutes.
     * The range is -14hours to +14hours
     * @return the implicit timezone as an offset from UTC in minutes
     */

    public final int getImplicitTimezone() {
        return controller.getImplicitTimezone();
    }

    /**
     * Mark the context as being in (or not in) temporary output state
     * @param temp true to set temporary output state; false to unset it
     */

    public void setTemporaryOutputState(boolean temp) {
        isTemporaryDestination = temp;
    }

    /**
     * Set the current exception (in saxon:catch)
     * @param exception the current exception
     */

    public void setCurrentException(XPathException exception) {
        currentException = exception;
    }

    /**
     * Get the current exception (in saxon:catch)
     * @return the current exception, or null if there is none defined
     */

    public XPathException getCurrentException() {
        return currentException;
    }

    // Note: consider eliminating this class. A new XPathContextMinor is created under two circumstances,
    // (a) when the focus changes (i.e., a new current iterator), and (b) when the current
    // receiver changes. We could handle these by maintaining a stack of iterators and a stack of
    // receivers in the XPathContextMajor object. Adding a new iterator or receiver to the stack would
    // generally be cheaper than creating the new XPathContextMinor object. The main difficulty (in the
    // case of iterators) is knowing when to pop the stack: currently we rely on the garbage collector.
    // We can only really do this when the iterator comes to its end, which is difficult to detect.
    // Perhaps we should try to do static allocation, so that fixed slots are allocated for different
    // minor-contexts within a Procedure, and a compiled expression that uses the focus knows which
    // slot to look in.

    // Investigated the above Sept 2008. On xmark, with a 100Mb input, the path expression
    // count(site/people/person/watches/watch) takes just 13ms to execute (compared with 6500ms for building
    // the tree). Only 6 context objects are created while doing this. This doesn't appear to be a productive
    // area to look for new optimizations.

    /**
     * Container for cached value of the last() function.
     * This is shared between all context objects that share the same current iterator.
     * Introduced in 9.2 to handle the case where a new context is introduced when the current
     * outputter changes, without changing the current iterator: in this case the cached value
     * was being lost because each call on last() used a different context object.
     */

    protected static class LastValue {
        public int value = 0;

        public LastValue(int count) {
            value = count;
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
