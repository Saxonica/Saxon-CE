package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.DateTimeValue;

/**
 * The Version class holds the SAXON version information.
 */

public final class Version {
	// possible: Alpha/Beta/RC/R etc.
	// Following value MUST match suffix of corresponding license file
	// e.g saxon-license.R4.txt - prevents browser caching issues
    private static final String VERSION_PREFIX = ""; // may be empty string
    private static final String MAJOR_VERSION = "1"; // "0";
    
    private static final String MINOR_VERSION = "1";    
    private static final String RELEASE_DATE = "2013-02-22";
    private static final String MAJOR_RELEASE_DATE = "2013-02-22";
    private static final DateTimeValue EXPIRY_DATE;
    
    //private static final String EDITION = "CE"; // this is fetched from configuration

    static {
        try{
        	//Not used because expiry date is now in license file
            EXPIRY_DATE = (DateTimeValue)DateTimeValue.makeDateTimeValue("2012-03-31T23:59:59.0Z").asAtomic();
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

    private Version() {
        // class is never instantiated
    }

    /**
     * Return the name of this product. Supports the XSLT 2.0 system property xsl:product-name
     * @return the string "SAXON"
     */


    public static String getProductName() {
        return "Saxon-" + Configuration.getEditionCode(); // e.g. Saxon-CE
    }
    
    /**
     * Return the edition of this product - anticipated to remain 'CE'
     * @return the string "CE" - used by Verifier
     */
    public static String getProductEdition() {
        return Configuration.getEditionCode(); // e.g. CE
    }

   /**
     * Get the version number of the schema-aware version of the product
     * @return the version number of this version of Saxon, as a string
     */

   public static String getProductVariantAndVersion() {
       String edition = Configuration.getEditionCode();
       return edition + " " + getProductVersion();
    }

    /**
     * Get the user-visible version number of this version of the product
     * @return the version number of this version of Saxon, as a string: for example "9.0.1"
     */

    public static String getProductVersion() {
    	String prefixSeparator = (VERSION_PREFIX.equals(""))? "" : " "; 
        return VERSION_PREFIX + prefixSeparator + MAJOR_VERSION + "." + MINOR_VERSION;
    }
  
    /**
     * Get the suffix for the saxonce license file that matches this major version
     * @return the suffix, as a string: for example "R2" for Release 2.3
     */

    public static String getLicenseFileName() {
    	String prefix = (VERSION_PREFIX.equals(""))? "" : Character.toString(VERSION_PREFIX.charAt(0));
        return "saxonce-license-" + prefix + MAJOR_VERSION + ".txt";
    }

    /**
     * Get the version of the XSLT specification that this product supports
     * @return the string 2.0
     */

    public static String getXSLVersionString() {
        return "2.0";
    }

    /**
     * Get a message used to identify this product when a transformation is run using the -t option
     * @return A string containing both the product name and the product
     *     version
     */

    public static String getProductTitle() {
        return getProductName() + ' ' + getProductVersion() + " from Saxonica";
    }

    /**
     * Return a web site address containing information about the product. Supports the XSLT system property xsl:vendor-url
     * @return the string "http://www.saxonica.com/"
     */

    public static String getWebSiteAddress() {
        return "http://www.saxonica.com/ce";
    }

    /**
     * Get the expiry date for this Saxon-CE release
     * Not used because expiry date is now in license file
     */

    public static DateTimeValue getExpiryDate() {
        return EXPIRY_DATE;
    }
    
    public static String getMajorReleaseDate() {
        return MAJOR_RELEASE_DATE;
    }
    
    public static String getReleaseDate() {
        return RELEASE_DATE;
    }

    /**
     * Invoking client.net.sf.saxon.ce.Version from the command line outputs the build number
     * @param args not used
     */
    public static void main(String[] args) {
        System.err.println(getProductTitle());
    }
}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
