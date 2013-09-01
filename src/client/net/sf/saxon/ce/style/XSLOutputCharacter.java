package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.UTF16CharacterSet;


/**
* An xsl:output-character element in the stylesheet. <br>
*/

public class XSLOutputCharacter extends StyleElement {

    private int codepoint = -1;
        // the character to be substituted, as a Unicode codepoint (may be > 65535)
    private String replacementString = null;

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals(StandardNames.CHARACTER)) {
                String s = atts.getValue(a);
                switch (s.length()) {
                    case 0:
                        compileError("character attribute must not be zero-length", "XTSE0020");
                        codepoint = 256; // for error recovery
                        break;
                    case 1:
                        codepoint = s.charAt(0);
                        break;
                    case 2:
                        if (UTF16CharacterSet.isHighSurrogate(s.charAt(0)) &&
                                UTF16CharacterSet.isLowSurrogate(s.charAt(1))) {
                            codepoint = UTF16CharacterSet.combinePair(s.charAt(0), s.charAt(1));
                        } else {
                            compileError("character attribute must be a single XML character", "XTSE0020");
                            codepoint = 256; // for error recovery
                        }
                        break;
                    default:
                        compileError("character attribute must be a single XML character", "XTSE0020");
                        codepoint = 256; // for error recovery
                }
        	} else if (f.equals(StandardNames.STRING)) {
        		replacementString = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }
        if (codepoint==-1) {
            reportAbsence("character");
            return;
        }

        if (replacementString==null) {
            reportAbsence("string");
            return;
        }

    }

    public void validate(Declaration decl) throws XPathException {
        if (!(getParent() instanceof XSLCharacterMap)) {
            compileError("xsl:output-character may appear only as a child of xsl:character-map", "XTSE0010");
        };
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

    public int getCodePoint() {
        return codepoint;
    }

    public String getReplacementString() {
        return replacementString;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.