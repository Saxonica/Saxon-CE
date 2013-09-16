package client.net.sf.saxon.ce.type;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.value.AtomicValue;



/**
 * This class contains static information about types and methods for constructing type codes.
 * The class is never instantiated.
 */

public abstract class Type  {

    // Note that the integer codes representing node kinds are the same as
    // the codes allocated in the DOM interface, while the codes for built-in
    // atomic types are fingerprints allocated in StandardNames. These two sets of
    // codes must not overlap!

    /**
     * Type representing an element node - element()
     */

    public static final short ELEMENT = 1;
    /**
     * Item type representing an attribute node - attribute()
     */
    public static final short ATTRIBUTE = 2;
    /**
     * Item type representing a text node - text()
     */
    public static final short TEXT = 3;
    /**
     * Item type representing a text node stored in the tiny tree as compressed whitespace
     */
    public static final short WHITESPACE_TEXT = 4;
    /**
     * Item type representing a processing-instruction node
     */
    public static final short PROCESSING_INSTRUCTION = 7;
    /**
     * Item type representing a comment node
     */
    public static final short COMMENT = 8;
    /**
     * Item type representing a document node
     */
    public static final short DOCUMENT = 9;
    /**
     * Item type representing a doctype node 
     */
    public static final short DOCUMENT_TYPE = 10;
    /**
     * Item type representing a namespace node
     */
    public static final short NAMESPACE = 13;
    /**
     * Dummy node kind used in the tiny tree to mark the end of the tree
     */
    public static final short STOPPER = 11;
    /**
     * Dummy node kind used in the tiny tree to contain a parent pointer
     */
    public static final short PARENT_POINTER = 12;

    /**
     * An item type that matches any node
     */

    public static final short NODE = 0;

    public static final ItemType NODE_TYPE = AnyNodeTest.getInstance();

    /**
     * An item type that matches any item
     */

    public static final short ITEM = 88;

    public static final ItemType ITEM_TYPE = AnyItemType.getInstance();

    /**
     * A type number for function()
     */

    public static final short FUNCTION = 99;

    public static final short MAX_NODE_TYPE = 13;
    /**
     * Item type that matches no items (corresponds to SequenceType empty())
     */
    public static final short EMPTY = 15;    // a test for this type will never be satisfied

    private Type() {
    }

    /**
     * Test whether a given type is (some subtype of) node()
     *
     * @param type The type to be tested
     * @return true if the item type is node() or a subtype of node()
     */

    public static boolean isNodeType(ItemType type) {
        return type instanceof NodeTest;
    }

    /**
     * Get the ItemType of an Item
     *
     * @param item the item whose type is required
     * @return the item type of the item
     */

    public static ItemType getItemType(Item item) {
        if (item instanceof AtomicValue) {
            return ((AtomicValue)item).getItemType();
        } else if (item instanceof NodeInfo) {
            return NodeKindTest.makeNodeKindTest(((NodeInfo)item).getNodeKind());
        } else { //if (item instanceof FunctionItem) {
            return null;
        } 
    }


    /**
     * Output (for diagnostics) a representation of the type of an item. This
     * does not have to be the most specific type
     * @param item the item whose type is to be displayed
     * @return a string representation of the type of the item
     */

