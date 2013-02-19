package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.event.Stripper;
import client.net.sf.saxon.ce.om.StandardNames;

import java.util.Arrays;

/**
  * The StylesheetStripper refines the Stripper class to do stripping of
  * whitespace nodes on a stylesheet. This is handled specially (a) because
  * it is done at compile time, so there is no Controller available, and (b)
  * because the rules are very simple
  * @author Michael H. Kay
  */

public class StylesheetStripper extends Stripper {


    //    Any child of one of the following elements is removed from the tree,
    //    regardless of any xml:space attributes. Note that this array must be in numeric
    //    order for binary chop to work correctly.

    private static final int[] specials = {
            StandardNames.XSL_ANALYZE_STRING,
            StandardNames.XSL_APPLY_IMPORTS,
            StandardNames.XSL_APPLY_TEMPLATES,
            StandardNames.XSL_ATTRIBUTE_SET,
            StandardNames.XSL_CALL_TEMPLATE,
            StandardNames.XSL_CHARACTER_MAP,
            StandardNames.XSL_CHOOSE,
            StandardNames.XSL_EVALUATE,
            StandardNames.XSL_MERGE,
            StandardNames.XSL_MERGE_SOURCE,
            StandardNames.XSL_NEXT_ITERATION,
            StandardNames.XSL_NEXT_MATCH,
            StandardNames.XSL_STYLESHEET,
            StandardNames.XSL_TRANSFORM
    };

    public Stripper getAnother() {
        return new StylesheetStripper();
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param fingerprint identifies the element being tested
    */

    public byte isSpacePreserving(int fingerprint) {
        if (fingerprint == StandardNames.XSL_TEXT) {
            return ALWAYS_PRESERVE;
        }

        if (Arrays.binarySearch(specials, fingerprint) >= 0) {
            return ALWAYS_STRIP;
        }

        return STRIP_DEFAULT;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
