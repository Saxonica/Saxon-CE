package client.net.sf.saxon.ce.expr.parser;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.om.StructuredQName;

/**
 *  A code injector can be used to add code to the expression tree (for example, diagnostic tracing code)
 *  during the process of parsing and tree construction
 */
public interface CodeInjector {

     /**
     * If tracing, wrap an expression in a trace instruction
     *
     * @param exp         the expression to be wrapped
     * @param env         the static context
     * @param construct   integer constant identifying the kind of construct
     * @param qName       the name of the construct (if applicable)
     * @return a replacement for the original expression (or the original expression unchanged)
     */

    public Expression inject(Expression exp, StaticContext env, int construct, StructuredQName qName);

    /**
     * Insert a tracing clause into a FLWOR expression
     * @param target the clause whose execution is being traced
     * @param env the static context of the containing FLWOR expression
     * @param container the container of the FLWOR expression (providing location information)
     */

    //public Clause injectClause(Clause target, StaticContext env, Container container);
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.