    public static String displayTypeName(Item item) {
        if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo)item;
            switch (node.getNodeKind()) {
                case DOCUMENT:
                    return "document-node()";
                case ELEMENT:
                    return "element(" +
                            ((NodeInfo)item).getDisplayName() + ')';
                case ATTRIBUTE:
                    return "attribute(" +
                            ((NodeInfo)item).getDisplayName()+ ')';
                case TEXT:      return "text()";
                case COMMENT:   return "comment()";
                case PROCESSING_INSTRUCTION:
                                return "processing-instruction()";
                case NAMESPACE: return "namespace()";
                default:        return "";
            }
        } else {
            return ((AtomicValue)item).getItemType().toString();
        }
    }

    /**
     * Get a type that is a common supertype of two given item types
     *
     *
     * @param t1 the first item type
     * @param t2 the second item type
     * @return the item type that is a supertype of both
     *     the supplied item types
     */

    public static ItemType getCommonSuperType(ItemType t1, ItemType t2) {
        if (t1 instanceof EmptySequenceTest) {
            return t2;
        }
        if (t2 instanceof EmptySequenceTest) {
            return t1;
        }
        TypeHierarchy th = TypeHierarchy.getInstance();
        int r = th.relationship(t1, t2);
        if (r == TypeHierarchy.SAME_TYPE) {
            return t1;
        } else if (r == TypeHierarchy.SUBSUMED_BY) {
            return t2;
        } else if (r == TypeHierarchy.SUBSUMES) {
            return t1;
        } else {
            return getCommonSuperType(t2.getSuperType(th), t1);
            // eventually we will hit a type that is a supertype of t2. We reverse
            // the arguments so we go up each branch of the tree alternately.
            // If we hit the root of the tree, one of the earlier conditions will be satisfied,
            // so the recursion will stop.
        }
    }

    /**
     * Determine whether two primitive atomic types are comparable under the rules for ValueComparisons
     * (that is, untyped atomic values treated as strings)
     * @param t1 the first type to compared.
     * This must be a primitive atomic type
     * @param t2 the second type to compared.
     * This must be a primitive atomic type
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     * if testing for an equality comparison (eq, ne)
     * @return true if the types are comparable, as defined by the rules of the "eq" operator
     */

    public static boolean isComparable(AtomicType t1, AtomicType t2, boolean ordered) {
        if (t1 == t2) {
            return true;
        }
        if (t1.equals(AtomicType.ANY_ATOMIC) || t2.equals(AtomicType.ANY_ATOMIC)) {
            return true; // meaning we don't actually know at this stage
        }
        if (t1.equals(AtomicType.UNTYPED_ATOMIC)) {
            t1 = AtomicType.STRING;
        }
        if (t2.equals(AtomicType.UNTYPED_ATOMIC)) {
            t2 = AtomicType.STRING;
        }
        if (t1.equals(AtomicType.ANY_URI)) {
            t1 = AtomicType.STRING;
        }
        if (t2.equals(AtomicType.ANY_URI)) {
            t2 = AtomicType.STRING;
        }
        if (t1.isPrimitiveNumeric()) {
            t1 = AtomicType.NUMERIC;
        }
        if (t2.isPrimitiveNumeric()) {
            t2 = AtomicType.NUMERIC;
        }
        if (!ordered) {
            if (t1.equals(AtomicType.DAY_TIME_DURATION)) {
                t1 = AtomicType.DURATION;
            }
            if (t2.equals(AtomicType.DAY_TIME_DURATION)) {
                t2 = AtomicType.DURATION;
            }
            if (t1.equals(AtomicType.YEAR_MONTH_DURATION)) {
                t1 = AtomicType.DURATION;
            }
            if (t2.equals(AtomicType.YEAR_MONTH_DURATION)) {
                t2 = AtomicType.DURATION;
            }
        }
        return t1 == t2;
    }

    /**
     * Determine whether two primitive atomic types are comparable under the rules for GeneralComparisons
     * (that is, untyped atomic values treated as comparable to anything)
     * @param t1 the first type to compared.
     * This must be a primitive atomic type
     * @param t2 the second type to compared.
     * This must be a primitive atomic type
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     * if testing for an equality comparison (eq, ne)
     * @return true if the types are comparable, as defined by the rules of the "eq" operator
     */

    public static boolean isGenerallyComparable(AtomicType t1, AtomicType t2, boolean ordered) {
        return t1.equals(AtomicType.ANY_ATOMIC)
                || t2.equals(AtomicType.ANY_ATOMIC)
                || t1.equals(AtomicType.UNTYPED_ATOMIC)
                || t2.equals(AtomicType.UNTYPED_ATOMIC)
                || isComparable(t1, t2, ordered);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.