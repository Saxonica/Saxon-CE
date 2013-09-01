package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.event.Stripper;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;

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

    private static final String[] specials = {
            "analyze-string", "apply-imports", "apply-templates",
            "attribute-set", "call-template", "character-map", "choose",
            "stylesheet", "transform" };

    public Stripper getAnother() {
        return new StylesheetStripper();
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param elementName identifies the element being tested
    */

    public byte isSpacePreserving(StructuredQName elementName) {
        if (elementName.getNamespaceURI().equals(NamespaceConstant.XSLT)) {
            String local = elementName.getLocalName();
            if (local.equals("text")) {
                return ALWAYS_PRESERVE;
            }

            if (Arrays.binarySearch(specials, local) >= 0) {
                return ALWAYS_STRIP;
            }
        }
        return STRIP_DEFAULT;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
