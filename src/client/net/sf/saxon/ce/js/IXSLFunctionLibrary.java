package client.net.sf.saxon.ce.js;

import client.net.sf.saxon.ce.expr.Container;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.functions.FunctionLibrary;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Library of Saxon-defined extension functions for the browser environment
 */
public class IXSLFunctionLibrary implements FunctionLibrary {
    public boolean hasFunctionSignature(StructuredQName functionName, int arity) {
        String uri = functionName.getNamespaceURI();
        if (NamespaceConstant.IXSL.equals(uri)) {
            return true;    // TODO: implement this more accurately!
        } else if (NamespaceConstant.JS.equals(uri)) {
            return exists(functionName.getLocalName());
        }
        return false;
    }

    private static native boolean exists(String member)
    /*-{
       return !!$wnd[member];
    }-*/;

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env, Container container) throws XPathException {
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        if (NamespaceConstant.IXSL.equals(uri)) {
            if (!hasFunctionSignature(functionName, staticArgs.length)) {
                return null;
            }
            return new IXSLFunction(local, staticArgs);
        } else if (NamespaceConstant.JS.equals(uri)) {
            Expression[] args = new Expression[staticArgs.length + 2];
            System.arraycopy(staticArgs, 0, args, 2, staticArgs.length);
            args[0] = new IXSLFunction("window", new Expression[0]);
            args[1] = StringLiteral.makeLiteral(new StringValue(local));
            return new IXSLFunction("call", args);
        } else if (NamespaceConstant.EXSLT_COMMON.equals(uri) && local.equals("node-set") && staticArgs.length == 1) {
                // exslt:node-set() is a no-op in XSLT 2.0
                return staticArgs[0];
        } else {
            return null;
        }
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.