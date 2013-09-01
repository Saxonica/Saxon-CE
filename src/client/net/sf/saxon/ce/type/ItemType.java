package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamePool;



/**
 * ItemType is an interface that allows testing of whether an Item conforms to an
 * expected type. ItemType represents the types in the type hierarchy in the XPath model,
 * as distinct from the schema model: an item type is either item() (matches everything),
 * a node type (matches nodes), an atomic type (matches atomic values), or empty()
 * (matches nothing). Atomic types, represented by the class AtomicType, are also
 * instances of SimpleType in the schema type hierarchy. Node Types, represented by
 * the class NodeTest, are also Patterns as used in XSLT.
 *
 * <p>Saxon assumes that apart from {@link AnyItemType} (which corresponds to <code>item()</item>
 * and matches anything), every ItemType will be either a {@link BuiltInAtomicType}, or a
 *  {@link client.net.sf.saxon.ce.pattern.NodeTest}. User-defined implementations of ItemType must therefore extend one of those
 * three classes/interfaces.</p>
 * @see BuiltInAtomicType
 * @see client.net.sf.saxon.ce.pattern.NodeTest
*/

public interface ItemType  {

    /**
     * Determine whether this item type is atomic (that is, whether it can ONLY match
     * atomic values)
     * @return true if this is ANY_ATOMIC_TYPE or a subtype thereof
     */

    public boolean isAtomicType();

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config);

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     * <p>
     * In fact the concept of "supertype" is not really well-defined, because the types
     * form a lattice rather than a hierarchy. The only real requirement on this function
     * is that it returns a type that strictly subsumes this type, ideally as narrowly
     * as possible.
     * @return the supertype, or null if this type is item()
     * @param th the type hierarchy cache
     */

    public ItemType getSuperType(TypeHierarchy th);

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that integer, xs:dayTimeDuration, and xs:yearMonthDuration
     * are considered to be primitive types.
     */

    public ItemType getPrimitiveItemType();

    /**
     * Produce a representation of this type name for use in error messages.
     * Where this is a QName, it will use conventional prefixes
     * @param pool the name pool
     * @return a string representation of the type, in notation resembling but not necessarily
     * identical to XPath syntax
     */

    public String toString(NamePool pool);

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     * @return  the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public BuiltInAtomicType getAtomizedItemType();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
