package client.net.sf.saxon.ce.trace;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.om.StructuredQName;

/**
 * A code injector that wraps every expression (other than a literal) in a TraceExpression, which causes
 * a TraceListener to be notified when the expression is evaluated
 */
public class XSLTTraceCodeInjector extends TraceCodeInjector {

    /**
     * If tracing, wrap an expression in a trace instruction
     *
     * @param exp         the expression to be wrapped
     * @param env         the static context
     * @param construct   integer constant identifying the kind of construct
     * @param qName       the name of the construct (if applicable)
     * @return the expression that does the tracing
     */

    public Expression inject(Expression exp, /*@NotNull*/ StaticContext env, int construct, StructuredQName qName) {
        if (XSLTTraceListener.tagName(construct) != null) {
            return super.inject(exp, env, construct, qName);
        } else {
            return exp;
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
