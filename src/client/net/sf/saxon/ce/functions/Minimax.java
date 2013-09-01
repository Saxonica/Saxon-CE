package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.sort.AtomicComparer;
import client.net.sf.saxon.ce.expr.sort.DescendingComparer;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;

/**
* This class implements the min() and max() functions
*/

public class Minimax extends CollatingFunction {

    public Minimax(int operation) {
        this.operation = operation;
    }

    public Minimax newInstance() {
        return new Minimax(operation);
    }
    public static final int MIN = 2;
    public static final int MAX = 3;

    private BuiltInAtomicType argumentType = BuiltInAtomicType.ANY_ATOMIC;
    private boolean ignoreNaN = false;

    /**
     * Indicate whether NaN values should be ignored. For the external min() and max() function, a
     * NaN value in the input causes the result to be NaN. Internally, however, min() and max() are also
     * used in such a way that NaN values should be ignored.
     * @param ignore true if NaN values are to be ignored when computing the min or max.
     */

    public void setIgnoreNaN(boolean ignore) {
        ignoreNaN = ignore;
    }

    /**
     * Test whether NaN values are to be ignored
     * @return true if NaN values are to be ignored. This is the case for internally-generated min() and max()
     * functions used to support general comparisons
     */

    public boolean isIgnoreNaN() {
        return ignoreNaN;
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
    }

    /**
     * Determine the cardinality of the function.
     */

    public int computeCardinality() {
        int c = super.computeCardinality();
        if (!Cardinality.allowsZero(argument[0].getCardinality())) {
            c = StaticProperty.EXACTLY_ONE;
        }
        return c;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        argumentType = (BuiltInAtomicType)argument[0].getItemType(th).getAtomizedItemType().getPrimitiveItemType();
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        if (getNumberOfArguments() == 1) {
            // test for a singleton: this often happens after (A<B) is rewritten as (min(A) lt max(B))
            int card = argument[0].getCardinality();
            if (!Cardinality.allowsMany(card) && th.isSubType(argument[0].getItemType(th), BuiltInAtomicType.NUMERIC)) {
                return argument[0];
            }
        }
        return this;
    }

