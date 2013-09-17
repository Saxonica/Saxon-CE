package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.expr.sort.GroupIterator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.Sequence;
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
 * This class represents a "major context" in which an XPath expression is evaluated:
 * a "major context" object allows all aspects of the dynamic context to change, whereas
 * a "minor context" only allows changes to the focus and the destination for push output.
*/

public class XPathContextMajor extends XPathContextMinor {

    private ParameterSet localParameters;
    private ParameterSet tunnelParameters;
    private UserFunction tailCallFunction;
    private Mode currentMode;
    private Rule currentTemplate;
    private GroupIterator currentGroupIterator;
    private RegexIterator currentRegexIterator;

    /**
     * Constructor should only be called by the Controller,
     * which acts as a XPathContext factory.
     * @param controller the Controller
     */

    public XPathContextMajor(Controller controller) {
        this.controller = controller;
        stackFrame = EMPTY_STACKFRAME;
        origin = controller;
    }

    private static final Sequence[] EMPTY_STACKFRAME = new Sequence[0];

    /**
    * Private Constructor
    */

    private XPathContextMajor() {
    }

    /**
    * Constructor for use in free-standing Java applications.
     * @param item the item to use as the initial context item. If this is null,
     * the comtext item is initially undefined (which will cause a dynamic error
     * if it is referenced).
     * @param exec the Executable
    */

    public XPathContextMajor(Item item, Executable exec) {
        controller = new Controller(exec.getConfiguration(), exec);
        if (item != null) {
            UnfailingIterator iter = SingletonIterator.makeIterator(item);
            currentIterator = new FocusIterator(iter);
            try {
                currentIterator.next();
            } catch (XPathException err) {
                throw new AssertionError(err);
            }
        }
        origin = controller;
    }

    /**
    * Construct a new context as a copy of another. The new context is effectively added
    * to the top of a stack, and contains a pointer to the previous context. The
    */

    public XPathContextMajor newContext() {
        XPathContextMajor c = new XPathContextMajor();
        c.controller = controller;
        c.currentIterator = currentIterator;
        c.stackFrame = stackFrame;
        c.localParameters = localParameters;
        c.tunnelParameters = tunnelParameters;
        c.currentReceiver = currentReceiver;
        c.isTemporaryDestination = isTemporaryDestination;
        c.currentMode = currentMode;
        c.currentTemplate = currentTemplate;
        c.currentRegexIterator = currentRegexIterator;
        c.currentGroupIterator = currentGroupIterator;
        c.caller = this;
        c.tailCallFunction = null;
        return c;
    }

    /**
     * Create a new "major" context (one that is capable of holding a stack frame with local variables
     * @param prev the previous context (the one causing the new context to be created)
     * @return the new major context
     */

    public static XPathContextMajor newContext(XPathContextMinor prev) {
        XPathContextMajor c = new XPathContextMajor();
        XPathContext p = prev;
        while (!(p instanceof XPathContextMajor)) {
            p = p.getCaller();
        }
        c.controller = p.getController();
        c.currentIterator = prev.getCurrentIterator();
        c.stackFrame = prev.getStackFrame();
        c.localParameters = p.getLocalParameters();
        c.tunnelParameters = p.getTunnelParameters();
        c.currentReceiver = prev.currentReceiver;
        c.isTemporaryDestination = prev.isTemporaryDestination;
        c.currentMode = p.getCurrentMode();
        c.currentTemplate = p.getCurrentTemplateRule();
        c.currentRegexIterator = p.getCurrentRegexIterator();
        c.currentGroupIterator = p.getCurrentGroupIterator();
        c.caller = prev;
        c.tailCallFunction = null;
        return c;
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
     * Get the tunnel parameters for the current template call.
     * @return the supplied tunnel parameters
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
     * Get the current mode.
     * @return the current mode. May return null if the current mode is the default mode.
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
     * Get the current template. This is used to support xsl:apply-imports
     *
     * @return the current template
     */

    public Rule getCurrentTemplateRule() {
        return currentTemplate;
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
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator() {
        return currentGroupIterator;
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
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator() {
        return currentRegexIterator;
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

        ParameterSet params = (isTunnel ? getTunnelParameters() : localParameters);
    	if (params==null) {
            return ParameterSet.NOT_SUPPLIED;
        }
        int index = params.getIndex(binding.getParameterId());
        if (index < 0) {
            return ParameterSet.NOT_SUPPLIED;
        }
        Sequence val = params.getValue(index);
        stackFrame[binding.getSlotNumber()] = val;
        boolean checked = params.isTypeChecked(index);
        return (checked ? ParameterSet.SUPPLIED_AND_CHECKED : ParameterSet.SUPPLIED);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
