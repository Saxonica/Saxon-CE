package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.NamespaceException;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.DecimalFormatManager;
import client.net.sf.saxon.ce.trans.DecimalSymbols;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

/**
* Handler for xsl:decimal-format elements in stylesheet. <br>
*/

public class XSLDecimalFormat extends StyleElement {

    boolean prepared = false;

    String name;
    String decimalSeparator;
    String groupingSeparator;
    String infinity;
    String minusSign;
    String NaN;
    String percent;
    String perMille;
    String zeroDigit;
    String digit;
    String patternSeparator;

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        if (prepared) {
            return;
        }
        prepared = true;

		AttributeCollection atts = getAttributeList();

        for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.NAME)) {
        		name = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.DECIMAL_SEPARATOR)) {
        		decimalSeparator = atts.getValue(a);
        	} else if (f.equals(StandardNames.GROUPING_SEPARATOR)) {
        		groupingSeparator = atts.getValue(a);
        	} else if (f.equals(StandardNames.INFINITY)) {
        		infinity = atts.getValue(a);
        	} else if (f.equals(StandardNames.MINUS_SIGN)) {
        		minusSign = atts.getValue(a);
        	} else if (f.equals(StandardNames.NAN)) {
        		NaN = atts.getValue(a);
        	} else if (f.equals(StandardNames.PERCENT)) {
        		percent = atts.getValue(a);
        	} else if (f.equals(StandardNames.PER_MILLE)) {
        		perMille = atts.getValue(a);
        	} else if (f.equals(StandardNames.ZERO_DIGIT)) {
        		zeroDigit = atts.getValue(a);
        	} else if (f.equals(StandardNames.DIGIT)) {
        		digit = atts.getValue(a);
        	} else if (f.equals(StandardNames.PATTERN_SEPARATOR)) {
        		patternSeparator = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
    }

    public void validate(Declaration decl) throws XPathException {
        checkTopLevel(null);
        checkEmpty();
    }

    public DecimalSymbols makeDecimalFormatSymbols() throws XPathException {
        DecimalSymbols d = new DecimalSymbols();
        if (decimalSeparator!=null) {
            d.decimalSeparator = (toChar(decimalSeparator));
        }
        if (groupingSeparator!=null) {
            d.groupingSeparator = (toChar(groupingSeparator));
        }
        if (infinity!=null) {
            d.infinity = (infinity);
        }
        if (minusSign!=null) {
            d.minusSign = (toChar(minusSign));
        }
        if (NaN!=null) {
            d.NaN = (NaN);
        }
        if (percent!=null) {
            d.percent = (toChar(percent));
        }
        if (perMille!=null) {
            d.permill = (toChar(perMille));
        }
        if (zeroDigit!=null) {
            d.zeroDigit = (toChar(zeroDigit));
            if (!(d.isValidZeroDigit())) {
                compileError(
                    "The value of the zero-digit attribute must be a Unicode digit with value zero",
                    "XTSE1295");
            }
        }
        if (digit!=null) {
            d.digit = (toChar(digit));
        }
        if (patternSeparator!=null) {
            d.patternSeparator = (toChar(patternSeparator));
        }
        try {
            d.checkDistinctRoles();
        } catch (XPathException err) {
            compileError(err.getMessage(), "XTSE1300");
        }
        return d;
    }

    /**
     * Method supplied by declaration elements to add themselves to a stylesheet-level index
     * @param decl the Declaration being indexed. (This corresponds to the StyleElement object
     * except in cases where one module is imported several times with different precedence.)
     * @param top  the outermost XSLStylesheet element
     */

    public void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException
    {
        prepareAttributes();
        DecimalSymbols d = makeDecimalFormatSymbols();
        DecimalFormatManager dfm = getPreparedStylesheet().getDecimalFormatManager();
        if (name==null) {
            try {
                dfm.setDefaultDecimalFormat(d, decl.getPrecedence());
            } catch (XPathException err) {
                compileError(err.getMessage(), err.getErrorCodeQName());
            }
        } else {
            try {
                StructuredQName formatName = makeQName(name);
                try {
                    dfm.setNamedDecimalFormat(formatName, d, decl.getPrecedence());
                } catch (XPathException err) {
                    compileError(err.getMessage(), err.getErrorCodeQName());
                }
            } catch (XPathException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XTSE0020");
            } catch (NamespaceException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XTSE0280");
            }
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

    /**
     * Get the Unicode codepoint corresponding to a String, which must represent a single Unicode character
     * @param s the input string, representing a single Unicode character, perhaps as a surrogate pair
     * @return
     * @throws XPathException
     */
    private int toChar(String s) throws XPathException {
        int[] e = StringValue.expand(s);
        if (e.length!=1)
            compileError("Attribute \"" + s + "\" should be a single character", "XTSE0020");
        return e[0];
    }

}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
