package client.net.sf.saxon.ce.regex;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;

import java.util.HashMap;

/**
 * This class holds data about the case-variants of Unicode characters. The data is automatically
 * generated from the Unicode database.
 */
public class CaseVariants {

    // Use one hashmap for characters with a single case variant, another for characters with multiple
    // case variants, to reduce the number of objects that need to be allocated

    private static HashMap<Integer, Integer> monoVariants = null;
    private static HashMap<Integer, int[]> polyVariants = null;


    static void build() {

        monoVariants = new HashMap<Integer, Integer>(2500);
        polyVariants = new HashMap<Integer, int[]>(100);

        Configuration config = new Configuration();
        DocumentInfo doc;
        try {
            doc = config.buildDocument("casevariants.xml");
        } catch (XPathException e) {
            throw new RuntimeException("Failed to build casevariants.xml", e);
        }

        UnfailingIterator iter = doc.iterateAxis(Axis.DESCENDANT, new NameTest(Type.ELEMENT, "", "c"));
        while (true) {
            NodeInfo item = (NodeInfo)iter.next();
            if (item == null) {
                break;
            }
            String code = Navigator.getAttributeValue(item, "", "n");
            int icode = Integer.parseInt(code, 16);
            String variants = Navigator.getAttributeValue(item, "", "v");
            String[] vhex = variants.split(",");
            int[] vint = new int[vhex.length];
            for (int i=0; i<vhex.length; i++) {
                vint[i] = Integer.parseInt(vhex[i], 16);
            }
            if (vhex.length == 1) {
                monoVariants.put(icode, vint[0]);
            } else {
                polyVariants.put(icode, vint);
            }
        }
    }

    /**
     * Get the case variants of a character
     *
     * @param code the character whose case variants are required
     * @return the case variants of the character, excluding the character itself
     */

    public static int[] getCaseVariants(int code) {
        if (monoVariants == null) {
            build();
        }
        Integer mono = monoVariants.get(code);
        if (mono != null) {
            return new int[]{mono};
        } else {
            int[] result = polyVariants.get(code);
            if (result == null) {
                return EMPTY_INT_ARRAY;
            } else {
                return result;
            }
        }
    }

    private final static int[] EMPTY_INT_ARRAY = new int[]{};

    /**
     * Get the case variants of roman letters (A-Z, a-z), other than the letters A-Z and a-z themselves
     */

    /*@NotNull*/ public static int[] ROMAN_VARIANTS = {0x0130, 0x0131, 0x212A, 0x017F};

    // The data file casevariants.xml was formed by applying the following query to the XML
    // version of the Unicode database (for Saxon 9.6, the Unicode 6.2.0 version was used)

//    declare namespace u = "http://www.unicode.org/ns/2003/ucd/1.0";
//    <variants>{
//    let $chars := doc('ucd.all.flat.xml')/ * / * /u:char[@suc!='#' or @slc!='#']
//    for $c in $chars
//    let $variants := ($chars[(@cp, @suc[.!='#']) = $c/(@cp, @suc[.!='#'])] |
//                          $chars[(@cp, @slc[.!='#']) = $c/(@cp, @slc[.!='#'])]) except $c
//    return
//         if (count($variants) gt 0) then
//           <c n="{$c/@cp}" v="{string-join($variants/@cp, ",")}"/>
//         else ()
//
//    }</variants>

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.