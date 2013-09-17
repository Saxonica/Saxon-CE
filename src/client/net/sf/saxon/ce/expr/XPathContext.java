package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.*;
import client.net.sf.saxon.ce.expr.instruct.ParameterSet;
import client.net.sf.saxon.ce.expr.instruct.UserFunction;
import client.net.sf.saxon.ce.expr.sort.GroupIterator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.regex.RegexIterator;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.Rule;
import client.net.sf.saxon.ce.trans.RuleManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.FocusIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;

import java.util.Arrays;

/**
* This class represents a context in which an XPath expression is evaluated.
*/

public class XPathContext {

    Controller controller;
    FocusIterator currentIterator;
    SequenceReceiver currentReceiver;
    boolean isTemporaryDestination = false;
    XPathContext caller = null;
    protected Sequence[] stackFrame;
    //Object origin = null;
    private ParameterSet localParameters;
    private ParameterSet tunnelParameters;
    private UserFunction tailCallFunction;
    private Mode currentMode;
    private Rule currentTemplate;
    private GroupIterator currentGroupIterator;
    private RegexIterator currentRegexIterator;

    private static final Sequence[] EMPTY_STACKFRAME = new Sequence[0];

    public XPathContext(Controller controller) {
        this.controller = controller;
        stackFrame = EMPTY_STACKFRAME;
    }
    /**
     * Construct a new context as a copy of another. The new context is effectively added
     * to the top of a stack, and contains a pointer to the previous context
     * @return a new context, created as a copy of this context
     */

    public XPathContext newContext() {
        XPathContext c = new XPathContext(controller);
        c.currentIterator = currentIterator;
        c.isTemporaryDestination = isTemporaryDestination;
        c.stackFrame = stackFrame;
        c.localParameters = localParameters;
        c.tunnelParameters = tunnelParameters;
        c.currentReceiver = currentReceiver;
        c.currentMode = currentMode;
        c.currentTemplate = currentTemplate;
        c.currentRegexIterator = currentRegexIterator;
        c.currentGroupIterator = currentGroupIterator;
        c.caller = this;
        c.tailCallFunction = null;
        return c;
    }

    /**
     * Construct a new context without copying (used for the context in a function call)
     * @return a new clean context
    */

    public XPathContext newCleanContext() {
        XPathContext c = new XPathContext(controller);
        c.stackFrame = EMPTY_STACKFRAME;
        c.caller = this;
        return c;
    }
                                                                        
    /**
     * Construct a new minor context. A minor context can only hold new values of the focus
     * (currentIterator) and current output destination.
     * @return a new minor context
     */

    public XPathContext newMinorContext() {
        return newContext();
    }

    /**
     * Get the local parameters for the current template call.
     * @return the supplied parameters
     */

    public ParameterSet getLocalParameters() {
        if (localParameters == null) {
            localParameters = new ParameterSet();
        }
        return localParameters;
    }

    /**
     * Set the local parameters for the current template call.
     * @param localParameters the supplied parameters
     */

    public void setLocalParameters(ParameterSet localParameters) {
        this.localParameters = localParameters;
    }

    /**
     * Get the tunnel parameters that were passed to the current function or template. This includes all
     * active tunnel parameters whether the current template uses them or not.
     * @return a ParameterSet containing the tunnel parameters
     */

    public ParameterSet getTunnelParameters() {
        return tunnelParameters;
    }

    /**
     * Set the tunnel parameters for the current template call.
     * @param tunnelParameters the supplied tunnel parameters
     */

    public void setTunnelParameters(ParameterSet tunnelParameters) {
        this.tunnelParameters = tunnelParameters;
    }

    /**
     * Set the local stack frame. This method is used when creating a Closure to support
     * delayed evaluation of expressions. The "stack frame" is actually on the Java heap, which
     * means it can survive function returns and the like.
     * @param size the number of slots needed on the stack frame
     * @param variables the array of "slots" to hold the actual variable values. This array will be
     * copied if it is too small to hold all the variables defined in the SlotManager
     */

    public void setStackFrame(int size, Sequence[] variables) {
        stackFrame = variables;
        if (variables.length != size) {
            if (variables.length > size) {
                throw new IllegalStateException(
                        "Attempting to set more local variables (" + variables.length +
                                ") than the stackframe can accommodate (" + size + ")");
            }
            stackFrame = new Sequence[size];
            System.arraycopy(variables, 0, stackFrame, 0, variables.length);
        }
    }

