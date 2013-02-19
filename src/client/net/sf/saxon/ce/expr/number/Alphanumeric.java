package client.net.sf.saxon.ce.expr.number;

/**
 * This class contains static utility methods to test whether a character is alphanumeric, as defined
 * by the rules of xsl:number: that is, whether it is in one of the Unicode categories
 * Nd, Nl, No, Lu, Ll, Lt, Lm or Lo
 */

public class Alphanumeric {

    private static int[] zeroDigits = {
            0x0030, 0x0660, 0x06f0, 0x0966, 0x09e6, 0x0a66, 0x0ae6, 0x0b66, 0x0be6, 0x0c66, 0x0ce6,
            0x0d66, 0x0e50, 0x0ed0, 0x0f20, 0x1040, 0x17e0, 0x1810, 0x1946, 0x19d0, 0xff10,
            0x104a0, 0x107ce, 0x107d8, 0x107e2, 0x107ec, 0x107f6 };

    // These data sets were generated from the Unicode 4.0 database using a custom stylesheet.
    // (copied below; source in MyJava/Unicode-db4/listAlphanumeric.xsl)
    // Note that the characters in the CJK Extended Ideograph ranges A and B, 3400-4DB5 and
    // 20000-2A6D6 as well as 4E00-9FBB and AC00-D7A3 are not listed individually in the database,
    // and therefore need to be handled specially.


    /**
     * Determine whether a Unicode codepoint is alphanumeric, that is, whether it is in one of the
     * categories Nd, Nl, No, Lu, Ll, Lt, Lm or Lo
     * @param c the codepoint to be tested
     * @return true if the codepoint is in one of these categories
     */

    public static boolean isAlphanumeric(int c) {
        if (c <= 0x7F) {
            // Fast path for ASCII characters
            return (c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A);
        } else if (c <= 0xffff) {
            return Character.isLetterOrDigit((char)c);
        } else {
            for (int i=0; i<startAstralAlphaNumeric.length; i++) {
                if (c <= endAstralAlphaNumeric[i]) {
                    return (c >= startAstralAlphaNumeric[i]);
                }
            }
            return false;
        }
    }

    private static int[] startAstralAlphaNumeric = {
        0x10000, 0x1000D, 0x10028, 0x1003C, 0x1003F, 0x10050, 0x10080, 0x10107, 0x10140, 0x1018A,
        0x10300, 0x10320, 0x10330, 0x10380, 0x103A0, 0x103C8, 0x103D1, 0x10400, 0x104A0, 0x10800,
        0x10808, 0x1080A, 0x10837, 0x1083C, 0x1083F, 0x10A00, 0x10A10, 0x10A15, 0x10A19, 0x10A40,
        0x1D400, 0x1D456, 0x1D49E, 0x1D4A2, 0x1D4A5, 0x1D4A9, 0x1D4AE, 0x1D4BB, 0x1D4BD, 0x1D4C5,
        0x1D507, 0x1D50D, 0x1D516, 0x1D51E, 0x1D53B, 0x1D540, 0x1D546, 0x1D54A, 0x1D552, 0x1D6A8,
        0x1D6C2, 0x1D6DC, 0x1D6FC, 0x1D716, 0x1D736, 0x1D750, 0x1D770, 0x1D78A, 0x1D7AA, 0x1D7C4,
        0x1D7CE, 0x20000, 0x2F800
    };

    private static int[] endAstralAlphaNumeric = {
        0x1000B, 0x10026, 0x1003A, 0x1003D, 0x1004D, 0x1005D, 0x100FA, 0x10133, 0x10178, 0x1018A,
        0x1031E, 0x10323, 0x1034A, 0x1039D, 0x103C3, 0x103CF, 0x103D5, 0x1049D, 0x104A9, 0x10805,
        0x10808, 0x10835, 0x10838, 0x1083C, 0x1083F, 0x10A00, 0x10A13, 0x10A17, 0x10A33, 0x10A47,
        0x1D454, 0x1D49C, 0x1D49F, 0x1D4A2, 0x1D4A6, 0x1D4AC, 0x1D4B9, 0x1D4BB, 0x1D4C3, 0x1D505,
        0x1D50A, 0x1D514, 0x1D51C, 0x1D539, 0x1D53E, 0x1D544, 0x1D546, 0x1D550, 0x1D6A5, 0x1D6C0,
        0x1D6DA, 0x1D6FA, 0x1D714, 0x1D734, 0x1D74E, 0x1D76E, 0x1D788, 0x1D7A8, 0x1D7C2, 0x1D7C9,
        0x1D7FF, 0x2A6D6, 0x2FA1D
    };

