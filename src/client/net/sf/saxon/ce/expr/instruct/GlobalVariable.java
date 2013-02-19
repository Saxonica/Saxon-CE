package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.SingletonClosure;

/**
* A compiled global variable in a stylesheet or query. <br>
*/

public class GlobalVariable extends GeneralVariable implements Container {

    private Executable executable;
    private SlotManager stackFrameMap = null;

    /**
     * Create a global variable
     */

    public GlobalVariable(){}

    /**
     * Get the executable containing this global variable
     * @return the containing executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the containing executable
     * @param executable the executable that contains this global variable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the granularity of the container.
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 2;
    }

    /**
     * The expression that initializes a global variable may itself use local variables.
     * In this case a stack frame needs to be allocated while evaluating the global variable
     * @param map The stack frame map for local variables used while evaluating this global
     * variable.
     */

    public void setContainsLocals(SlotManager map) {
        this.stackFrameMap = map;
    }

    /**
     * Is this a global variable?
     * @return true (yes, it is a global variable)
     */

    public boolean isGlobal() {
        return true;
    }


    /**
    * Process the variable declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        // This code is not used. A global variable is not really an instruction, although
        // it is modelled as such, and it will be evaluated using the evaluateVariable() call
        return null;
    }

    /**
     * Evaluate the variable. That is,
     * get the value of the select expression if present or the content
     * of the element otherwise, either as a tree or as a sequence
    */

    public ValueRepresentation getSelectValue(XPathContext context) throws XPathException {
        if (select==null) {
            throw new AssertionError("*** No select expression for global variable $" +
                    getVariableQName().getDisplayName() + "!!");
        } else {
            try {
                XPathContextMajor c2 = context.newCleanContext();
                UnfailingIterator initialNode =
                        SingletonIterator.makeIterator(c2.getController().getContextForGlobalVariables());
                initialNode.next();
                c2.setCurrentIterator(initialNode);
                if (stackFrameMap != null) {
                    c2.openStackFrame(stackFrameMap);
                }
                return ExpressionTool.evaluate(select, evaluationMode, c2, referenceCount);
            } catch (XPathException e) {
                if (!getVariableQName().getNamespaceURI().equals(NamespaceConstant.SAXON_GENERATED_GLOBAL)) {
                    e.setIsGlobalError(true);
                }
                throw e;
            }
        }
    }

    /**
    * Evaluate the variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        final Bindery b = controller.getBindery();

        final ValueRepresentation v = b.getGlobalVariable(getSlotNumber());

        if (v != null) {
            return v;
        } else {
            return actuallyEvaluate(context);
        }
    }

    /**
     * Evaluate the global variable, and save its value for use in subsequent references.
     * @param context the XPath dynamic context
     * @return the value of the variable
     * @throws XPathException if evaluation fails
     */

    protected ValueRepresentation actuallyEvaluate(XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        final Bindery b = controller.getBindery();
        try {
            // Set a flag to indicate that the variable is being evaluated. This is designed to prevent
            // (where possible) the same global variable being evaluated several times in different threads
            boolean go = b.setExecuting(this);
            if (!go) {
                // some other thread has evaluated the variable while we were waiting
                return b.getGlobalVariable(getSlotNumber());
            }

            ValueRepresentation value = getSelectValue(context);
            return b.saveGlobalVariableValue(this, value);

        } catch (XPathException err) {
            b.setNotExecuting(this);
            if (err instanceof XPathException.Circularity) {
                err.setErrorCode("XTDE0640");
                err.setXPathContext(context);
                // Detect it more quickly the next time (in a pattern, the error is recoverable)
                SingletonClosure closure = new SingletonClosure(new ErrorExpression(err), context);
                b.defineGlobalVariable(this, closure);
                err.setLocator(getSourceLocator());
                throw err;
            } else {
                throw err;
            }
        }
    }

 }

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
