package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * A Literal is an expression whose value is constant: it is a class that implements the {@link Expression}
 * interface as a wrapper around a {@link Value}. This may derive from an actual literal in an XPath expression
 * or query, or it may be the result of evaluating a constant subexpression such as true() or xs:date('2007-01-16')
*/

public class Literal extends Expression {

    private Value value;

    /**
     * Create a literal as a wrapper around a Value
     * @param value the value of this literal
     */

    public Literal(Value value) {
        this.value = value;
    }

    /**
     * Create a literal as a wrapper around a Value (factory method)
     * @param value the value of this literal
     * @return the Literal
     */

    public static Literal makeLiteral(ValueRepresentation value) {
        if (value instanceof StringValue) {
            return new StringLiteral((StringValue)value);
        } else {
            return new Literal(Value.asValue(value));
        }
    }

    /**
     * Make an empty-sequence literal
     * @return a literal whose value is the empty sequence
     */

    public static Literal makeEmptySequence() {
        return new Literal(EmptySequence.getInstance());
    }

    /**
     * Get the value represented by this Literal
     * @return the constant value
     */

    public Value getValue() {
        return value;
    }

    /**
    * TypeCheck an expression
    * @return for a Value, this always returns the value unchanged
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) {
        return this;
    }

    /**
    * Optimize an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) {
        return this;
    }

    /**
     * Determine the data type of the items in the expression, if possible
     * @return for the default implementation: AnyItemType (not known)
     */

    public ItemType getItemType() {
        return value.getItemType();
    }

    /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        if (value instanceof EmptySequence) {
            return StaticProperty.EMPTY;
        } else if (value instanceof AtomicValue) {
            return StaticProperty.EXACTLY_ONE;
        }
        try {
            SequenceIterator iter = value.iterate();
            Item next = iter.next();
            if (next == null) {
                return StaticProperty.EMPTY;
            } else {
                if (iter.next() != null) {
                    return StaticProperty.ALLOWS_ONE_OR_MORE;
                } else {
                    return StaticProperty.EXACTLY_ONE;
                }
            }
        } catch (XPathException err) {
            // can't actually happen
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
     * Compute the static properties of this expression (other than its type). For a
     * Value, the only special property is {@link StaticProperty#NON_CREATIVE}.
     * @return the value {@link StaticProperty#NON_CREATIVE}
     */


    public int computeSpecialProperties() {
        if (getValue() instanceof EmptySequence) {
            // An empty sequence has all special properties except "has side effects".
            return StaticProperty.SPECIAL_PROPERTY_MASK &~ StaticProperty.HAS_SIDE_EFFECTS;
        }
        return StaticProperty.NON_CREATIVE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    private Expression copy() {
        return new Literal(value);
    }


    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
     * @return for a Value, this always returns zero.
    */

    public final int getDependencies() {
        return 0;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return value.iterate();
    }

    /**
     * Evaluate as a singleton item (or empty sequence). Note: this implementation returns
     * the first item in the sequence. The method should not be used unless appropriate type-checking
     * has been done to ensure that the value will be a singleton.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        if (value instanceof AtomicValue) {
            return (AtomicValue)value;
        }
        return value.iterate().next();
    }


    /**
      * Process the value as an instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = value.iterate();
        SequenceReceiver out = context.getReceiver();
        while (true) {
            Item it = iter.next();
            if (it==null) break;
            out.append(it, NodeInfo.ALL_NAMESPACES);
        }
    }

    /*
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @exception client.net.sf.saxon.ce.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @exception ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public CharSequence evaluateAsString(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue) evaluateItem(context);
        if (value == null) return "";
        return value.getStringValue();
    }

    /**
     * Determine whether two literals are equal, when considered as expressions.
     * @param obj the other expression
     * @return true if the two literals are equal. The test here requires (a) identity in the
     * sense defined by XML Schema (same value in the same value space), and (b) identical type
     * annotations. For example the literal xs:int(3) is not equal (as an expression) to xs:short(3), 
     * because the two expressions are not interchangeable.
     */

    public boolean equals(Object obj) {
        if (!(obj instanceof Literal)) {
            return false;
        }
        Value v0 = value;
        Value v1 = ((Literal)obj).value;
        try {
            SequenceIterator i0 = v0.iterate();
            SequenceIterator i1 = v1.iterate();
            while (true) {
                Item m0 = i0.next();
                Item m1 = i1.next();
                if (m0==null && m1==null) {
                    return true;
                }
                if (m0==null || m1==null) {
                    return false;
                }
                boolean n0 = (m0 instanceof NodeInfo);
                boolean n1 = (m1 instanceof NodeInfo);
                if (n0 != n1) {
                    return false;
                }
                if (n0 && n1 && !((NodeInfo)m0).isSameNodeInfo((NodeInfo)m1)) {
                    return false;
                }
                // added test for atomicValue types - using getStringValue ensures that a collator
                // will be used once collators are supported
                // assertion: m0 != null && m1 != null
                if(m0 instanceof StringValue && m1 instanceof StringValue){
               	    if (!(((StringValue)m0).getStringValue().equals(((StringValue)m1).getStringValue()))) {
               	    	return false;
               	    }
                } else if(m0 instanceof AtomicValue && m1 instanceof AtomicValue){
					if (!n0 && !n1 && (!((AtomicValue)m0).equals((AtomicValue)m1)) ||
							((AtomicValue)m0).getItemType() != ((AtomicValue)m1).getItemType()) {
						return false;
					}
				}
            }
        } catch (XPathException err) {
            return false;
        }
    }

    /**
     * Return a hash code to support the equals() function
     */

    public int hashCode() {
        return value.hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return value.toString();
    }


    /**
     * Test whether the literal wraps an atomic value. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to contain
     * a single atomic value).
     * @param exp an expression
     * @return if the expression is a literal and the literal wraps an AtomicValue
     */

    public static boolean isAtomic(Expression exp) {
        return exp instanceof Literal && ((Literal)exp).getValue() instanceof AtomicValue;
    }

    /**
     * Test whether the literal explicitly wraps an empty sequence. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to be empty).
     * @param exp an expression
     * @return if the expression is a literal and the literal wraps an AtomicValue
     */

    public static boolean isEmptySequence(Expression exp) {
        return exp instanceof Literal && ((Literal)exp).getValue() instanceof EmptySequence;
    }

    /**
     * Test if a literal represents the boolean value true
     * @param exp an expression
     * @param value true or false
     * @return if the expression is a literal and the literal represents the boolean value given in the
     * second argument
     */

    public static boolean isConstantBoolean(Expression exp, boolean value) {
        if (exp instanceof Literal) {
            Value b = ((Literal)exp).getValue();
            return (b instanceof BooleanValue && ((BooleanValue)b).getBooleanValue() == value);
        }
        return false;
    }

    /**
     * Test if a literal represents the integer value 1
     * @param exp an expression
     * @return if the expression is a literal and the literal represents the integer value 1
     */

    public static boolean isConstantOne(Expression exp) {
        try {
            if (exp instanceof Literal) {
                Value v = ((Literal)exp).getValue();
                return (v instanceof IntegerValue && ((IntegerValue)v).intValue() == 1);
            }
            return false;
        } catch (XPathException e) {
            // overflow
            return false;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
