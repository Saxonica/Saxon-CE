package client.net.sf.saxon.ce.functions;

import java.util.logging.Logger;

import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionTool;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.TraceExpression;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.TraceListener;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trace.Location;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.SequenceExtent;
import client.net.sf.saxon.ce.value.Value;

/**
 * This class supports the XPath 2.0 function trace().
 * The value is traced to the Logger output if logLevel is FINE, if logLevel is FINEST then
 * TraceListener is in use, in which case the information is sent to the TraceListener
*/


public class Trace extends SystemFunction {
	
	private static Logger logger = Logger.getLogger("Trace");
    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-significant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        return argument[0].getSpecialProperties();
    }

    /**
    * Get the static cardinality
    */

    public int computeCardinality() {
        return argument[0].getCardinality();
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item val = argument[0].evaluateItem(context);
        
        if (LogConfiguration.loggingIsEnabled()) {
        	String label = argument[1].evaluateAsString(context).toString();
	        if (LogController.traceIsEnabled()) {
	            notifyListener(label, Value.asValue(val), context);
	        } else {
	                traceItem(val, label);
	        }
        }
        return val;
    }

    private void notifyListener(String label, Value val, XPathContext context) {
        TraceExpression info = new TraceExpression(this);
        info.setConstructType(Location.TRACE_CALL);
        info.setSourceLocator(this.getSourceLocator());
        info.setProperty("label", label);
        info.setProperty("value", val);
        TraceListener listener = LogController.getTraceListener();
        listener.enter(info, context);
        listener.leave(info);
    }

    public static void traceItem(/*@Nullable*/ Item val, String label) {
        if (val==null) {
            logger.info(label + ": empty sequence");
        } else {
            if (val instanceof NodeInfo) {
                logger.info(label + ": " + Type.displayTypeName(val) + ": "
                                    + Navigator.getPath((NodeInfo)val));
            } else {
                logger.info(label + ": " + Type.displayTypeName(val) + ": "
                                    + val.getStringValue());
            }
        }
    }

    /**
    * Iterate over the results of the function
    */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
            String label = argument[1].evaluateAsString(context).toString();
            int evalMode = ExpressionTool.eagerEvaluationMode(argument[0]); // eagerEvaluate not implemented in CE
            Value value = Value.asValue(ExpressionTool.evaluate(argument[0], evalMode, context));
            notifyListener(label, value, context);
            return value.iterate();
        } else {
            if (!LogConfiguration.loggingIsEnabled()) {
                return argument[0].iterate(context);
            } else {
                return new TracingIterator(argument[0].iterate(context),
                        argument[1].evaluateAsString(context).toString());
            }
        }
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {

        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
            String label = arguments[1].next().getStringValue();
            Value value = Value.asValue(SequenceExtent.makeSequenceExtent(arguments[0]));
            notifyListener(label, value, context);
            return value.iterate();
        } else {

            if (!LogConfiguration.loggingIsEnabled()) {
                return argument[0].iterate(context);
            } else {
                return new TracingIterator(argument[0].iterate(context),
                        argument[1].evaluateAsString(context).toString());
            }
        }
    }

    /**
    * Tracing Iterator class
    */

    private class TracingIterator implements SequenceIterator {

        SequenceIterator base;
        String label;
        boolean empty = true;


        public TracingIterator(SequenceIterator base, String label) {
            this.base = base;
            this.label = label;
        }

        public Item next() throws XPathException {
            Item n = base.next();
            if (n==null) {
                if (empty) {
                    traceItem(null, label);
                }
            } else {
                traceItem(n, label + " [" + position() + ']');
                empty = false;
            }
            return n;
        }

        public Item current() {
            return base.current();
        }

        public int position() {
            return base.position();
        }

        /*@NotNull*/
        public SequenceIterator getAnother() throws XPathException {
            return new TracingIterator(base.getAnother(), label);
        }

    }

	@Override
	public SystemFunction newInstance() {
		return new Trace();
	}

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
