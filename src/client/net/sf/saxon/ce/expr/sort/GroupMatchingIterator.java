package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
import client.net.sf.saxon.ce.value.AtomicValue;

import java.util.List;

/**
 * A GroupMatchingIterator contains code shared between GroupStartingIterator and GroupEndingIterator
 */

public abstract class GroupMatchingIterator implements  GroupIterator {

    protected SequenceIterator population;
    protected Pattern pattern;
    protected XPathContext baseContext;
    protected XPathContext runningContext;
    protected List currentMembers;
    protected Item next;
    protected Item current = null;
    protected int position = 0;


    protected abstract void advance() throws XPathException;

    public AtomicValue getCurrentGroupingKey() {
        return null;
    }

    public SequenceIterator iterateCurrentGroup() {
        return new ListIterator(currentMembers);
    }

    public Item next() throws XPathException {
        if (next != null) {
            current = next;
            position++;
            advance();
            return current;
        } else {
            current = null;
            position = -1;
            return null;
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public int getProperties() {
        return 0;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.