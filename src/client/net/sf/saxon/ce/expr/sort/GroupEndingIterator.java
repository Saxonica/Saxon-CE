package client.net.sf.saxon.ce.expr.sort;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.ArrayList;

/**
 * A GroupEndingIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-ending-with="x". The groups are returned in
 * order of first appearance.
 */

public class GroupEndingIterator extends GroupMatchingIterator implements GroupIterator {

    public GroupEndingIterator(SequenceIterator population, Pattern endPattern,
                               XPathContext context)
            throws XPathException {
        this.pattern = endPattern;
        baseContext = context;
        runningContext = context.newMinorContext();
        this.population = runningContext.setCurrentIterator(population);
        // the first item in the population always starts a new group
        next = population.next();
    }

    protected void advance() throws XPathException {
        currentMembers = new ArrayList(20);
        currentMembers.add(current);

        next = current;
        while (next != null) {
            if (pattern.matches((NodeInfo)next, runningContext)) {
                next = population.next();
                if (next != null) {
                    break;
                }
            } else {
                next = population.next();
                if (next != null) {
                    currentMembers.add(next);
                }
            }
        }
    }

    public SequenceIterator getAnother() throws XPathException {
        return new GroupEndingIterator(population.getAnother(), pattern, baseContext);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.