package client.net.sf.saxon.ce.trace;

import java.util.ArrayList;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.expr.TraceExpression;
import client.net.sf.saxon.ce.expr.parser.CodeInjector;
import client.net.sf.saxon.ce.om.StructuredQName;



/**
 * A code injector that wraps every expression (other than a literal) in a TraceExpression, which causes
 * a TraceListener to be notified when the expression is evaluated
 */
public class TraceCodeInjector implements CodeInjector {

    /**
     * If tracing, wrap an expression in a trace instruction
     *
     *
     * @param exp         the expression to be wrapped
     * @param env         the static context
     * @param construct   integer constant identifying the kind of construct
     * @param qName       the name of the construct (if applicable)
     * @return the expression that does the tracing
     */

    public Expression inject(Expression exp, /*@NotNull*/ StaticContext env, StructuredQName construct, StructuredQName qName) {
        if (exp instanceof Literal) {
            return exp;
        }
        TraceExpression trace = new TraceExpression(exp);
        //ExpressionTool.copyLocationInfo(exp, trace);
        trace.setNamespaceResolver(env.getNamespaceResolver());
        trace.setConstructType(construct);
        trace.setObjectName(qName);
        ArrayList<String[]> properties = exp.getTraceProperties();
        if (properties != null) {
        	for (String[] property: properties) {
        		trace.setProperty(property[0], property[1]);
        	}
        }
        //trace.setObjectNameCode(objectNameCode);
        return trace;
    }

    /**
     * If tracing, add a clause to a FLWOR expression that can be used to monitor requests for
     * tuples to be processed
     * @param target the clause whose evaluation is to be traced (or otherwise monitored)
     * @param env the static context of the containing FLWOR expression
     * @param container the container of the containing FLWOR Expression
     * @return the new clause to do the tracing; or null if no tracing is required at this point
     */

    //public Clause injectClause(Clause target, StaticContext env, Container container) {
    //    return new TraceClause(target, env.getNamespaceResolver(), container);
    //}
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
