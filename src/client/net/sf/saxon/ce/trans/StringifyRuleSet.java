package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.XPathContextMajor;
import client.net.sf.saxon.ce.expr.instruct.ApplyTemplates;
import client.net.sf.saxon.ce.expr.instruct.ParameterSet;
import client.net.sf.saxon.ce.expr.instruct.TailCall;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.Type;

/**
 *  The built-in rule set used for 1.0 and 2.0, which for document and element nodes does an apply-templates
 *  to children, and for text nodes and attribute nodes copies the node.
 */
public class StringifyRuleSet implements BuiltInRuleSet {

    private static StringifyRuleSet THE_INSTANCE = new StringifyRuleSet();

    /**
     * Get the singleton instance of this class
     * @return the singleton instance
     */

    public static StringifyRuleSet getInstance() {
        return THE_INSTANCE;
    }

    private StringifyRuleSet() {};

    /**
     * Perform the built-in template action for a given node.
     * @param node         the node to be processed
     * @param parameters   the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param context      the dynamic evaluation context
     * @param sourceLocator   location of the instruction (apply-templates, apply-imports etc) that caused
     *                     the built-in template to be invoked
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs
     */

    public void process(NodeInfo node, ParameterSet parameters,
                        ParameterSet tunnelParams, XPathContext context,
                        SourceLocator sourceLocator) throws XPathException {
        switch(node.getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                SequenceIterator iter = node.iterateAxis(Axis.CHILD, AnyNodeTest.getInstance());
                XPathContextMajor c2 = context.newContext();
	            TailCall tc = ApplyTemplates.applyTemplates(
                        iter, context.getCurrentMode(), parameters, tunnelParams, c2, sourceLocator);
                while (tc != null) {
                    tc = tc.processLeavingTail();
                }
	            return;
	        case Type.TEXT:
	            // NOTE: I tried changing this to use the text node's copy() method, but
	            // performance was worse
	        case Type.ATTRIBUTE:
	            context.getReceiver().characters(node.getStringValue());
	            return;
	        case Type.COMMENT:
	        case Type.PROCESSING_INSTRUCTION:
	        case Type.NAMESPACE:
	            // no action
        }

    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.



