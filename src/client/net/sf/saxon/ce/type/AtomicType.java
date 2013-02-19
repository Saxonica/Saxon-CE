package client.net.sf.saxon.ce.type;

/**
 * Interface for atomic types (these are either built-in atomic types
 * or user-defined atomic types). An AtomicType is both an ItemType (a possible type
 * for items in a sequence) and a SchemaType (a possible type for validating and
 * annotating nodes).
 */
public interface AtomicType extends SchemaType, ItemType {

    /**
     * Determine whether the atomic type is ordered, that is, whether less-than and greater-than comparisons
     * are permitted
     * @return true if ordering operations are permitted
     */

    public boolean isOrdered();

    /**
     * Determine whether the atomic type is a primitive type.  The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; and all supertypes of these (xs:anyAtomicType, xs:numeric, ...)
     * @return true if the type is considered primitive under the above rules
     */

    public boolean isPrimitiveType();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
