package client.net.sf.saxon.ce.type;
import client.net.sf.saxon.ce.om.Item;


/**
 * An implementation of ItemType that matches any item (node or atomic value)
*/

public class AnyItemType implements ItemType {

    private AnyItemType(){}

    private static AnyItemType theInstance = new AnyItemType();

    /**
     * Factory method to get the singleton instance
     */

    public static AnyItemType getInstance() {
        return theInstance;
    }

    /**
     * Test whether a given item conforms to this type
     *
     *
     * @param item The item to be tested
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item) {
        return true;
    }

    public ItemType getSuperType() {
        return null;
    }

    public AtomicType getAtomizedItemType() {
        return AtomicType.ANY_ATOMIC;
    }

    public String toString() {
        return "item()";
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return "AnyItemType".hashCode();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
