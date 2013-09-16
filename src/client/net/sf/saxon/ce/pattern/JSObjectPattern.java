package client.net.sf.saxon.ce.pattern;

import com.google.gwt.core.client.JavaScriptObject;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;

public class JSObjectPattern extends NodeSetPattern {
	
	private Expression expression;
	private JavaScriptObject val = null;
	
	public JSObjectPattern(Expression exp, Configuration config) {
		super(exp, config);
		expression = exp;
	}
	
    public int getNodeKind() {
    	return Type.EMPTY;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getNodeTest() {
        return AnyJSObjectNodeTest.getInstance();
    }
    /**
     * Evaluate the pattern - it should normally be ixsl:window() - uses 
     * local variable to cache value so it can be used for a match test
     */
    public JavaScriptObject evaluate(XPathContext context) throws XPathException{
			Sequence valueRep = (Sequence)expression.evaluateItem(context);
			val = (JavaScriptObject)IXSLFunction.convertToJavaScript(valueRep);
			return val;
    }
    
    public boolean matchesObject(JavaScriptObject obj) {
    	return testEquality(val, obj);
    }
    
    private static native boolean testEquality(JavaScriptObject a, JavaScriptObject b) /*-{
		return a === b;
	}-*/;

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
