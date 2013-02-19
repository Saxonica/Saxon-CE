package client.net.sf.saxon.ce.expr;

/**
* This class contains constants identifying dependencies that an XPath expression
* might have on its context.
*/

public abstract class StaticProperty {

    /**
    * Bit setting: Expression depends on current() item
    */

    public static final int DEPENDS_ON_CURRENT_ITEM = 1;

    /**
    * Bit setting: Expression depends on context item
    */

    public static final int DEPENDS_ON_CONTEXT_ITEM = 1<<1;

    /**
    * Bit setting: Expression depends on position()
    */

    public static final int DEPENDS_ON_POSITION = 1<<2;

    /**
    * Bit setting: Expression depends on last()
    */

    public static final int DEPENDS_ON_LAST = 1<<3;

    /**
    * Bit setting: Expression depends on the document containing the context node
    */

    public static final int DEPENDS_ON_CONTEXT_DOCUMENT = 1<<4;

    /**
    * Bit setting: Expression depends on current-group() and/or current-grouping-key()
    */

    public static final int DEPENDS_ON_CURRENT_GROUP = 1<<5;

    /**
    * Bit setting: Expression depends on regex-group()
    */

    public static final int DEPENDS_ON_REGEX_GROUP = 1<<6;


    /**
     * Bit setting: Expression depends on local variables
     */

    public static final int DEPENDS_ON_LOCAL_VARIABLES = 1<<7;

    /**
     * Bit setting: Expression depends on user-defined functions
     */

    public static final int DEPENDS_ON_USER_FUNCTIONS = 1<<8;

    /**
     * Bit setting: Expression can't be evaluated at compile time for reasons other than the above
     */

    public static final int DEPENDS_ON_RUNTIME_ENVIRONMENT = 1<<10;

    /**
    * Combination of bits representing dependencies on the XSLT context
    */

    public static final int DEPENDS_ON_XSLT_CONTEXT =
            DEPENDS_ON_CURRENT_ITEM |
            DEPENDS_ON_CURRENT_GROUP |
            DEPENDS_ON_REGEX_GROUP;

    /**
    * Combination of bits representing dependencies on the focus
    */

    public static final int DEPENDS_ON_FOCUS =
            DEPENDS_ON_CONTEXT_ITEM |
            DEPENDS_ON_POSITION |
            DEPENDS_ON_LAST |
            DEPENDS_ON_CONTEXT_DOCUMENT;

    /**
    * Combination of bits representing dependencies on the focus, but excluding dependencies
    * on the current document
    */

    public static final int DEPENDS_ON_NON_DOCUMENT_FOCUS =
            DEPENDS_ON_CONTEXT_ITEM |
            DEPENDS_ON_POSITION |
            DEPENDS_ON_LAST;

    /*
    * Bit set if an empty sequence is allowed
    */

    public static final int ALLOWS_ZERO = 1<<13;

    /**
    * Bit set if a single value is allowed
    */

    public static final int ALLOWS_ONE = 1<<14;

    /**
    * Bit set if multiple values are allowed
    */

    public static final int ALLOWS_MANY = 1<<15;

    /**
     * Mask for all cardinality bits
     */

    public static final int CARDINALITY_MASK =
            ALLOWS_ZERO | ALLOWS_ONE | ALLOWS_MANY;

    /**
    * Occurence indicator for "one or more" (+)
    */

    public static final int ALLOWS_ONE_OR_MORE =
            ALLOWS_ONE | ALLOWS_MANY;

    /**
    * Occurence indicator for "zero or more" (*)
    */

    public static final int ALLOWS_ZERO_OR_MORE =
            ALLOWS_ZERO | ALLOWS_ONE | ALLOWS_MANY;

    /**
    * Occurence indicator for "zero or one" (?)
    */

    public static final int ALLOWS_ZERO_OR_ONE =
            ALLOWS_ZERO | ALLOWS_ONE;

    /**
    * Occurence indicator for "exactly one" (default occurrence indicator)
    */

    public static final int EXACTLY_ONE = ALLOWS_ONE;

    /**
    * Occurence indicator when an empty sequence is required
    */

