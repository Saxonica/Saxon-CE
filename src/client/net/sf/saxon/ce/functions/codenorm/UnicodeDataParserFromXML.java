package client.net.sf.saxon.ce.functions.codenorm;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class reads the data compiled into class UnicodeData, and builds hash tables
 * that can be used by the Unicode normalization routines. This operation is performed
 * once only, the first time normalization is attempted after Saxon is loaded.
 */

class UnicodeDataParserFromXML {

    // This class is never instantiated
    private UnicodeDataParserFromXML(){}

    /**
     * Called exactly once by NormalizerData to build the static data
     */

    static NormalizerData build(Configuration config) throws XPathException {

        DocumentInfo doc = config.buildDocument("normalizationData.xml");

        BitSet isExcluded = new BitSet(128000);
        BitSet isCompatibility = new BitSet(128000);

        NodeInfo canonicalClassKeys = null;
        NodeInfo canonicalClassValues = null;
        NodeInfo decompositionKeys = null;
        NodeInfo decompositionValues = null;

        UnfailingIterator iter = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
        while (true) {
            NodeInfo item = (NodeInfo)iter.next();
            if (item == null) {
                break;
            }
            if (item.getLocalPart().equals("CanonicalClassKeys")) {
                canonicalClassKeys = item;
            } else if (item.getLocalPart().equals("CanonicalClassValues")) {
                canonicalClassValues = item;
            } else if (item.getLocalPart().equals("DecompositionKeys")) {
                decompositionKeys = item;
            } else if (item.getLocalPart().equals("DecompositionValues")) {
                decompositionValues = item;
            } else if (item.getLocalPart().equals("ExclusionList")) {
                readExclusionList(item.getStringValue(), isExcluded);
            } else if (item.getLocalPart().equals("CompatibilityList")) {
                readCompatibilityList(item.getStringValue(), isCompatibility);
            }
        }

        Map<Integer, Integer> canonicalClass = new HashMap<Integer, Integer>(400);
        readCanonicalClassTable(canonicalClassKeys.getStringValue(), canonicalClassValues.getStringValue(), canonicalClass);


        Map<Integer, String> decompose = new HashMap<Integer, String>(18000);
        Map<Integer, Integer> compose = new HashMap<Integer, Integer>(15000);

        readDecompositionTable(decompositionKeys.getStringValue(), decompositionValues.getStringValue(),
                decompose, compose, isExcluded, isCompatibility);

        return new NormalizerData(canonicalClass, decompose, compose,
              isCompatibility, isExcluded);
    }

    /**
     * Reads exclusion list and stores the data
     */

    private static void readExclusionList(String s, BitSet isExcluded) {
        for (String tok : Whitespace.tokenize(s)) {
            int value = Integer.parseInt(tok, 32);
            isExcluded.set(value);
        }
    }

    /**
     * Reads compatibility list and stores the data
     */

    private static void readCompatibilityList(String s, BitSet isCompatible) {
        for (String tok : Whitespace.tokenize(s)) {
            int value = Integer.parseInt(tok, 32);
            isCompatible.set(value);
        }
    }

    /**
     * Read canonical class table (mapping from character codes to their canonical class)
     */

    private static void readCanonicalClassTable(String keyString, String valueString, Map<Integer, Integer> canonicalClasses) {
        ArrayList keys = new ArrayList(5000);

        for (String tok : Whitespace.tokenize(keyString)) {
            int value = Integer.parseInt(tok, 32);
            keys.add(Integer.valueOf(value));
        }

        int k = 0;
        for (String tok : Whitespace.tokenize(valueString)) {
            int clss;
            int repeat = 1;
            int star = tok.indexOf('*');
            if (star < 0) {
                clss = Integer.parseInt(tok, 32);
            } else {
                repeat = Integer.parseInt(tok.substring(0, star));
                clss = Integer.parseInt(tok.substring(star+1), 32);
            }
            for (int i=0; i<repeat; i++) {
                canonicalClasses.put(((Integer)keys.get(k++)).intValue(), clss);
            }
        }
    }

    /**
     * Read canonical class table (mapping from character codes to their canonical class)
     */

    private static void readDecompositionTable(
            String decompositionKeyString, String decompositionValuesString,
            Map<Integer, String> decompose, Map<Integer, Integer> compose,
            BitSet isExcluded, BitSet isCompatibility) {
        int k = 0;

        List<String> values = new ArrayList<String>(1000);
        for (String tok : Whitespace.tokenize(decompositionValuesString)) {
            String value = "";
            for (int c=0; c<tok.length();) {
                char h0 = tok.charAt(c++);
                char h1 = tok.charAt(c++);
                char h2 = tok.charAt(c++);
                char h3 = tok.charAt(c++);
                int code = ("0123456789abcdef".indexOf(h0)<<12) +
                     ("0123456789abcdef".indexOf(h1)<<8) +
                     ("0123456789abcdef".indexOf(h2)<<4) +
                     ("0123456789abcdef".indexOf(h3)); // was <<12
                value += (char)code;
            }
            values.add(value);
        }


        for (String tok : Whitespace.tokenize(decompositionKeyString)) {
            int key = Integer.parseInt(tok, 32);
            String value = values.get(k++);
            decompose.put(key, value);
            // only compositions are canonical pairs
            // skip if script exclusion

            if (!isCompatibility.get(key) && !isExcluded.get(key)) {
                char first = '\u0000';
                char second = value.charAt(0);
                if (value.length() > 1) {
                    first = second;
                    second = value.charAt(1);
                }

                // store composition pair in single integer

                int pair = (first << 16) | second;
                compose.put(pair, key);
            }
        }

        // Add algorithmic Hangul decompositions
        // This fragment code is copied from the normalization code published by Unicode consortium.
        // See module net.sf.saxon.serialize.codenorm.Normalizer for applicable copyright information.

        for (int SIndex = 0; SIndex < SCount; ++SIndex) {
            int TIndex = SIndex % TCount;
            char first, second;
            if (TIndex != 0) { // triple
                first = (char)(SBase + SIndex - TIndex);
                second = (char)(TBase + TIndex);
            } else {
                first = (char)(LBase + SIndex / NCount);
                second = (char)(VBase + (SIndex % NCount) / TCount);
            }
            int pair = (first << 16) | second;
            int key = SIndex + SBase;
            decompose.put(key, String.valueOf(first) + second);
            compose.put(pair, key);
        }
    }

    /**
     * Hangul composition constants
     */
    private static final int
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172

}

// This class has its origins in the normalization software published
// by the Unicode Consortium.

// Modified by Michael Kay (Saxonca) to change the way in which the data files are stored.

// * Copyright (c) 1991-2005 Unicode, Inc.
// * For terms of use, see http://www.unicode.org/terms_of_use.html
// * For documentation, see UAX#15.<br>
// * The Unicode Consortium makes no expressed or implied warranty of any
// * kind, and assumes no liability for errors or omissions.
// * No liability is assumed for incidental and consequential damages
// * in connection with or arising out of the use of the information here.