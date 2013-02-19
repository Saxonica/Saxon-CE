package client.net.sf.saxon.ce.trace;

import java.util.Iterator;

import client.net.sf.saxon.ce.expr.UserFunctionCall;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.XPathContextMajor;
import client.net.sf.saxon.ce.expr.instruct.ApplyTemplates;
import client.net.sf.saxon.ce.expr.instruct.CallTemplate;
import client.net.sf.saxon.ce.expr.instruct.GeneralVariable;
import client.net.sf.saxon.ce.om.StandardNames;

/**
 * This class provides a representation of the current runtime call stack, as represented by the stack
 * of XPathContext objects.
 */
public class ContextStackIterator implements Iterator<ContextStackFrame> {

    private XPathContextMajor next;

    /**
     * Create an iterator over the stack of XPath dynamic context objects, starting with the top-most
     * stackframe and working down. The objects returned by this iterator will be of class {@link ContextStackFrame}.
     * Note that only "major" context objects are considered - those that have a stack frame of their own.
     * @param context the current context
     */

    public ContextStackIterator(XPathContext context) {
        if (!(context instanceof XPathContextMajor)) {
            context = getMajorCaller(context);
        }
        next = (XPathContextMajor)context;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns the next element in the iteration.  Calling this method
     * repeatedly until the {@link #hasNext()} method returns false will
     * return each element in the underlying collection exactly once.
     *
     * @return the next element in the iteration, which will always be an instance
     * of {@link ContextStackFrame}
     * @throws java.util.NoSuchElementException
     *          iteration has no more elements.
     */
    /*@Nullable*/ public ContextStackFrame next() {
        XPathContextMajor context = next;
        if (context == null) {
            return null;
        }
        int construct = context.getOriginatingConstructType();
        Object origin = context.getOrigin();

        if (construct == Location.CONTROLLER) {
            next = getMajorCaller(context);
            return new ContextStackFrame.CallingApplication();
        } else if (construct == Location.BUILT_IN_TEMPLATE) {
            next = getMajorCaller(context);
            return new ContextStackFrame.BuiltInTemplateRule();
        }
        if (construct == Location.FUNCTION_CALL) {
            ContextStackFrame.FunctionCall sf = new ContextStackFrame.FunctionCall();
            UserFunctionCall ufc = (UserFunctionCall)origin;
            sf.setSystemId(ufc.getSystemId());
            sf.setLineNumber(-1); //(ufc.getLineNumber());
            sf.setContainer(ufc.getContainer());
            sf.setFunctionName(ufc.getFunctionName());
            sf.setContextItem(context.getContextItem());
            next = getMajorCaller(context);
            return sf;
        } else if (construct == StandardNames.XSL_APPLY_TEMPLATES) {
            ContextStackFrame.ApplyTemplates sf = new ContextStackFrame.ApplyTemplates();
            ApplyTemplates loc = (ApplyTemplates)origin;
            sf.setSystemId(loc.getSystemId());
            sf.setLineNumber(-1); //(loc.getLineNumber());
            sf.setContainer(loc.getContainer());
            sf.setContextItem(context.getContextItem());
            next = getMajorCaller(context);
            return sf;
        } else if (construct == StandardNames.XSL_CALL_TEMPLATE) {
            ContextStackFrame.CallTemplate sf = new ContextStackFrame.CallTemplate();
            CallTemplate loc = (CallTemplate)origin;
            sf.setSystemId(loc.getSystemId());
            sf.setLineNumber(-1); //(loc.getLineNumber());
            sf.setContainer(loc.getContainer());
            sf.setTemplateName(loc.getObjectName());
            sf.setContextItem(context.getContextItem());
            next = getMajorCaller(context);
            return sf;
        } else if (construct == StandardNames.XSL_VARIABLE) {
            ContextStackFrame.VariableEvaluation sf = new ContextStackFrame.VariableEvaluation();
            GeneralVariable var = ((GeneralVariable)origin);
            sf.setSystemId(var.getSystemId());
            sf.setLineNumber(-1); //(var.getLineNumber());
            sf.setContainer(var.getContainer());
            sf.setContextItem(context.getContextItem());
            sf.setVariableName(var.getVariableQName());
            next = getMajorCaller(context);
            return sf;
        } else {
            //other context changes are not considered significant enough to report
            //out.println("    In unidentified location " + construct);
            next = getMajorCaller(context);
            ContextStackFrame csf = next();
            if (csf == null) {
                // we can't return null, because hasNext() returned true...
                return new ContextStackFrame.CallingApplication();
            } else {
                return csf;
            }
        }

    }

    private static XPathContextMajor getMajorCaller(XPathContext context) {
        XPathContext caller = context.getCaller();
        while (!(caller == null || caller instanceof XPathContextMajor)) {
            caller = caller.getCaller();
        }
        return (XPathContextMajor)caller;
    }

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).
     *
     * @throws UnsupportedOperationException as the <tt>remove</tt>
     *                                       operation is not supported by this Iterator.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.