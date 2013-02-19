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
* This class represents a context in which an XPath expression is evaluated.
*/

public interface XPathContext {


    /**
     * Construct a new context as a copy of another. The new context is effectively added
     * to the top of a stack, and contains a pointer to the previous context
     * @return a new context, created as a copy of this context
     */

    public XPathContextMajor newContext();

    /**
     * Construct a new context without copying (used for the context in a function call)
     * @return a new clean context
    */

    public XPathContextMajor newCleanContext();
                                                                        
    /**
     * Construct a new minor context. A minor context can only hold new values of the focus
     * (currentIterator) and current output destination.
     * @return a new minor context
     */

    public XPathContextMinor newMinorContext();

    /**
     * Get the local (non-tunnel) parameters that were passed to the current function or template
     * @return a ParameterSet containing the local parameters
     */

    public ParameterSet getLocalParameters();

    /**
     * Get the tunnel parameters that were passed to the current function or template. This includes all
     * active tunnel parameters whether the current template uses them or not.
     * @return a ParameterSet containing the tunnel parameters
     */

    public ParameterSet getTunnelParameters();

    /**
     * Get the Controller. May return null when running outside XSLT or XQuery
     * @return the controller for this query or transformation
    */

    public Controller getController();

    /**
     * Get the Configuration
     * @return the Saxon configuration object
     */

    public Configuration getConfiguration();

    /**
     * Get the Name Pool
     * @return the name pool
     */

    public NamePool getNamePool();

    /**
     * Set the calling XPathContext
     * @param caller the XPathContext of the calling expression
     */

    public void setCaller(XPathContext caller);

    /**
     * Get the calling XPathContext (the next one down the stack). This will be null if unknown, or
     * if the bottom of the stack has been reached.
     * @return the XPathContext of the calling expression
     */

    public XPathContext getCaller();

    /**
     * Set a new sequence iterator.
     * @param iter the current iterator. The context item, position, and size are determined by reference
     * to the current iterator.
     */

    public void setCurrentIterator(SequenceIterator iter);

     /**
     * Get the current iterator.
     * This encapsulates the context item, context position, and context size.
     * @return the current iterator, or null if there is no current iterator
     * (which means the context item, position, and size are undefined).
    */

    public SequenceIterator getCurrentIterator();

    /**
     * Get the context position (the position of the context item)
     * @return the context position (starting at one)
     * @throws XPathException if the context position is undefined
    */

    public int getContextPosition() throws XPathException;

    /**
     * Get the context item
     * @return the context item, or null if the context item is undefined
    */

    public Item getContextItem();
    /**
     * Get the context size (the position of the last item in the current node list)
     * @return the context size
     * @throws XPathException if the context position is undefined
     */

    public int getLast() throws XPathException;

    /**
     * Determine whether the context position is the same as the context size
     * that is, whether position()=last(). In many cases this has better performance
     * than a direct comparison, because it does not require reading to the end of the
     * sequence.
     * @return true if the context position is the same as the context size.
    */

    public boolean isAtLast() throws XPathException;

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
                                     boolean isTunnel) throws XPathException;

    /**
     * Get a reference to the local stack frame for variables. Note that it's
     * the caller's job to make a local copy of this. This is used for creating
     * a Closure containing a retained copy of the variables for delayed evaluation.
     * @return array of variables.
     */

    public StackFrame getStackFrame();

     /**
      * Get the value of a local variable, identified by its slot number
      * @param slotnumber the slot number allocated at compile time to the variable,
      * which identifies its position within the local stack frame
      * @return the value of the variable.
     */

    public ValueRepresentation evaluateLocalVariable(int slotnumber);

    /**
     * Set the value of a local variable, identified by its slot number
     * @param slotnumber the slot number allocated at compile time to the variable,
     * which identifies its position within the local stack frame
     * @param value the value of the variable
     */

    public void setLocalVariable(int slotnumber, ValueRepresentation value);

    /**
     * Set a new output destination, supplying the output format details. <BR>
     * Note that it is the caller's responsibility to close the Writer after use.
     *
     * @exception XPathException if any dynamic error occurs; and
     *     specifically, if an attempt is made to switch to a final output
     *     destination while writing a temporary tree or sequence @param isFinal true if the destination is a final result tree
     *     (either the principal output or a secondary result tree); false if not
     */

    public void changeOutputDestination(Receiver receiver,
                                        boolean isFinal
    ) throws XPathException;

    /**
     * Set the SequenceReceiver to which output is to be written, marking it as a temporary (non-final)
     * output destination.
     * @param out The SequenceReceiver to be used
     */

    public void setTemporaryReceiver(SequenceReceiver out);

    /**
     * Change the SequenceReceiver to which output is written
     * @param receiver the SequenceReceiver to be used
     */

    public void setReceiver(SequenceReceiver receiver);

    /**
     * Get the Receiver to which output is currently being written.
     * @return the current SequenceReceiver
     */
    public SequenceReceiver getReceiver();

    /**
     * Get the current mode.
     * @return the current mode
     */

    public Mode getCurrentMode();

    /**
     * Get the current template rule. This is used to support xsl:apply-imports and xsl:next-match
     * @return the current template rule
     */

    public Rule getCurrentTemplateRule();

    /**
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator();

    /**
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator();

    /**
     * Get the current date and time
     * @return the current date and time. All calls within a single query or transformation
     * will return the same value
     */

    public DateTimeValue getCurrentDateTime() throws NoDynamicContextException;

    /**
     * Get the implicit timezone
     * @return the implicit timezone. This will be the timezone of the current date and time, and
     * all calls within a single query or transformation will return the same value. The result is
     * expressed as an offset from UTC in minutes.
     */

    public int getImplicitTimezone() throws NoDynamicContextException;

    public XPathException getCurrentException();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