    /**
     * Reset the stack frame variable map, while reusing the StackFrame object itself. This
     * is done on a tail call to a different function
     * @param numberOfSlots the number of slots needed for the stack frame contents
     * @param numberOfParams the number of parameters required on the new stack frame
     */

    public void resetStackFrameMap(int numberOfSlots, int numberOfParams) {
        if (stackFrame.length != numberOfSlots) {
            Sequence[] v2 = new Sequence[numberOfSlots];
            System.arraycopy(stackFrame, 0, v2, 0, numberOfParams);
            stackFrame = v2;
        } else {
            // not strictly necessary
            Arrays.fill(stackFrame, numberOfParams, stackFrame.length, null);
        }
    }

    /**
     * Reset the local stack frame. This method is used when processing a tail-recursive function.
     * Instead of the function being called recursively, the parameters are set to new values and the
     * function body is evaluated repeatedly
     * @param fn the user function being called using tail recursion
     * @param variables the parameter to be supplied to the user function
     */

    public void requestTailCall(UserFunction fn, Sequence[] variables) {
        if (variables.length > stackFrame.length) {
            Sequence[] v2 = new Sequence[fn.getNumberOfSlots()];
            System.arraycopy(variables, 0, v2, 0, variables.length);
            stackFrame = v2;
        } else {
            System.arraycopy(variables, 0, stackFrame, 0, variables.length);
        }
        tailCallFunction = fn;
    }

    /**
     * Determine whether the body of a function is to be repeated, due to tail-recursive function calls
     * @return null if no tail call has been requested, or the name of the function to be called otherwise
     */

    public UserFunction getTailCallFunction() {
        UserFunction fn = tailCallFunction;
        tailCallFunction = null;
        return fn;
    }

    /**
     * Create a new stack frame for local variables
     * @param numberOfSlots the number of slots needed in the stack frame
     */
    public void openStackFrame(int numberOfSlots) {
        if (numberOfSlots == 0) {
            stackFrame = EMPTY_STACKFRAME;
        } else {
            stackFrame = new Sequence[numberOfSlots];
        }
    }

    /**
     * Set the current mode.
     * @param mode the new current mode
     */

    public void setCurrentMode(Mode mode) {
        this.currentMode = mode;
    }


    /**
     * Get the Controller. May return null when running outside XSLT or XQuery
     * @return the controller for this query or transformation
    */

    public Controller getController() {
        return controller;
    }

    /**
     * Get the Configuration
     * @return the Saxon configuration object
     */

    public Configuration getConfiguration() {
        return controller.getConfiguration();
    }

    /**
     * Set the calling XPathContext
     * @param caller the XPathContext of the calling expression
     */

    public void setCaller(XPathContext caller) {
        this.caller = caller;
    }

    /**
     * Get the calling XPathContext (the next one down the stack). This will be null if unknown, or
     * if the bottom of the stack has been reached.
     * @return the XPathContext of the calling expression
     */

    public XPathContext getCaller() {
        return caller;
    }

    /**
     * Set a new sequence iterator.
     * @param iter the current iterator. The context item, position, and size are determined by reference
     * to the current iterator.
     * @return the new current iterator. This will be a FocusIterator that wraps the supplied iterator,
     * maintaining the values of current() and position() as items are read. Note that this returned
     * iterator MUST be used in place of the original, or the values of current() and position() will
     * be incorrect.
     */

    public FocusIterator setCurrentIterator(SequenceIterator iter) {
        if (iter instanceof FocusIterator) {
            currentIterator = (FocusIterator)iter;
        } else {
            currentIterator = new FocusIterator(iter);
        }
        return currentIterator;
    }

    /**
     * Set a singleton focus: a context item, with position and size both equal to one
     * @param item the singleton focus
     */

    public void setSingletonFocus(Item item) {
        UnfailingIterator iter = SingletonIterator.makeIterator(item);
        FocusIterator focus = setCurrentIterator(iter);
        try {
            focus.next();
        } catch (XPathException e) {
            throw new AssertionError(e);
        }
    }

     /**
     * Get the current iterator.
     * This encapsulates the context item, context position, and context size.
     * @return the current iterator, or null if there is no current iterator
     * (which means the context item, position, and size are undefined).
    */

    public FocusIterator getCurrentIterator() {
        return currentIterator;
    }