    /**
     * Determine the item type of the value returned by the function
     *
     * @param th the type hierarchy cache
     * @return the statically inferred type of the expression
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType t = Atomizer.getAtomizedItemType(argument[0], false, th);
        if (t == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return BuiltInAtomicType.DOUBLE;
        } else {
            return t;
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicComparer comparer = getAtomicComparer(context);
        SequenceIterator iter = argument[0].iterate(context);
        try {
            return minimax(iter, operation, comparer, ignoreNaN, context);
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
    }

    public AtomicComparer getAtomicComparer(XPathContext context) throws XPathException {
        StringCollator collator = getCollator(1, context);
        BuiltInAtomicType type = argumentType;
        if (type == BuiltInAtomicType.UNTYPED_ATOMIC) {
            type = BuiltInAtomicType.DOUBLE;
        }
        AtomicComparer comparer =
                GenericAtomicComparer.makeAtomicComparer(type, type, collator, context);
        return comparer;
    }

    /**
     * Static method to evaluate the minimum or maximum of a sequence
     * @param iter Iterator over the input sequence
     * @param operation either {@link #MIN} or {@link #MAX}
     * @param atomicComparer an AtomicComparer used to compare values
     * @param ignoreNaN true if NaN values are to be ignored
     * @param context dynamic evaluation context
     * @return the min or max value in the sequence, according to the rules of the fn:min() or fn:max() functions
     * @throws XPathException typically if non-comparable values are found in the sequence
     */
    public static AtomicValue minimax(SequenceIterator iter, int operation,
                                      AtomicComparer atomicComparer, boolean ignoreNaN, XPathContext context)
            throws XPathException {

        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        boolean foundDouble = false;
        boolean foundFloat = false;
        boolean foundNaN = false;

        // For the max function, reverse the collator
        if (operation == MAX) {
            atomicComparer = new DescendingComparer(atomicComparer);
        }

        // Process the sequence, retaining the min (or max) so far. This will be an actual value found
        // in the sequence. At the same time, remember if a double and/or float has been encountered
        // anywhere in the sequence, and if so, convert the min/max to double/float at the end. This is
        // done to avoid problems if a decimal is converted first to a float and then to a double.

        // Get the first value in the sequence, ignoring any NaN values if we are ignoring NaN values
        AtomicValue min;
        AtomicValue prim;

        while (true) {
            min = (AtomicValue)iter.next();
            if (min == null) {
                return null;
            }
            prim = min;
            if (min instanceof UntypedAtomicValue) {
                try {
                    min = new DoubleValue(StringToDouble.stringToNumber(min.getStringValueCS()));
                    prim = min;
                    foundDouble = true;
                } catch (NumberFormatException e) {
                    XPathException de = new XPathException("Failure converting " + Err.wrap(min.getStringValueCS()) + " to a number");
                    de.setErrorCode("FORG0001");
                    de.setXPathContext(context);
                    throw de;
                }
            } else {
                if (prim instanceof DoubleValue) {
                    foundDouble = true;
                } else if (prim instanceof FloatValue) {
                    foundFloat = true;
                }
            }
            if (prim.isNaN()) {
                // if there's a NaN in the sequence, return NaN, unless ignoreNaN is set
                if (ignoreNaN) {
                    //continue;   // ignore the NaN and treat the next item as the first real one
                } else if (prim instanceof DoubleValue) {
                    return min; // return double NaN
                } else {
                    // we can't ignore a float NaN, because we might need to promote it to a double NaN
                    foundNaN = true;
                    min = FloatValue.NaN;
                    break;
                }
            } else {
                if (!prim.getPrimitiveType().isOrdered()) {
                    XPathException de = new XPathException("Type " + prim.getPrimitiveType() + " is not an ordered type");
                    de.setErrorCode("FORG0006");
                    de.setIsTypeError(true);
                    de.setXPathContext(context);
                    throw de;
                }
                break;          // process the rest of the sequence
            }
        }

        BuiltInAtomicType lowestCommonSuperType = min.getPrimitiveType();

        while (true) {
            AtomicValue test = (AtomicValue)iter.next();
            if (test==null) {
                break;
            }
            AtomicValue test2 = test;
            prim = test2;
            if (test instanceof UntypedAtomicValue) {
                try {
                    test2 = new DoubleValue(StringToDouble.stringToNumber(test.getStringValueCS()));
                    if (foundNaN) {
                        return DoubleValue.NaN;
                    }
                    prim = test2;
                    foundDouble = true;
                } catch (NumberFormatException e) {
                    XPathException de = new XPathException("Failure converting " + Err.wrap(test.getStringValueCS()) + " to a number");
                    de.setErrorCode("FORG0001");
                    de.setXPathContext(context);
                    throw de;
                }
            } else {
                if (prim instanceof DoubleValue) {
                    if (foundNaN) {
                        return DoubleValue.NaN;
                    }
                    foundDouble = true;
                } else if (prim instanceof FloatValue) {
                    foundFloat = true;
                }
            }
            lowestCommonSuperType = (BuiltInAtomicType)Type.getCommonSuperType(
                    lowestCommonSuperType, prim.getPrimitiveType(), th);
            if (prim.isNaN()) {
                // if there's a double NaN in the sequence, return NaN, unless ignoreNaN is set
                if (ignoreNaN) {
                    //continue;
                } else if (foundDouble) {
                    return DoubleValue.NaN;
                } else {
                    // can't return float NaN until we know whether to promote it
                    foundNaN = true;
                }
            } else {
                try {
                    if (atomicComparer.compareAtomicValues(prim, min) < 0) {
                        min = test2;
                    }
                } catch (ClassCastException err) {
                    XPathException de = new XPathException("Cannot compare " + min.getItemType(th) + " with " + test2.getItemType(th));
                    de.setErrorCode("FORG0006");
                    de.setIsTypeError(true);
                    de.setXPathContext(context);
                    throw de;
                }
            }
        }
        if (foundNaN) {
            return FloatValue.NaN;
        }
        if (foundDouble) {
            if (!(min instanceof DoubleValue)) {
                min = min.convert(BuiltInAtomicType.DOUBLE, true).asAtomic();
            }
        } else if (foundFloat) {
            if (!(min instanceof FloatValue)) {
                min = min.convert(BuiltInAtomicType.FLOAT, true).asAtomic();
            }    
        }
        return min.convert(lowestCommonSuperType, false).asAtomic();
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
