package client.net.sf.saxon.ce.expr.sort;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public class SetUtils {
    /**
     * Form a new set that is the union of two IntSets.
     */

    public static Set union(Set one, Set two) {
        HashSet n = new HashSet(one.size() + two.size());
        Iterator it = one.iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        it = two.iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        return n;
    }

    /**
     * Form a new set that is the intersection of one set with another set.
     */

    public static Set intersect(Set one, Set two) {
        HashSet n = new HashSet(one.size());
        Iterator it = one.iterator();
        while (it.hasNext()) {
            Object v = it.next();
            if (two.contains(v)) {
                n.add(v);
            }
        }
        return n;
    }

    /**
     * Form a new set that is the difference of one set with another set.
     */

    public static Set except(Set one, Set two) {
        HashSet n = new HashSet(one.size());
        Iterator it = one.iterator();
        while (it.hasNext()) {
            Object v = it.next();
            if (!two.contains(v)) {
                n.add(v);
            }
        }
        return n;
    }

    /**
     * Test if one set has overlapping membership with another set
     */

    public static boolean containsSome(Set one, Set two) {
        Iterator it = two.iterator();
        while (it.hasNext()) {
            if (one.contains(it.next())) {
                return true;
            }
        }
        return false;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


