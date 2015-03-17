package client.net.sf.saxon.ce.dom;

import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.StripSpaceRules;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.LinkedList;
import java.util.Stack;

/**
 * The Sanitizer is responsible for preprocessing a supplied DOM document to make it more suitable for XSLT/XPath
 * processing. The main operation is removal of whitespace-only text nodes as defined using
 * xsl:strip-space and xsl:preserve-space directives in the stylesheet. In future the operation can also potentially
 * merge adjacent text nodes and add namespace information to the tree.
 */
public class Sanitizer {

    private StripSpaceRules rules;
    private Stack<String> xmlSpaceStack = new Stack<String>();

    public Sanitizer(StripSpaceRules rules) {
        this.rules = rules;
    }

    public void sanitize(HTMLDocumentWrapper doc) {
        xmlSpaceStack.push("default");
        sanitizeChildren(doc.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT), false);
    }

    private void sanitizeChildren(UnfailingIterator iterator, boolean strip) {
        LinkedList<HTMLNodeWrapper> strippedNodes = null;
        while (true) {
            NodeInfo node = (NodeInfo)iterator.next();
            if (node == null) {
                break;
            }
            if (node.getNodeKind() == Type.ELEMENT) {
                String xmlSpace = Navigator.getAttributeValue(node, NamespaceConstant.XML, "space");
                if (xmlSpace == null) {
                    xmlSpace = xmlSpaceStack.peek();
                }
                xmlSpaceStack.push(xmlSpace);
                boolean stripChildren = rules.isSpaceStripped(node.getNodeName());
                sanitizeChildren(node.iterateAxis(Axis.CHILD, AnyNodeTest.getInstance()), stripChildren);
                xmlSpaceStack.pop();
            } else if (strip && node.getNodeKind() == Type.TEXT &&
                    !xmlSpaceStack.peek().equals("preserve") && Whitespace.isWhite(node.getStringValue())) {
                if (strippedNodes == null) {
                    strippedNodes = new LinkedList<HTMLNodeWrapper>();
                }
                strippedNodes.addFirst((HTMLNodeWrapper)node);
            }
        }
        if (strippedNodes != null) {
            for (HTMLNodeWrapper node : strippedNodes) {
                node.stripTextNode();
            }
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

