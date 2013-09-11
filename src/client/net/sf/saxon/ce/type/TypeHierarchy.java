package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.expr.sort.SetUtils;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.pattern.NodeTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This class exists to provide answers to questions about the type hierarchy. Because
 * such questions are potentially expensive, it caches the answers.
 *
 * <p>In Saxon-CE, because the number of types is bounded, and the same for all applications,
 * the TypeHierarchy cache is in static data.</p>
 */

public class TypeHierarchy {

    private static TypeHierarchy THE_INSTANCE = new TypeHierarchy();

    public static TypeHierarchy getInstance() {
        return THE_INSTANCE;
    }

    private Map<ItemTypePair, Integer> map;

    /**
     * Constant denoting relationship between two types: A is the same type as B
     */
    public static final int SAME_TYPE = 0;
    /**
     * Constant denoting relationship between two types: A subsumes B
     */
    public static final int SUBSUMES = 1;
    /**
     * Constant denoting relationship between two types: A is subsumed by B
     */
    public static final int SUBSUMED_BY = 2;
    /**
     * Constant denoting relationship between two types: A overlaps B
     */
    public static final int OVERLAPS = 3;
    /**
     * Constant denoting relationship between two types: A is disjoint from B
     */
    public static final int DISJOINT = 4;

    //private String[] relnames = {"SAME", "SUBSUMES", "SUBSUMED_BY", "OVERLAPS", "DISJOINT"};

    /**
     * Create the type hierarchy cache for a configuration
     */

    public TypeHierarchy(){
        map = new HashMap<ItemTypePair, Integer>();
    }

    /**
     * Determine whether type A is type B or one of its subtypes, recursively
     *
     * @param subtype identifies the first type
     * @param supertype identifies the second type
     * @return true if the first type is the second type or a (direct or
     *     indirect) subtype of the second type
     */

    public boolean isSubType(ItemType subtype, ItemType supertype) {
        int relation = relationship(subtype, supertype);
        return (relation==SAME_TYPE || relation==SUBSUMED_BY);
    }