    /**
     * Determine whether a character represents a decimal digit and if so, which digit.
     * @param in the Unicode character being tested.
     * @return -1 if it's not a decimal digit, otherwise the digit value.
     */

    public static int getDigitValue(int in) {
        for (int z=0; z<zeroDigits.length; z++) {
            if (in <= zeroDigits[z]+9) {
                if (in >= zeroDigits[z]) {
                    return in - zeroDigits[z];
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Determine which digit family a decimal digit belongs to: that is, return the corresponding zero digit.
     * @param in a Unicode character
     * @return if the character is a digit, return the Unicode character that represents zero in the same digit
     * family. Otherwise, return -1.
     */
    
    public static int getDigitFamily(int in){
        for (int z=0; z<zeroDigits.length; z++) {
            if (in <= zeroDigits[z]+9) {
                if (in >= zeroDigits[z]) {
                    return zeroDigits[z];
                } else {
                    return -1;
                }
            }
        }
        return -1;
        
    }

    private Alphanumeric(){}
}

// For completeness, here is the stylesheet used to generate these lists of ranges from UnicodeData.txt:

//<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
//   xmlns:xs="http://www.w3.org/2001/XMLSchema"
//   xmlns:f="http://saxonica.com/ns/unicode"
//   exclude-result-prefixes="xs f"
//>
//
//<!-- Output a list of the start and end points of contiguous ranges of characters
//     classified as letters or digits.
//
//     Note this doesn't handle the CJK Extended Ideograph ranges A and B, 3400-4DB5 and 20000-2A6D6,
//     which have to be edited in by hand. Also 4E00-9FBB and AC00-D7A3
//-->
//
//<xsl:output method="text"/>
//<xsl:variable name="data" select="doc('UnicodeData.xml')"/>
//
//<xsl:function name="f:isAlphaNum" as="xs:boolean">
//  <xsl:param name="char" as="element(Char)"/>
//  <xsl:sequence select="$char/Field3=('Nd', 'Nl', 'No', 'Lu', 'Ll', 'Lt', 'Lm', 'Lo')"/>
//</xsl:function>
//
//<xsl:function name="f:hexToInt" as="xs:integer?">
//  <xsl:param name="hex" as="xs:string?"/>
//  <xsl:sequence select="if (empty($hex)) then () else Integer:parseInt($hex, 16)"
//                xmlns:Integer="java:java.lang.Integer"/>
//</xsl:function>
//
//<xsl:param name="p"/>
//<xsl:template name="test">
//  <xsl:value-of select="f:hexToInt($p)"/>
//</xsl:template>
//
//<xsl:template name="main">
//
//    <xsl:text>int[] startPoints = new int[]{</xsl:text>
//    <xsl:for-each-group select="$data/*/Char" group-adjacent="concat(f:isAlphaNum(.), f:hexToInt(code) - position())">
//      <xsl:if test="f:isAlphaNum(.)">
//	      <xsl:text>0x</xsl:text>
//	      <xsl:value-of select="current-group()[1]/code"/>
//	      <xsl:text>, </xsl:text>
//	      <xsl:if test="position() mod 10 = 0">&#xa;</xsl:if>
//	    </xsl:if>
//    </xsl:for-each-group>
//    <xsl:text>};&#xa;&#xa;</xsl:text>
//    <xsl:text>int[] endPoints = new int[]{</xsl:text>
//    <xsl:for-each-group select="$data/*/Char" group-adjacent="concat(f:isAlphaNum(.), f:hexToInt(code) - position())">
//      <xsl:if test="f:isAlphaNum(.)">
//	      <xsl:text>0x</xsl:text>
//	      <xsl:value-of select="current-group()[last()]/code"/>
//	      <xsl:text>, </xsl:text>
//	      <xsl:if test="position() mod 10 = 0">&#xa;</xsl:if>
//	    </xsl:if>
//    </xsl:for-each-group>
//    <xsl:text>};&#xa;&#xa;</xsl:text>
//
//</xsl:template>
//
//
//</xsl:stylesheet>

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
