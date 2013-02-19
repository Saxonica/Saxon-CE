package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;

import java.util.ArrayList;
import java.util.List;

/**
 * A GroupAdjacentIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-adjacent="x". The groups are returned in
 * order of first appearance.
 * <p>
 * Each step of this iterator advances to the first item of the next group,
 * leaving the members of that group in a saved list.
 */

public class GroupAdjacentIterator implements GroupIterator {

    private SequenceIterator population;
    private Expression keyExpression;
    private StringCollator collator;
    private AtomicComparer comparer;
    private ComparisonKey currentComparisonKey;
    private XPathContext baseContext;
    private XPathContext runningContext;
    private AtomicValue currentKey = null;
    private List currentMembers;
    private AtomicValue nextKey = null;
    private Item next;
    private Item current = null;
    private int position = 0;

    public GroupAdjacentIterator(SequenceIterator population, Expression keyExpression,
                                 XPathContext baseContext, StringCollator collator)
    throws XPathException {
        this.population = population;
        this.keyExpression = keyExpression;
        this.baseContext = baseContext;
        this.runningContext = baseContext.newMinorContext();
        runningContext.setCurrentIterator(population);
        this.collator = collator;
        int type = keyExpression.getItemType(baseContext.getConfiguration().getTypeHierarchy()).getPrimitiveType();
        this.comparer = AtomicSortComparer.makeSortComparer(collator, type, baseContext);
        next = population.next();
        if (next != null) {
            nextKey = (AtomicValue)keyExpression.evaluateItem(runningContext);
        }
    }

    private void advance() throws XPathException {
        currentMembers = new ArrayList(20);
        currentMembers.add(current);
        while (true) {
            Item nextCandidate = population.next();
            if (nextCandidate == null) {
                break;
            }
            AtomicValue candidateKey =
                    (AtomicValue)keyExpression.evaluateItem(runningContext);
            try {
                if (currentComparisonKey.equals(comparer.getComparisonKey(candidateKey))) {
                    currentMembers.add(nextCandidate);
                } else {
                    next = nextCandidate;
                    nextKey = candidateKey;
                    return;
                }
            } catch (ClassCastException e) {
                XPathException err = new XPathException("Grouping key values are of non-comparable types (" +
                        Type.displayTypeName(currentKey) +
                        " and " +
                        Type.displayTypeName(candidateKey) + ')');
                err.setIsTypeError(true);
                err.setXPathContext(runningContext);
                throw err;
            }
        }
        next = null;
        nextKey = null;
    }

    public AtomicValue getCurrentGroupingKey() {
        return currentKey;
    }

    public SequenceIterator iterateCurrentGroup() {
        return new ListIterator(currentMembers);
    }

    public Item next() throws XPathException {
        if (next == null) {
            current = null;
            position = -1;
            return null;
        }
        current = next;
        currentKey = nextKey;
        currentComparisonKey = comparer.getComparisonKey(currentKey);
        position++;
        advance();
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() throws XPathException {
        return new GroupAdjacentIterator(population.getAnother(), keyExpression, baseContext, collator);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.