    /**
     * Determine the relationship of one item type to another.
     * @param t1 the first item type
     * @param t2 the second item type
     * @return {@link #SAME_TYPE} if the types are the same; {@link #SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link #SUBSUMED_BY} if the second type subsumes the first;
     * {@link #OVERLAPS} if the two types overlap (have a non-empty intersection, but neither
     * subsumes the other); {@link #DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    public int relationship(ItemType t1, ItemType t2) {
        if (t1 == null) {
            throw new NullPointerException();
        }
        if (t1.equals(t2)) {
            return SAME_TYPE;
        }
        ItemTypePair pair = new ItemTypePair(t1, t2);
        Integer result = map.get(pair);
        if (result == null) {
            result = computeRelationship(t1, t2);
            map.put(pair, result);
        }
        return result;
    }

    /**
     * Determine the relationship of one item type to another.
     * @param t1 the first item type
     * @param t2 the second item type
     * @return {@link #SAME_TYPE} if the types are the same; {@link #SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link #SUBSUMED_BY} if the second type subsumes the first;
     * {@link #OVERLAPS} if the two types overlap (have a non-empty intersection, but neither
     * subsumes the other); {@link #DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    private int computeRelationship(ItemType t1, ItemType t2) {
        //System.err.println("computeRelationship " + t1 + ", " + t2);
        if (t1 == t2) {
            return SAME_TYPE;
        }
        if (t1 instanceof AnyItemType) {
            if (t2 instanceof AnyItemType) {
                return SAME_TYPE;
            } else {
                return SUBSUMES;
            }
        } else if (t2 instanceof AnyItemType) {
            return SUBSUMED_BY;
        } else if (t1.isAtomicType()) {
            if (t2 instanceof NodeTest) {
                return DISJOINT;
            } else {
                ItemType t = t2;
                while (t.isAtomicType()) {
                    if (t1 == t) {
                        return SUBSUMES;
                    }
                    t = t.getSuperType(this);
                }
                t = t1;
                while (t.isAtomicType()) {
                    if (t == t2) {
                        return SUBSUMED_BY;
                    }
                    t = t.getSuperType(this);
                }
                return DISJOINT;
            }
        } else if (t1 instanceof NodeTest) {
            if (t2.isAtomicType()) {
                return DISJOINT;
            } else {
                // both types are NodeTests
                if (t1 instanceof AnyNodeTest) {
                    if (t2 instanceof AnyNodeTest) {
                        return SAME_TYPE;
                    } else {
                        return SUBSUMES;
                    }
                } else if (t2 instanceof AnyNodeTest) {
                    return SUBSUMED_BY;
                } else if (t1 instanceof EmptySequenceTest) {
                    return DISJOINT;
                } else if (t2 instanceof EmptySequenceTest) {
                    return DISJOINT;
                } else {
                    // first find the relationship between the node kinds allowed
                    int nodeKindRelationship;
                    int m1 = ((NodeTest)t1).getNodeKindMask();
                    int m2 = ((NodeTest)t2).getNodeKindMask();
                    if ((m1 & m2) == 0) {
                        return DISJOINT;
                    } else if (m1 == m2) {
                        nodeKindRelationship = SAME_TYPE;
                    } else if ((m1 & m2) == m1) {
                        nodeKindRelationship = SUBSUMED_BY;
                    } else if ((m1 & m2) == m2) {
                        nodeKindRelationship = SUBSUMES;
                    } else {
                        nodeKindRelationship = OVERLAPS;
                    }

                    // now find the relationship between the node names allowed. Note that although
                    // NamespaceTest and LocalNameTest are NodeTests, they do not occur in SequenceTypes,
                    // so we don't need to consider them.
                    int nodeNameRelationship;
                    HashSet<StructuredQName> n1 = ((NodeTest)t1).getRequiredNodeNames(); // null means all names allowed
                    HashSet<StructuredQName> n2 = ((NodeTest)t2).getRequiredNodeNames(); // null means all names allowed
                    if (n1 == null) {
                        if (n2 == null) {
                            nodeNameRelationship = SAME_TYPE;
                        } else {
                            nodeNameRelationship = SUBSUMES;
                        }
                    } else if (n2 == null) {
                        nodeNameRelationship = SUBSUMED_BY;
                    } else if (n1.containsAll(n2)) {
                        if (n1.size() == n2.size()) {
                            nodeNameRelationship = SAME_TYPE;
                        } else {
                            nodeNameRelationship = SUBSUMES;
                        }
                    } else if (n2.containsAll(n1)) {
                        nodeNameRelationship = SUBSUMED_BY;
                    } else if (SetUtils.containsSome(n1, n2)) {
                        nodeNameRelationship = OVERLAPS;
                    } else {
                        nodeNameRelationship = DISJOINT;
                    }


                    // now analyse the three different relationsships

                    if (nodeKindRelationship == SAME_TYPE &&
                            nodeNameRelationship == SAME_TYPE) {
                        return SAME_TYPE;
                    } else if ((nodeKindRelationship == SAME_TYPE || nodeKindRelationship == SUBSUMES) &&
                            (nodeNameRelationship == SAME_TYPE || nodeNameRelationship == SUBSUMES) ) {
                        return SUBSUMES;
                    } else if ((nodeKindRelationship == SAME_TYPE || nodeKindRelationship == SUBSUMED_BY) &&
                            (nodeNameRelationship == SAME_TYPE || nodeNameRelationship == SUBSUMED_BY) ) {
                        return SUBSUMED_BY;
                    } else if (nodeNameRelationship == DISJOINT) {
                        return DISJOINT;
                    } else {
                        return OVERLAPS;
                    }
                }
            }
        } else {
            // t1 is a FunctionItemType
            return DISJOINT;
        }

    }


    private static class ItemTypePair  {
        ItemType s;
        ItemType t;

        public ItemTypePair(ItemType s, ItemType t) {
            this.s = s;
            this.t = t;
        }

        /**
         * Returns a hash code value for the object.
         * @return a hash code value for this object.
         * @see Object#equals(Object)
         * @see java.util.Hashtable
         */
        public int hashCode() {
            return s.hashCode() ^ t.hashCode();
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         */

        public boolean equals(Object obj) {
            if (obj instanceof ItemTypePair) {
                final ItemTypePair pair = (ItemTypePair)obj;
                return s.equals(pair.s) && t.equals(pair.t);
            } else {
                return false;
            }
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
