package client.net.sf.saxon.ce.tree.wrapper;

import client.net.sf.saxon.ce.event.Stripper;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;

import java.util.HashMap;

/**
  * A SpaceStrippedDocument represents a view of a real Document in which selected
  * whitespace text nodes are treated as having been stripped.
  */

public class SpaceStrippedDocument extends SpaceStrippedNode implements DocumentInfo {

    private Stripper stripper;
    private boolean preservesSpace;
    private HashMap<String, Object> userData;

    /**
     * Create a space-stripped view of a document
     * @param doc the underlying document
     * @param stripper an object that contains the rules defining which whitespace
     * text nodes are to be absent from the view
     */

    public SpaceStrippedDocument(DocumentInfo doc, Stripper stripper) {
        node = doc;
        parent = null;
        docWrapper = this;
        this.stripper = stripper;
        preservesSpace = findPreserveSpace(doc);
    }

    /**
     * Get the document's stripper
     */

    public Stripper getStripper() {
        return stripper;
    }

	/**
	* Get the unique document number
	*/

	public int getDocumentNumber() {
	    return node.getDocumentNumber();
	}

    /**
    * Get the element with a given ID, if any
    * @param id the required ID value
    * @return the element with the given ID value, or null if there is none.
    */

    public NodeInfo selectID(String id) {
        NodeInfo n = ((DocumentInfo)node).selectID(id);
        if (n==null) {
            return null;
        } else {
            return makeWrapper(n, this, null);
        }
    }

    /**
     * Determine whether the wrapped document contains any xml:space="preserve" attributes. If it
     * does, we will look for them when stripping individual nodes. It's more efficient to scan
     * the document in advance checking for xml:space attributes than to look for them every time
     * we hit a whitespace text node.
     */

    private static boolean findPreserveSpace(DocumentInfo doc) {
        UnfailingIterator iter = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
        while (true) {
            NodeInfo node = (NodeInfo)iter.next();
            if (node == null) {
                return false;
            }
            String val = Navigator.getAttributeValue(node, NamespaceConstant.XML, "space");
            if ("preserve".equals(val)) {
                return true;
            }
        }
    }

    /**
     * Does the stripped document contain any xml:space="preserve" attributes?
     */

    public boolean containsPreserveSpace() {
        return preservesSpace;
    }

    /**
     * Set user data on the document node. The user data can be retrieved subsequently
     * using {@link #getUserData}
     * @param key   A string giving the name of the property to be set. Clients are responsible
     *              for choosing a key that is likely to be unique. Must not be null. 
     * @param value The value to be set for the property. May be null, which effectively
     *              removes the existing value for the property.
     */

    public void setUserData(String key, Object value) {
        if (userData == null) {
            userData = new HashMap(4);
        }
        if (value == null) {
            userData.remove(key);
        } else {
            userData.put(key, value);
        }
    }

    /**
     * Get user data held in the document node. This retrieves properties previously set using
     * {@link #setUserData}
     * @param key A string giving the name of the property to be retrieved.
     * @return the value of the property, or null if the property has not been defined.
     */

    public Object getUserData(String key) {
        if (userData == null) {
            return null;
        } else {
            return userData.get(key);
        }
    }    
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.