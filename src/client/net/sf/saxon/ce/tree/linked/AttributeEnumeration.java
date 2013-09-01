package client.net.sf.saxon.ce.tree.linked;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.tree.iter.AxisIteratorImpl;
import client.net.sf.saxon.ce.type.Type;

/**
* AttributeEnumeration is an enumeration of all the attribute nodes of an Element.
*/

final class AttributeEnumeration extends AxisIteratorImpl  {

    private ElementImpl element;
    private AttributeCollection attributes;
    private NodeTest nodeTest;
    private NodeInfo next;
    private int index;
    private int length;

    /**
    * Constructor
    * @param node: the element whose attributes are required. This may be any type of node,
    * but if it is not an element the enumeration will be empty
    * @param nodeTest: condition to be applied to the names of the attributes selected
    */

    public AttributeEnumeration(NodeImpl node, NodeTest nodeTest) {

        this.nodeTest = nodeTest;

        if (node.getNodeKind()==Type.ELEMENT) {
            element = (ElementImpl)node;
            attributes = element.getAttributeList();
            AttributeCollection attlist = element.getAttributeList();
            index = 0;

            if (nodeTest instanceof NameTest) {
            	NameTest test = (NameTest)nodeTest;
                index = attlist.findByStructuredQName(test.getRequiredNodeName());

                if (index<0) {
                    next = null;
                } else {
                    next = new AttributeImpl(element, index);
                    index = 0;
                    length = 0; // force iteration to select one node only
                }

            } else  {
                index = 0;
                length = attlist.getLength();
                advance();
            }
        }
        else {      // if it's not an element, or if we're not looking for attributes,
                    // then there's nothing to find
            next = null;
            index = 0;
            length = 0;
        }
    }

    /**
    * Get the next node in the iteration, or null if there are no more.
    */

    public Item next() {
        if (next == null) {
            current = null;
            position = -1;
            return null;
        } else {
            current = next;
            position++;
            advance();
            return current;
        }
    }

    /**
    * Move to the next node in the enumeration.
    */

    private void advance() {
        while (true) {
            if (index >= length) {
                next = null;
                return;
            } else {
                next = new AttributeImpl(element, index);
                index++;
                if (nodeTest.matches(next)) {
                    return;
                }
            }
        } 
    }

    /**
    * Get another enumeration of the same nodes
    */

    public SequenceIterator getAnother() {
        return new AttributeEnumeration(element, nodeTest);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }
}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