    /**
     * Get the context position (the position of the context item)
     * @return the context position (starting at one)
     * @throws XPathException if the context position is undefined
    */

    public int getContextPosition() throws XPathException {
        if (currentIterator==null) {
            throw new XPathException("The context position is currently undefined", "FONC0001");
        }
        return currentIterator.position();
    }

    /**
     * Get the context item
     * @return the context item, or null if the context item is undefined
    */

    public Item getContextItem() {
        return (currentIterator == null ? null : currentIterator.current());
    }

    /**
     * Get the context size (the position of the last item in the current node list)
     * @return the context size
     * @throws XPathException if the context position is undefined
     */

    public int getLast() throws XPathException {
        if (currentIterator == null) {
            throw new XPathException("The context size is currently undefined", "FONC0001");
        }
        return currentIterator.last();
    }

    /**
     * Get a reference to the local stack frame for variables. Note that it's
     * the caller's job to make a local copy of this.
     * @return array of variables.
     */

    public Sequence[] getStackFrame() {
        return stackFrame;
    }

     /**
      * Get the value of a local variable, identified by its slot number
      * @param slotnumber the slot number allocated at compile time to the variable,
      * which identifies its position within the local stack frame
      * @return the value of the variable.
     */

    public Sequence evaluateLocalVariable(int slotnumber) {
        return stackFrame[slotnumber];
    }

    /**
     * Set the value of a local variable, identified by its slot number
     * @param slotnumber the slot number allocated at compile time to the variable,
     * which identifies its position within the local stack frame
     * @param value the value of the variable
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
     * Note that it is the caller's responsibility to close the Writer after use.
     * @param receiver the new destination
     * @param isFinal true if this is a "final" output destination, e.g. xsl:result-document; non-final destinations
     * such as a temporary tree inhibit creating a final destination.
     * @throws XPathException if any dynamic error occurs; and
     *     specifically, if an attempt is made to switch to a final output
     *     destination while writing a temporary tree or sequence @param isFinal true if the destination is a final result tree
     *     (either the principal output or a secondary result tree); false if not
     */

    public void changeOutputDestination(Receiver receiver, boolean isFinal) throws XPathException {
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
     * Set the SequenceReceiver to which output is to be written, marking it as a temporary (non-final)
     * output destination.
     * @param out The SequenceReceiver to be used
     */

    public void setTemporaryReceiver(SequenceReceiver out) {
        isTemporaryDestination = true;
        currentReceiver = out;
    }

    public void setTemporaryOutputState(boolean temporary) {
        this.isTemporaryDestination = temporary;
    }

    /**
     * Get the Receiver to which output is currently being written.
     * @return the current SequenceReceiver
     */
    public SequenceReceiver getReceiver() {
        return currentReceiver;
    }

    /**
     * Get the current mode.
     * @return the current mode
     */

    public Mode getCurrentMode() {
        Mode m = currentMode;
        if (m == null) {
            RuleManager rm = getController().getRuleManager();
            if (rm != null) {
                return rm.getUnnamedMode();
            } else {
                return null;
            }
        } else {
            return m;
        }
    }

    /**
     * Get the current template rule. This is used to support xsl:apply-imports and xsl:next-match
     * @return the current template rule
     */

    public Rule getCurrentTemplateRule() {
        return currentTemplate;
    }

    /**
     * Set the current template. This is used to support xsl:apply-imports. The caller
     * is responsible for remembering the previous current template and resetting it
     * after use.
     *
     * @param rule the current template rule
     */

    public void setCurrentTemplateRule(Rule rule) {
        this.currentTemplate = rule;
    }

    /**
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator() {
        return currentGroupIterator;
    }

    /**
     * Set the current grouping iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @param iterator the new current GroupIterator
     */

    public void setCurrentGroupIterator(GroupIterator iterator) {
        this.currentGroupIterator = iterator;
    }

    /**
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator() {
        return currentRegexIterator;
    }

    /**
     * Set the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @param currentRegexIterator the current regex iterator
     */

    public void setCurrentRegexIterator(RegexIterator currentRegexIterator) {
        this.currentRegexIterator = currentRegexIterator;
    }

    /**
     * Get the implicit timezone
     * @return the implicit timezone. This will be the timezone of the current date and time, and
     * all calls within a single query or transformation will return the same value. The result is
     * expressed as an offset from UTC in minutes.
     */

    public int getImplicitTimezone() {
        return getConfiguration().getImplicitTimezone();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
