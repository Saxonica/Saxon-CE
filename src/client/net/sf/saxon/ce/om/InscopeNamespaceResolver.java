package client.net.sf.saxon.ce.om;

import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.tree.util.NamespaceIterator;
import client.net.sf.saxon.ce.type.Type;

import java.util.Iterator;

/**
 * A NamespaceResolver that resolves namespace prefixes by reference to a node in a document for which
 * those namespaces are in-scope.
 */
public class InscopeNamespaceResolver implements NamespaceResolver {

    private NodeInfo node;

    /**
     * Create a NamespaceResolver that resolves according to the in-scope namespaces
     * of a given node
     * @param node the given node
     */

    public InscopeNamespaceResolver(NodeInfo node) {
        if (node.getNodeKind() == Type.ELEMENT) {
            this.node = node;
        } else {
            this.node = node.getParent();
        }
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     *
     * @param prefix     the namespace prefix
     * @param useDefault true if the default namespace is to be used when the
     *                   prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope
     * Return "" for the no-namespace.
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if ("".equals(prefix) && !useDefault) {
            return "";
        }
        AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
        while (true) {
            NodeInfo node = (NodeInfo)iter.next();
            if (node == null) {
                break;
            }
            if (node.getLocalPart().equals(prefix)) {
                return node.getStringValue();
            }
        }
        if ("".equals(prefix)) {
            return "";
        } else {
            return null;
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        return new Iterator() {
            int phase = 0;
            Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(node);

            public boolean hasNext() {
                if (iter.hasNext()) {
                    return true;
                } else if (phase == 0) {
                    phase = 1;
                    return true;
                } else {
                    return false;
                }
            }

            public Object next() {
                if (phase == 1) {
                    phase = 2;
                    return "xml";
                } else {
                    return iter.next().getPrefix();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    /**
     * Get the node on which this namespace resolver is based
     * @return the node on which this namespace resolver is based
     */

    public NodeInfo getNode() {
        return node;
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.