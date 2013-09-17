package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.Cardinality;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Expression equivalent to the imaginary syntax
 * expr sortby (sort-key)+
 */

public class SortExpression extends Expression
        implements SortKeyEvaluator {

    private Expression select = null;
    private SortKeyDefinition[] sortKeyDefinitions = null;
    private AtomicComparer[] comparators = null;

    /**
     * Create a sort expression
     * @param select the expression whose result is to be sorted
     * @param sortKeys the set of sort key definitions to be used, in major to minor order
     */

    public SortExpression(Expression select, SortKeyDefinition[] sortKeys) {
        this.select = select;
        sortKeyDefinitions = sortKeys;
        Iterator children = iterateSubExpressions();
        while (children.hasNext()) {
            Expression exp = (Expression) children.next();
            adoptChildExpression(exp);
        }
    }

    /**
     * Get the expression defining the sequence being sorted
     * @return the expression whose result is to be sorted
     */

    public Expression getBaseExpression() {
        return select;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator<Expression> iterateSubExpressions() {
        return iterateSubExpressions(true);
    }

    private Iterator<Expression> iterateSubExpressions(boolean includeSortKey) {
        List<Expression> list = new ArrayList<Expression>(8);
        list.add(select);
        for (SortKeyDefinition skd : sortKeyDefinitions) {
            if (includeSortKey) {
                list.add(skd.getSortKey());
            }
            for (int i=0; i<SortKeyDefinition.N; i++) {
                Expression e = skd.getSortProperty(i);
                if (e != null) {
                    list.add(e);
                }
            }
        }
        return list.iterator();
    }


    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return isSortKey(child);
    }

    /**
     * Simplify an expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        return this;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression select2 = visitor.typeCheck(select, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        ItemType sortedItemType = select.getItemType();

        boolean allKeysFixed = true;
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            if (!(sortKeyDefinition.isFixed())) {
                allKeysFixed = false;
                break;
            }
        }

        if (allKeysFixed) {
            comparators = new AtomicComparer[sortKeyDefinitions.length];
        }

        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            Expression sortKey = sortKeyDefinitions[i].getSortKey();
            sortKey = visitor.typeCheck(sortKey, sortedItemType);
            if (visitor.getStaticContext().isInBackwardsCompatibleMode()) {
                sortKey = new FirstItemExpression(sortKey);
            } else {
                RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0);
                role.setErrorCode("XTTE1020");
                sortKey = CardinalityChecker.makeCardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
            }
            sortKeyDefinitions[i].setSortKey(sortKey);
            sortKeyDefinitions[i].typeCheck(visitor, contextItemType);
            if (sortKeyDefinitions[i].isFixed()) {
                AtomicComparer comp = sortKeyDefinitions[i].makeComparator(new EarlyEvaluationContext(visitor.getConfiguration()));
                sortKeyDefinitions[i].setFinalComparator(comp);
                if (allKeysFixed) {
                    comparators[i] = comp;
                }
            }
//            if (!ExpressionTool.dependsOnFocus(sortKey)) {
//                visitor.getStaticContext().issueWarning(
//                        "Sort key will have no effect because its value does not depend on the context item",
//                        sortKey.getSourceLocator());
//            }

        }
        return this;
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
        Expression select2 = visitor.optimize(select, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        // optimize the sort keys
        ItemType sortedItemType = select.getItemType();
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            Expression sortKey = sortKeyDefinition.getSortKey();
            sortKey = visitor.optimize(sortKey, sortedItemType);
            sortKeyDefinition.setSortKey(sortKey);
        }
        if (Cardinality.allowsMany(select.getCardinality())) {
            return this;
        } else {
            return select;
        }
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            select = doPromotion(select, offer);
            for (SortKeyDefinition skd : sortKeyDefinitions) {
                final Expression sk2 = skd.getSortKey().promote(offer, parent);
                skd.setSortKey(sk2);
                for (int i=0; i<SortKeyDefinition.N; i++) {
                    Expression e = skd.getSortProperty(i);
                    if (e != null) {
                        skd.setSortProperty(i, e.promote(offer, parent));
                    }
                }
            }
            return this;
        }
    }

    /**
     * Test whether a given expression is one of the sort keys
     * @param child the given expression
     * @return true if the given expression is one of the sort keys
     */

    public boolean isSortKey(Expression child) {
        for (SortKeyDefinition skd : sortKeyDefinitions) {
            Expression exp = skd.getSortKey();
            if (exp == child) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the static cardinality
     */

    public int computeCardinality() {
        return select.getCardinality();
    }

    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    public ItemType getItemType() {
        return select.getItemType();
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-significant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int props = 0;
        if ((select.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        if ((select.getSpecialProperties() & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        if ((select.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
            props |= StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * Enumerate the results of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        SequenceIterator iter = select.iterate(context);
        if (iter instanceof EmptyIterator) {
            return iter;
        }
        XPathContext xpc = context.newMinorContext();

        AtomicComparer[] comps = comparators;
        if (comparators == null) {
            comps = new AtomicComparer[sortKeyDefinitions.length];
            for (int s = 0; s < sortKeyDefinitions.length; s++) {
                AtomicComparer comp = sortKeyDefinitions[s].getFinalComparator();
                if (comp == null) {
                    comp = sortKeyDefinitions[s].makeComparator(xpc);
                }
                comps[s] = comp;
            }
        }
        return new SortedIterator(xpc, iter, this, comps);
    }

    /**
     * Callback for evaluating the sort keys
     */

    public Item evaluateSortKey(int n, XPathContext c) throws XPathException {
        return sortKeyDefinitions[n].getSortKey().evaluateItem(c);
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.