package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.functions.DistinctValues;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.NoDynamicContextException;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
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
    //private AtomicComparer comparer;
    private Object currentComparisonKey;
    private XPathContext baseContext;
    private XPathContext runningContext;
    private AtomicValue currentKey = null;
    private List<Item> currentMembers;
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
                Object compKey = comparisonKey(candidateKey);
                if (currentComparisonKey.equals(compKey)) {
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
                throw err;
            }
        }
        next = null;
        nextKey = null;
    }

    private Object comparisonKey(AtomicValue candidateKey) throws NoDynamicContextException {
        if (candidateKey.isNaN()) {
            return DistinctValues.class;
        } else {
            return candidateKey.getXPathComparable(false, collator, baseContext.getImplicitTimezone());
        }
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
        currentComparisonKey = comparisonKey(currentKey);
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


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.