    public static final int EMPTY = ALLOWS_ZERO;

    /**
     * Reduce the cardinality value to an integer in the range 0-7
     * @param cardinality the result of calling getCardinality() on an expression
     * @return the cardinality code
     */

    public static int getCardinalityCode(int cardinality) {
        return (cardinality & CARDINALITY_MASK) >> 13;
    }

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression whose item type is node, when the nodes in the result are
     * guaranteed all to be in the same document as the context node. For
     * expressions that return values other than nodes, the setting is undefined.
     */

    public static final int CONTEXT_DOCUMENT_NODESET = 1<<16;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression whose item type is node, when the nodes in the result are
     * in document order.
     */

    public static final int ORDERED_NODESET = 1<<17;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers items in the reverse of the correct order, when unordered
     * retrieval is requested.
     */

    public static final int REVERSE_DOCUMENT_ORDER = 1<<18;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers a set of nodes with the guarantee that no node in the
     * set will be an ancestor of any other. This property is useful in deciding whether the
     * results of a path expression are pre-sorted. The property is only used in the case where
     * the NATURALLY_SORTED property is true, so there is no point in setting it in other cases.
     */

    public static final int PEER_NODESET = 1<<19;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers a set of nodes with the guarantee that every node in the
     * result will be a descendant or self, or attribute or namespace, of the context node
     */

    public static final int SUBTREE_NODESET = 1<<20;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers a set of nodes with the guarantee that every node in the
     * result will be an attribute or namespace of the context node
     */

    public static final int ATTRIBUTE_NS_NODESET = 1<<21;

    /**
     * Expression property: this bit is set in the case of an expression that will
     * never return newly created nodes, nor a value that depends on the identity
     * of newly created nodes (for example generate-id(new-node())). Expressions
     * that do create new nodes cannot be moved out of loops as this could cause
     * too few nodes to be created: for example if f() creates a new node, then
     * count(for $i in 1 to 5 return f()) must be 5.
     */

    public static final int NON_CREATIVE = 1<<22;

    /**
     * Expression property: this bit is set in the case of an expression that delivers
     * a set of nodes that are all in the same document (not necessarily the same
     * document as the context node).
     */

    public static final int SINGLE_DOCUMENT_NODESET = 1<<23;

    /**
     * Expression property: this bit indicates that an expression has (or might have)
     * side-effects. This property is applied to calls on extension functions and to
     * certain instructions such as xsl:result-document and xsl:message.
     */

    public static final int HAS_SIDE_EFFECTS = 1<<24;

    /**
     * Expression property: this bit indicates that although the static type of the expression
     * permits untyped values, it is known that the value will not be untyped.
     */

    public static final int NOT_UNTYPED = 1<<25;

    /**
     * Mask to select all the dependency bits
     */

    public static final int DEPENDENCY_MASK =
            DEPENDS_ON_CONTEXT_DOCUMENT |
            DEPENDS_ON_CONTEXT_ITEM |
            DEPENDS_ON_CURRENT_GROUP |
            DEPENDS_ON_REGEX_GROUP |
            DEPENDS_ON_CURRENT_ITEM |
            DEPENDS_ON_FOCUS |
            DEPENDS_ON_LOCAL_VARIABLES |
            DEPENDS_ON_USER_FUNCTIONS |
            DEPENDS_ON_RUNTIME_ENVIRONMENT |
            HAS_SIDE_EFFECTS;

    /**
     * Mask for "special properties": that is, all properties other than cardinality
     * and dependencies
     */

    public static final int SPECIAL_PROPERTY_MASK =
            CONTEXT_DOCUMENT_NODESET |
            ORDERED_NODESET |
            REVERSE_DOCUMENT_ORDER |
            PEER_NODESET |
            SUBTREE_NODESET |
            ATTRIBUTE_NS_NODESET |
            SINGLE_DOCUMENT_NODESET |
            NON_CREATIVE |
            HAS_SIDE_EFFECTS |
            NOT_UNTYPED;

    // This class is not instantiated
    private StaticProperty() {}
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
