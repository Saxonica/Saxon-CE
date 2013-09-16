package client.net.sf.saxon.ce.type;
import client.net.sf.saxon.ce.Configuration;
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
     * Determine whether this item type is atomic (that is, whether it can ONLY match
     * atomic values)
     *
     * @return false: this type can match nodes or atomic values
     */

    public boolean isAtomicType() {
        return false;
    }

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        return true;
    }

    public ItemType getSuperType(TypeHierarchy th) {
        return null;
    }

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public ItemType getPrimitiveItemType() {
        return this;
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
