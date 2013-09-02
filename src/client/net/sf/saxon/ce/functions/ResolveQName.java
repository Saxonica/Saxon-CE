package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.QNameValue;


/**
* This class supports the resolve-QName function in XPath 2.0
*/

public class ResolveQName extends SystemFunction {

    public ResolveQName newInstance() {
        return new ResolveQName();
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0 == null) {
            return null;
        }

        CharSequence lexicalQName = arg0.getStringValue();

        NodeInfo element = (NodeInfo)argument[1].evaluateItem(context);
        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        StructuredQName qName;

        try {
            qName= StructuredQName.fromLexicalQName(lexicalQName, true, resolver);
        } catch (XPathException e) {
            e.maybeSetLocation(getSourceLocator());
            throw e;
        }

        return new QNameValue(qName, BuiltInAtomicType.QNAME);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.