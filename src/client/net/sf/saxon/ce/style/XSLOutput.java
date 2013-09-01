package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;
import java.util.*;

/**
* An xsl:output element in the stylesheet.
*/

public class XSLOutput extends StyleElement {

    private StructuredQName outputFormatName;
    private String method = null;
    private String version = null;
    private String indent = null;
    private String encoding = null;
    private String mediaType = null;
    private String doctypeSystem = null;
    private String doctypePublic = null;
    private String omitDeclaration = null;
    private String standalone = null;
    private String cdataElements = null;
    private String includeContentType = null;
    private String nextInChain = null;
    private String suppressIndentation = null;
    private String doubleSpace = null;
    private String representation = null;
    private String indentSpaces = null;
    private String lineLength = null;
    private String byteOrderMark = null;
    private String escapeURIAttributes = null;
    private String normalizationForm = null;
    private String recognizeBinary = null;
    private String requireWellFormed = null;
    private String undeclareNamespaces = null;
    private String useCharacterMaps = null;
    private HashMap<String, String> userAttributes = null;

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
		AttributeCollection atts = getAttributeList();
		String nameAtt = null;

        for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals(StandardNames.NAME)) {
        		nameAtt = Whitespace.trim(atts.getValue(a));
			} else if (f.equals(StandardNames.METHOD)) {
        		method = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.VERSION)) {
        		version = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.BYTE_ORDER_MARK)) {
                byteOrderMark = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.ENCODING)) {
        		encoding = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.OMIT_XML_DECLARATION)) {
        		omitDeclaration = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.STANDALONE)) {
        		standalone = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.DOCTYPE_PUBLIC)) {
        		doctypePublic = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.DOCTYPE_SYSTEM)) {
        		doctypeSystem = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.CDATA_SECTION_ELEMENTS)) {
        		cdataElements = atts.getValue(a);
        	} else if (f.equals(StandardNames.INDENT)) {
        		indent = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.MEDIA_TYPE)) {
        		mediaType = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.INCLUDE_CONTENT_TYPE)) {
        		includeContentType = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.NORMALIZATION_FORM)) {
        		normalizationForm = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.ESCAPE_URI_ATTRIBUTES)) {
        		escapeURIAttributes = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.USE_CHARACTER_MAPS)) {
        		useCharacterMaps = atts.getValue(a);
            } else if (f.equals(StandardNames.UNDECLARE_PREFIXES)) {
        		undeclareNamespaces = atts.getValue(a);
         	} else {
        	    String attributeURI = qn.getNamespaceURI();
        	    if ("".equals(attributeURI) ||
        	            NamespaceConstant.XSLT.equals(attributeURI) ||
        	            NamespaceConstant.SAXON.equals(attributeURI)) {
        		    checkUnknownAttribute(qn);
        		} else {
        		    String name = '{' + attributeURI + '}' + atts.getLocalName(a);
        		    if (userAttributes==null) {
        		        userAttributes = new HashMap<String, String>(5);
        		    }
        		    userAttributes.put(name, atts.getValue(a));
        		}
        	}
        }
        if (nameAtt!=null) {
            try {
                outputFormatName = makeQName(nameAtt);
            } catch (NamespaceException err) {
                compileError(err.getMessage(), "XTSE1570");
            } catch (XPathException err) {
                compileError(err.getMessage(), "XTSE1570");
            }
        }
    }

    /**
     * Get the name of the xsl:output declaration
     * @return the name, as a structured QName; or null for an unnamed output declaration
     */

    public StructuredQName getFormatQName() {
        return outputFormatName;
    }

    public void validate(Declaration decl) throws XPathException {
        checkTopLevel(null);
        checkEmpty();
    }

    public Expression compile(Executable exec, Declaration decl) {
        return null;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
