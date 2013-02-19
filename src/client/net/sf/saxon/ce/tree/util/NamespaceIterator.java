package client.net.sf.saxon.ce.tree.util;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class provides an iterator over the namespace codes representing the in-scope namespaces
 * of any node. It relies on nodes to implement the method
 * {@link client.net.sf.saxon.ce.om.NodeInfo#getDeclaredNamespaces(client.net.sf.saxon.ce.om.NamespaceBinding[])}.
 *
 * <p>The result does not include the XML namespace.</p>
 */
public class NamespaceIterator implements Iterator<NamespaceBinding> {

    private NodeInfo element;
    private int index;
    private NamespaceBinding next;
    private NamespaceBinding[] localDeclarations;
    HashSet<String> undeclaredPrefixes;

    /**
     * Factory method: create an iterator over the in-scope namespace bindings for an element
     * @param element the element (or other node) whose in-scope namespaces are required. If this
     * is not an element, the result will be an empty iterator
     * @return an iterator over the namespace bindings. A namespace binding represents
     * a prefix-uri binding; the prefix and URI can be obtained by reference to the name pool. This
     * iterator will represent all the in-scope namespaces, without duplicates, and respecting namespace
     * undeclarations. It does not include the XML namespace.
     */

    public static Iterator<NamespaceBinding> iterateNamespaces(NodeInfo element) {
        if (element.getNodeKind() == Type.ELEMENT) {
            return new NamespaceIterator(element);
        } else {
            return Collections.EMPTY_LIST.iterator();
        }
    }

    /**
     * Send all the in-scope namespaces for a node (except the XML namespace) to a specified receiver
     * @param element the element in question (the method does nothing if this is not an element)
     * @param receiver the receiver to which the namespaces are notified
     */

    public static void sendNamespaces(NodeInfo element, Receiver receiver) throws XPathException {
         if (element.getNodeKind() == Type.ELEMENT) {
            boolean foundDefault = false;
            for (Iterator<NamespaceBinding> iter = iterateNamespaces(element); iter.hasNext();) {
                NamespaceBinding nb = iter.next();
                if (nb.getPrefix().isEmpty()) {
                    foundDefault = true;
                }
                receiver.namespace(nb, 0);
            }
            if (!foundDefault) {
                // see bug 5857
                receiver.namespace(NamespaceBinding.DEFAULT_UNDECLARATION, 0);
            }
        }
    }

    private NamespaceIterator(NodeInfo element) {
        this.element = element;
        undeclaredPrefixes = new HashSet(8);
        index = 0;
        localDeclarations = element.getDeclaredNamespaces(null);
    }

    public boolean hasNext() {
        if (next == null && index != 0) {
            return false;
        }
        advance();
        return next != null;
    }

    public NamespaceBinding next() {
        return next;
    }

    private void advance() {
        while (true) {
            boolean ascend = index >= localDeclarations.length;
            NamespaceBinding nsCode = null;
            if (!ascend) {
                nsCode = localDeclarations[index++];
                ascend = nsCode == null;
            }
            if (ascend) {
                element = element.getParent();
                if (element != null && element.getNodeKind() == Type.ELEMENT) {
                    localDeclarations = element.getDeclaredNamespaces(localDeclarations);
                    index = 0;
                    continue;
                } else {
                    next = null;
                    return;
                }
            }
            String uri = nsCode.getURI();
            String prefix = nsCode.getPrefix();
            if (uri.isEmpty()) {
                // this is an undeclaration
                undeclaredPrefixes.add(prefix);
            } else {
                if (undeclaredPrefixes.add(prefix)) {
                    // it was added, so it's new, so return it
                    next = nsCode;
                    return;
                }
                // else it wasn't added, so we've already seen it
            }
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
