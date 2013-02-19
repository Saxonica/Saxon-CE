package client.net.sf.saxon.ce.lib;

 /**
 * This class is not instantiated, it exists to hold a set of constants representing known
 * namespaces. For each of these, there is a constant for the namespace URI and for many of
 * them, there is a numeric constant used as the code for this namespace in the name pool.
 *
 * <p>This class also defines constant URIs for some objects other than namespaces -
 * for example, URIs that identify the various object models used in the JAXP XPath API,
 * and the Unicode codepoint collation URI.</p>
 *
 * @author Michael H. Kay
 */

public class NamespaceConstant {

	/**
	 * A URI representing the null namespace (actually, an empty string)
	 */

	public static final String NULL = "";
	/**
	 * The numeric URI code representing the null namespace (actually, zero)
	 */
	public static final short NULL_CODE = 0;
    /**
     * The namespace code for the null namespace
     */
    public static final int NULL_NAMESPACE_CODE = 0;

    /**
     * Fixed namespace name for XML: "http://www.w3.org/XML/1998/namespace".
     */
    public static final String XML = "http://www.w3.org/XML/1998/namespace";
    /**
     * Numeric code representing the XML namespace
     */
    public static final short XML_CODE = 1;
    /**
     * The namespace code for the XML namespace
     */
    public static final int XML_NAMESPACE_CODE = 0x00010001;


    /**
     * Fixed namespace name for XSLT: "http://www.w3.org/1999/XSL/Transform"
     */
    public static final String XSLT = "http://www.w3.org/1999/XSL/Transform";
    /**
     * Numeric code representing the XSLT namespace
     */
    public static final short XSLT_CODE = 2;

    /**
     * Fixed namespace name for SAXON: "http://saxon.sf.net/"
     */
    public static final String SAXON = "http://saxon.sf.net/";
    /**
     * Numeric code representing the SAXON namespace
     */
    public static final short SAXON_CODE = 3;

    /**
     * Namespace name for XML Schema: "http://www.w3.org/2001/XMLSchema"
     */
    public static final String SCHEMA = "http://www.w3.org/2001/XMLSchema";
    /**
     * Numeric code representing the schema namespace
     */
    public static final short SCHEMA_CODE = 4;

    /**
     * XML-schema-defined namespace for use in instance documents ("xsi")
     */
    public static final String SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    public static final short XSI_CODE = 5;

    /**
     * Standard namespace for Saxon "Interactive XSLT" extensions
     */

    public static final String IXSL = "http://saxonica.com/ns/interactiveXSLT";
    public static final String IXSL_VIRTUAL = IXSL + "-v";
    
    public static final short IXSL_CODE = 6;

    /**
     * Standard namespace for global javascript methods (defined on the Window object)
     */

    public static final String JS = "http://saxonica.com/ns/globalJS";

    /**
     * Namespace for pseudo-attributes of HTML DOM elements that represent
     * element properties: for example, the <code>checked</code> property
     * (which is not the same as the <code>checked</code> attribute) can be
     * accessed as prop:checked. Note that these attributes can be accessed
     * by name, but they are not included in the result of @* or @prop:*
     */

    public static final String HTML_PROP = "http://saxonica.com/ns/html-property";

     /**
     * Namespace for pseudo-attributes of HTML DOM elements that represent
     * style properties: for example, the <code>checked</code> property
     * (which is not the same as the <code>checked</code> attribute) can be
     * accessed as prop:checked. Note that these attributes can be accessed
     * by name, but they are not included in the result of @* or @prop:*
     */

    public static final String HTML_STYLE_PROP = "http://saxonica.com/ns/html-style-property";

    /**
     * The standard namespace for functions and operators
     */
    public static final String FN = "http://www.w3.org/2005/xpath-functions";

    /**
     * The standard namespace for system error codes
     */
    public static final String ERR = "http://www.w3.org/2005/xqt-errors";


    /**
     * Recognize the Microsoft namespace so we can give a suitably sarcastic error message
     */

    public static final String MICROSOFT_XSL = "http://www.w3.org/TR/WD-xsl";
    
    /**
     * Namespace for XHTML
     */
    public static final String XHTML = "http://www.w3.org/1999/xhtml";

    /**
     * Namespace for Scalable Vector Graphics
     */

    public static final String SVG = "http://www.w3.org/2000/svg";

    /**
     * The XMLNS namespace (used in DOM)
     */

    public static final String XMLNS = "http://www.w3.org/2000/xmlns/";


    /**
     * URI identifying the Unicode codepoint collation
     */

    public static final String CODEPOINT_COLLATION_URI = "http://www.w3.org/2005/xpath-functions/collation/codepoint";

    /**
     * URI identifying case-insensitive collation
     */

    public static final String CASE_INSENSITIVE_COLLATION_URI = "http://saxon.sf.net/collation/case-insensitive";

    /**
     * URI for the names of generated global variables
     */

    public static final String SAXON_GENERATED_GLOBAL = SAXON + "generated-global-variable";

    /**
     * Private constructor: class is never instantiated
     */

    private NamespaceConstant() {
    }

    /**
     * Determine whether a namespace is a reserved namespace
     */

    public static final boolean isReserved(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.equals(XSLT) ||
                uri.equals(FN) ||
                uri.equals(XML) ||
                uri.equals(SCHEMA)||
                uri.equals(SCHEMA_INSTANCE);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
