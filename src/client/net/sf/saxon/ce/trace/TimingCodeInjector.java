package client.net.sf.saxon.ce.trace;

import client.net.sf.saxon.ce.expr.Container;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.om.StructuredQName;

/**
 * A code injector that wraps the body of a template or function in a TraceExpression, which causes
 * the TimingTraceListener to be notified at the start and end of the function/template evaluation
 */
public class TimingCodeInjector extends TraceCodeInjector {

    /**
     * If tracing, wrap an expression in a trace instruction
     *
     * @param exp         the expression to be wrapped
     * @param env         the static context
     * @param construct   integer constant identifying the kind of construct
     * @param qName       the name of the construct (if applicable)
     * @return the expression that does the tracing
     */

    public Expression inject(Expression exp, StaticContext env, int construct, StructuredQName qName) {
        if (construct == StandardNames.XSL_FUNCTION || construct == StandardNames.XSL_TEMPLATE) {
            return super.inject(exp, env, construct, qName);
        } else {
            return exp;
        }
    }

//    public Clause injectClause(Clause target, StaticContext env, Container container) {
//        return null;
//    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
