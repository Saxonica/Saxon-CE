package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.BooleanValue;


public class Lang extends SystemFunction {

    public Lang newInstance() {
        return new Lang();
    }

 
    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (argument.length==1) {
            if (contextItemType == null) {
                typeError("The context item for lang() is undefined", "XPDY0002");
            } else if (contextItemType instanceof BuiltInAtomicType) {
                typeError("The context item for lang() is not a node", "XPDY0002");
            }
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo target;
        if (argument.length > 1) {
            target = (NodeInfo)argument[1].evaluateItem(c);
        } else {
            Item current = c.getContextItem();
            if (current==null) {
                dynamicError("The context item for lang() is undefined", "XPDY0002");
            }
            if (!(current instanceof NodeInfo)) {
                dynamicError("The context item for lang() is not a node", "XPDY0002");
            }
            target = (NodeInfo)current;
        }
        final Item arg0Val = argument[0].evaluateItem(c);
        final String testLang = (arg0Val==null ? "" : arg0Val.getStringValue());
        boolean b = isLang(testLang, target);
        return BooleanValue.get(b);
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return (argument.length == 1 ? StaticProperty.DEPENDS_ON_CONTEXT_ITEM : 0);
    }

    /**
    * Test whether the context node has the given language attribute
    * @param arglang the language being tested
    * @param target the target node
    */

    public static boolean isLang(String arglang, NodeInfo target) {
        String doclang = null;
        NodeInfo node = target;

        while(node!=null) {
            doclang = Navigator.getAttributeValue(node, NamespaceConstant.XML, "lang");
            if (doclang!=null) {
                break;
            }
            node = node.getParent();
            if (node==null) {
                return false;
            }
        }

        if (doclang==null) {
            return false;
        }

        while (true) {
            if (arglang.equalsIgnoreCase(doclang)) {
                return true;
            }
            int hyphen = doclang.indexOf("-");
            if (hyphen<0) {
                return false;
            }
            doclang = doclang.substring(0, hyphen);
        }
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
