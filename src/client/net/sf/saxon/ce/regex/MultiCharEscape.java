package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.expr.z.*;
import client.net.sf.saxon.ce.om.NameChecker;

import java.util.ArrayList;
import java.util.List;


/**
 * Class to handle the character category escapes
 */
public class MultiCharEscape {

    public final static IntPredicate ESCAPE_s =
            new IntSetPredicate(IntHashSet.fromArray(new int[]{9, 10, 13, 32}));

    public final static IntPredicate ESCAPE_S = new IntComplementPredicate(ESCAPE_s);

    public final static IntPredicate ESCAPE_i = new IntPredicate() {
        public boolean matches(int value) {
            return NameChecker.isNCNameStartChar(value) || value==':';
        }
    };

    public final static IntPredicate ESCAPE_I = new IntPredicate() {
        public boolean matches(int value) {
            return !(NameChecker.isNCNameStartChar(value) || value==':');
        }
    };

    public final static IntPredicate ESCAPE_c = new IntPredicate() {
        public boolean matches(int value) {
            return NameChecker.isNCNameChar(value) || value==':';
        }
    };

    public final static IntPredicate ESCAPE_C = new IntPredicate() {
        public boolean matches(int value) {
            return !(NameChecker.isNCNameChar(value) || value==':');
        }
    };

    public final static IntPredicate ESCAPE_d = new IntSetPredicate(getSubCategoryCharClass("Nd"));

    public final static IntPredicate ESCAPE_D = new IntComplementPredicate(ESCAPE_d);

    private static IntPredicate CATEGORY_P = getCategoryCharClass('P');
    private static IntPredicate CATEGORY_Z = getCategoryCharClass('Z');
    private static IntPredicate CATEGORY_C = getCategoryCharClass('C');

    public final static IntPredicate ESCAPE_w = new IntPredicate () {
        public boolean matches(int value) {
            return !(CATEGORY_P.matches(value) || CATEGORY_Z.matches(value) || CATEGORY_C.matches(value));
        }
    };

    public final static IntPredicate ESCAPE_W = new IntComplementPredicate(ESCAPE_w);

    public static synchronized IntPredicate getCategoryCharClass(char category) {
        final List<IntSet> ranges = new ArrayList<IntSet>(10);
        for (String sub : Categories.CATEGORIES.keySet()) {
            if (sub.charAt(0) == category) {
                ranges.add(getSubCategoryCharClass(sub));
            }
        }
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Unknown category " + category);
        }
        return new IntPredicate() {
            public boolean matches(int value) {
                for (IntSet cat : ranges) {
                    if (cat.contains(value)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static IntRangeSet getSubCategoryCharClass(String category) {
        int[] codes = Categories.CATEGORIES.get(category);
        if (codes == null) {
            throw new IllegalArgumentException("Unknown category " + category);
        }
        int[] startPoints = new int[codes.length/2];
        int[] endPoints = new int[codes.length/2];
        for (int i=0; i<codes.length; i+=2) {
            startPoints[i/2] = codes[i];
            endPoints[i/2] = codes[i+1];
        }
        return new IntRangeSet(startPoints, endPoints);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
