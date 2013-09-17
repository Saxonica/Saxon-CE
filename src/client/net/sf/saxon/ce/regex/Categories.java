////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package client.net.sf.saxon.ce.regex;


import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.z.*;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NameChecker;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
<<<<<<< HEAD
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
=======
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
>>>>>>> 2ccb299... Changes to use new Unicode character categories file
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Data for Regular expression character categories. The data is in an XML file derived from the Unicode
 * database (In Saxon 9.6, this is based on Unicode 6.2.0). Since Saxon 9.4,
 * we no longer make use of Java's support for character categories since there are too many differences
 * from Unicode.
 */
public class Categories {


    private static HashMap<String, IntPredicate> CATEGORIES = null;

    static void build() {

        CATEGORIES = new HashMap<String, IntPredicate>(30);

        Configuration config = new Configuration();
        DocumentInfo doc;
        try {
            doc = config.buildDocument("categories.xml");
        } catch (XPathException e) {
            throw new RuntimeException("Failed to build categories.xml", e);
        }

<<<<<<< HEAD
        UnfailingIterator iter = doc.iterateAxis(Axis.DESCENDANT, new NameTest(Type.ELEMENT, "", "cat"));
=======
        AxisIterator iter = doc.iterateAxis(Axis.DESCENDANT, new NameTest(Type.ELEMENT, "", "cat"));
>>>>>>> 2ccb299... Changes to use new Unicode character categories file
        while (true) {
            NodeInfo item = (NodeInfo)iter.next();
            if (item == null) {
                break;
            }
            String cat = Navigator.getAttributeValue(item, "", "name");
            IntRangeSet irs = new IntRangeSet();
<<<<<<< HEAD
            UnfailingIterator iter2 = item.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
=======
            AxisIterator iter2 = item.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
>>>>>>> 2ccb299... Changes to use new Unicode character categories file
            while (true) {
                NodeInfo r = (NodeInfo)iter2.next();
                if (r == null) {
                    break;
                }
                String from = Navigator.getAttributeValue(r, "", "f");
                String to = Navigator.getAttributeValue(r, "", "t");
                irs.addRange(Integer.parseInt(from, 16), Integer.parseInt(to, 16));
            }

            CATEGORIES.put(cat, new IntSetPredicate(irs));
        }

        String c = "CLMNPSZ";
        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);
            IntPredicate ip = null;
            for (Map.Entry<String, IntPredicate> entry : CATEGORIES.entrySet()) {
                if (entry.getKey().charAt(0) == ch) {
                    ip = (ip == null ? entry.getValue() : new IntUnionPredicate(ip, entry.getValue()));
                }
            }
            CATEGORIES.put(ch + "", ip);
        }
    }

    public final static IntPredicate ESCAPE_s =
            new IntSetPredicate(IntHashSet.fromArray(new int[]{9, 10, 13, 32}));

    public final static IntPredicate ESCAPE_S = new IntComplementPredicate(ESCAPE_s);

    public final static IntPredicate ESCAPE_i = new IntPredicate() {
        public boolean matches(int value) {
            return NameChecker.isNCNameStartChar(value) || value == ':';
        }
    };

    public final static IntPredicate ESCAPE_I = new IntPredicate() {
        public boolean matches(int value) {
            return !(NameChecker.isNCNameStartChar(value) || value == ':');
        }
    };

    public final static IntPredicate ESCAPE_c = new IntPredicate() {
        public boolean matches(int value) {
            return NameChecker.isNCNameChar(value) || value == ':';
        }
    };

    public final static IntPredicate ESCAPE_C = new IntPredicate() {
        public boolean matches(int value) {
            return !(NameChecker.isNCNameChar(value) || value == ':');
        }
    };

    public final static IntPredicate ESCAPE_d = getCategory("Nd");

    public final static IntPredicate ESCAPE_D = new IntComplementPredicate(ESCAPE_d);

    static IntPredicate CATEGORY_P = getCategory("P");
    static IntPredicate CATEGORY_Z = getCategory("Z");
    static IntPredicate CATEGORY_C = getCategory("C");

    public final static IntPredicate ESCAPE_w = new IntPredicate() {
        public boolean matches(int value) {
            return !(CATEGORY_P.matches(value) || CATEGORY_Z.matches(value) || CATEGORY_C.matches(value));
        }
    };

    public final static IntPredicate ESCAPE_W = new IntComplementPredicate(ESCAPE_w);


    /**
     * Get a predicate to test characters for membership of one of the Unicode
     * character categories
     *
     * @param cat a one-character or two-character category name, for example L or Lu
     * @return a predicate that tests whether a given character belongs to the category
     */

    public static IntPredicate getCategory(String cat) {
        if (CATEGORIES == null) {
            build();
        }
        return CATEGORIES.get(cat);
    }


}

// The following stylesheet was used to generate the categories.xml file from the Unicode 6.2.0 database:

//<?xml version="1.0" encoding="UTF-8"?>
//<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
//    xmlns:xs="http://www.w3.org/2001/XMLSchema"
//    xmlns:f="http://local-functions/"
//    exclude-result-prefixes="xs f"
//    xpath-default-namespace="http://www.unicode.org/ns/2003/ucd/1.0"
//    version="3.0">
//    
//    <!-- Output XML representation of character categories -->
//    
//    <xsl:output method="xml" indent="yes" encoding="us-ascii" saxon:suppress-indentation="cat" xmlns:saxon="http://saxon.sf.net/"/>
//    
//    <xsl:param name="v6" select="doc('ucd.all.flat.xml')"/>
//    
//    <xsl:key name="cat-key" match="*[@cp]" use="@gc"/>
//    <xsl:key name="range-key" match="*[@first-cp]" use="@gc"/>
//    
//    <xsl:template name="main">
//      <categories>
//        <xsl:variable name="categories" select="distinct-values($v6/ucd/repertoire/(char|reserved)/@gc)"/>
//        <xsl:for-each select="$categories">
//            <xsl:sort select="."/>
//            
//            <cat name="{.}">
//              <xsl:variable name="chars" select="key('cat-key', ., $v6)/@cp"/>
//              <xsl:variable name="codes" select="for $c in $chars return f:hexToInt(0,$c)"/>
//              
//              <xsl:variable name="ranges" as="element()*">
//                <xsl:for-each-group select="$codes" group-adjacent=". - position()">
//                  <range f="{f:intToHex(current-group()[1])}" t="{f:intToHex(current-group()[1] + count(current-group()) - 1)}"/>
//                </xsl:for-each-group>
//              
//                <xsl:for-each select="key('range-key', ., $v6)">
//                  <range f="{f:intToHex(f:hexToInt(0,@first-cp))}" t="{f:intToHex(f:hexToInt(0,@last-cp))}"/>
//                </xsl:for-each>
//              </xsl:variable>
//              
//              <xsl:perform-sort select="$ranges">
//                <xsl:sort select="f:hexToInt(0,@f)"/>
//              </xsl:perform-sort>    
//              
//            </cat>         
//        </xsl:for-each>
//      </categories>    
//    </xsl:template>
//    
//    
//    <xsl:function name="f:hexToInt" as="xs:integer">
//      <xsl:param name="acc" as="xs:integer"/>
//      <xsl:param name="in" as="xs:string"/>
//      <xsl:choose>
//        <xsl:when test="$in eq ''">
//          <xsl:sequence select="$acc"/>
//        </xsl:when>
//        <xsl:otherwise>
//          <xsl:variable name="first" select="string-length(substring-before('0123456789ABCDEF', substring($in, 1, 1)))"/>
//          <xsl:sequence select="f:hexToInt($acc * 16 + $first, substring($in, 2))"/>
//        </xsl:otherwise>
//      </xsl:choose>
//    </xsl:function>
//    
//    <xsl:function name="f:intToHex" as="xs:string">
//      <xsl:param name="in" as="xs:integer"/>
//      <xsl:choose>
//        <xsl:when test="$in eq 0">
//          <xsl:sequence select="''"/>
//        </xsl:when>
//        <xsl:otherwise>
//          <xsl:variable name="last" select="substring('0123456789ABCDEF', $in mod 16 + 1, 1)"/>
//          <xsl:sequence select="concat(f:intToHex($in idiv 16), $last)"/>
//        </xsl:otherwise>
//      </xsl:choose>
//    </xsl:function>
//          
<<<<<<< HEAD
//</xsl:stylesheet>

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
=======
//</xsl:stylesheet>
>>>>>>> 2ccb299... Changes to use new Unicode character categories file
