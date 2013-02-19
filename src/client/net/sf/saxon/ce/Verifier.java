package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Version;
//import client.net.sf.saxon.ce;.lib.FeatureKeys;
//import client.net.sf.saxon.ce.lib.FeatureKeys;
//import client.net.sf.saxon.ce;.trans.LicenseException;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.functions.FunctionLibrary;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.sxpath.AbstractStaticContext;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.DateValue;
import client.net.sf.saxon.ce.value.DateTimeValue;
import client.net.sf.saxon.ce.value.DayTimeDurationValue;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Window;

/**
  * The class for Saxon-CE that extracts data from issued encrypted license keys and verifies the
  * data meets licensing rules. Date based verification is the same as for Saxon-EE. Each license
  * is valid only for major versions, this is constrained by license filename, but can/should also be
  * enforced by setting UpgradeDays=1. The license filename is changed for major releases because
  * web clients often cache license files.
  * 
  * The corresponding encryption class for SaxonCE is at: licensing.src.com.websina.tool.SaxonCELicense
  */
public class Verifier {

    private static final int MS_A_DAY = 24 * 3600 * 1000;
    private static HashMap<String, String> features;
    private static Logger logger = Logger.getLogger("Verifier");

    public Verifier() {
    }
    
    /**
     * The main method for this class. It's purpose is to throw an exception
     * if the license is not found, corrupt or invalid in any way
     */
    public static void loadLicense() throws LicenseException {
    	try {
        String encryptedContent = getFileContent();
        String plainLicense = getLicenseText(encryptedContent);
        configure(plainLicense);
    	} catch(LicenseException le) { throw le; }
    	  catch(Exception e) {
    		  // should not happen, but just in case
    		  throw new LicenseException(e.getMessage(), LicenseException.CANNOT_READ );
    	}

    }

     private static String getFeature(String name){
    	String value = null;
    	if (features != null && features.containsKey(name)) {
    		value = features.get(name);
    	}
    	return value;
    }
    
    /**
     * Parses the formatted plain-text license into name/value pairs that
     * are added to the static HashMap features field 
     * @param licenseText The license data in plain-text form
     * @return If successful returns boolean true else returns false
     */
    private static boolean loadLicenseFeatures(String licenseText) {
    	boolean isValid = true;
    	try {
	    	features = new HashMap<String, String>();
			String[] fields = licenseText.split("[\n=]");
			String[] reqFields = new String[] { 
					"Email", "Serial", "Issued", "Expiration", "UpgradeDays", "MaintenanceDays", "Domains"};
			boolean append = false;
			String keyName  = "";
			for (int i = 0; i < fields.length; i++){
				String field = fields[i].trim();
				if (i % 2 == 0) {
					append = (Arrays.asList(reqFields).contains(field));
					char ch =  (field.equals(""))? '\0' : field.charAt(0);
					if (ch == '\0' || ch == '!' || ch == '#') {
						i++;
						continue;
					}
					keyName = field;
				} else if (append) {
					features.put(keyName, field);
				}
			}
    	} catch(Exception e) {
    		isValid = false;
    	}
    	return isValid;
    }

    /**
     * Extracts the plain-text license data From the encrypted part of the 
     * text within the license file
     * @param license The string that was read from the license file
     *                complete with the encrypted license data, formatting
     *                characters and any preamble
     * @return The license data string, ready for parsing
     */
	private static String getLicenseText(String license) {
		license = license.trim();
		int absBreakPos = license.lastIndexOf("\n\n");
		int winBreakPos = license.lastIndexOf("\r\n\r\n");
		int resBreakPos = (winBreakPos > absBreakPos)? winBreakPos + 2: absBreakPos;
		if (resBreakPos > -1) {
			license = license.substring(resBreakPos + 2);
		}
		
		license = license.replace("\r\n", "");
		license = license.replace("\n", "");
		String decryptKey = extractKeyFromFile(license);
		String decryptHex = license.substring(0, license.length() - 48);
		byte[] hexBytes = convertHexToBinary(decryptHex);
		byte[] lrsBytes = getLrs(decryptKey, hexBytes.length);
		byte[] decrypted = xorBytes(hexBytes, lrsBytes);
		String text = new String(decrypted);
		return text;
	}
	
	// The hard-coded key used to decrypt the unique key appended to the license
	private static final String fillText = "Ti8-A9QsRz921_+23Toa8AsTQ";
	
	private static String extractKeyFromFile(String licenseText){
		String txtFill = fillText;
		String decryptKey = licenseText.substring(licenseText.length() - 48);
		
		byte[] keyBytes = convertHexToBinary(decryptKey);
		byte[] txtLrs = getLrs(txtFill, 24);
		byte[] decryptByte = xorBytes(keyBytes, txtLrs);
		
		return new String(decryptByte);
	}
	
	private static byte[] xorBytes(byte[] bytes, byte[] lrsBytes) {
		byte[] result = new byte[bytes.length];
		for(int x = 0; x < bytes.length; x++) {
			result[x] = (byte)(bytes[x] ^ lrsBytes[x]);
		}
		return result;
	}
	/**
	 * 
	 * @param str The Fill for the 'register' used to generate the
	 *            Linear Recursive Sequence (LRS)
	 * @param lrsLength - The length of the sequence to generate
	 * @return An LRS based on the provided params 
	 */
	private static byte[] getLrs(String str, int lrsLength) {

		byte[] bLrs = new byte[lrsLength];
		byte[] bytes = str.getBytes();
		int count = 0;
		int offset = 8;
		
		while (count < lrsLength) {
			for (int i = 0; i < bytes.length; i++) {
				byte fr = bytes[i];

				byte nr = fr;
			    
			    for (int y = 0; y < offset; y++) {
			    	// set the tapping points
			    	byte le = (byte)(((nr & 1) == 1)? 0x80: 0); // use bit 8 value to get value 1 to xor
			    	byte le2 = (byte)(((nr & 2) == 2)? 0x80: 0); // use bit 7 value to get value 2 to xor
			    	// xor (or modulo addition) for a single Tap
			    	byte xle = (byte)(le ^ le2);
			    	// unsigned shift right - using & prevents signed arithmetic operation:
					byte tgtRegster = (byte) ((nr & 0xFF) >>> 1);
					// feedback Tap output into the register input
			        nr = (byte)(tgtRegster | xle);
			    }
				bLrs[count] = nr;
				count++;
				if (!(count < lrsLength)) {
					break;
				}
		    				offset++;
				if(offset > 16) {
					offset = 8; // for performance
				}
			}
		}		
		return bLrs;
	}

    /**
     * It returns how many days are left for the license.
     * The value 0 is valid, it may indicate a never expire license.
     * @param dtf Must be an instance of DateTimeFormat for the short_date
     * patter - passed as an argument for peformance reasons only
     * @return how many days are left for the license.
     */
    protected static int daysLeft(DateTimeFormat dtf) throws XPathException {
        String expiration = getFeature("Expiration");
        if (expiration == null) {
            return -1;
        } else if (expiration.trim().isEmpty() || expiration.contains("never")) {
            return 0;
        }
        try {
            Date expiryDate = dtf.parse(expiration);
            long time = expiryDate.getTime(); //Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
            return 1 + (int) (time / MS_A_DAY);
        } catch (Exception err) {
            throw new XPathException("Invalid expiry date found in license");
        }
    }

    /**
     * Display a message about the license status - not currently used
     * but carried over from Saxon-EE.
     * @param config the Configuration
     */
    public static void displayLicenseMessage() throws LicenseException {
        if (features == null) {
            loadLicense();
        }
        String sxnLicense = "Saxon evaluation license "; 
        	try {
        		
        		DateTimeFormat dtf = DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT);
	            int left = daysLeft(dtf);
	            if (left == 1) {
	            	logger.log(Level.WARNING, sxnLicense + "expires tomorrow!");
	            } else if (left > 0 && left < 15) {
	            	logger.log(Level.WARNING, sxnLicense + "expires in " + left + " days");
	            } else if (left > 0 && left < 60) {
	            	logger.log(Level.INFO, sxnLicense + "expires in " + left + " days");
	            }
	            logger.log(Level.INFO, "Saxon license registered for domains: " + Verifier.getFeature("Domains"));
	            logger.log(Level.INFO, "Product Version: " + Version.getProductTitle());
        	} catch(Exception e) {
        		throw new LicenseException(sxnLicense + "is invalid: " + e.getMessage(), LicenseException.INVALID);
        	}
        	
    }

    private static String getFileContent() throws LicenseException {
        URI home = Configuration.getLocation();

        if (home != null) {
        	String path = getLicensePath(home);

            try {
                CharSequence ch = XMLDOM.makeHTTPRequest(path);
                return ch.toString();
            } catch (Exception e) {
            	throw new LicenseException("No Saxon-CE license found at: " + path, LicenseException.NOT_FOUND); 
            }
        } else {
        	throw new LicenseException("Invalid path found when attempting to determine Saxon-CE license location", LicenseException.NOT_FOUND);
        }
    }
    
    /**
     * Fetches the src attribute of the script element for the Saxonce js entry point 
     * and uses this to get the resolved license path. License file name suffix is
     * derived from the major version number to ensure it is not chached.
     * 
     * @param baseURI Used to resolve a relative URI in the src attribute
     * @return the full URI string to be used in the request to fetch the license
     *         e.g.  http://localhost/main/saxonce-licenseR2.txt"
    */
    private static String getLicensePath(URI baseURI) {
        NodeList<Element> scripts = Xslt20ProcessorImpl.getScriptsOnLoad();
        String sourceURI = "";
        for (int i=0; i<scripts.getLength(); i++) {
            String type = scripts.getItem(i).getAttribute("type");
            if (type.equals("text/javascript")) {
                sourceURI = scripts.getItem(i).getAttribute("src");
                if(sourceURI.endsWith("Saxonce.nocache.js")) {
                	break;
                }
            }
        }
        
        URI resolvedURI = baseURI.resolve(sourceURI);
        return resolvedURI.resolve("../" + Version.getLicenseFileName()).toString();
    }
    
    /**
     * Extract just the domain name from the URI string
     *      returns saxonica.com
     * @param uriString Any absolute URI string
     *  e.g. http://saxonica.com:8080/doc/index.html?pos=825
     * @return the domain name as a string, e.g. saxonica.com
     */
    private static String getDomainFromUri(String uriString)
    {
    	int pathStart = uriString.indexOf("//") + 2;

    	int pathEnd = indexOfAny(uriString, new char[] {':','?','/'}, pathStart);
    	if (pathEnd < 0) {
    		return uriString.substring(pathStart);
    	} else {
    		return uriString.substring(pathStart, pathEnd);
    	}
    	
    }
    /**
     * Utility function to find the position of the first matching
     * char in a string
     * @param str The string to search
     * @param searchChars The characters to look for
     * @param start The first position at which to start looking
     * @return the position in the string or -1 if no match found
     */
    public static int indexOfAny(String str, char[] searchChars, int start) {

        if ((str == null) || (searchChars == null) || start < 0 || start >= str.length()) {
            return -1;
        }
        int sz = searchChars.length;
        
        if (start > 0) {
        	str = str.substring(start);        	
        }
        int max_value = str.length() + 2; // equiv to max_value
        int ret = max_value;

        int tmp = 0;
        for (int i = 0; i < sz; i++) {
            char ch = searchChars[i];
            tmp = str.indexOf(ch);
            if (tmp == -1) {
                continue;
            }

            if (tmp < ret) {
                ret = tmp;
            }
        }
        
        if (ret < 0) {
        	return -1;
        } else {
        	return (ret == max_value) ? -1 : ret + start;
        }
    }
    
    /**
     * Applies licensing rules to license field values and throws an exception if license
     * found to be invalid.
     * @param fileString - the plain-text license comprising name/value pairs
     */
    private static void configure(String fileString) {
        try {

            boolean valid = loadLicenseFeatures(fileString);
            if (!valid) {
                throw new LicenseException("Invalid license file found", LicenseException.INVALID);
            }
            String uriString = Configuration.getLocation().toString();
            
            if (!uriString.startsWith("file://")) {
            	String domain = getDomainFromUri(uriString);
	            if (!verify(domain)) {
	            	throw new LicenseException("License file not registered for: " + domain, LicenseException.INVALID);
	            }
            }
            DateTimeFormat dtf = DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT);
            
            if (daysLeft(dtf) < 0) {
                throw new LicenseException("License (serial number " + getSerialNumber() + ") has expired",
                        LicenseException.EXPIRED);
            }
            
            Date licenseIssued = dtf.parse(getFeature("Issued"));
            Date majorVersionIssued = dtf.parse(Version.getMajorReleaseDate());

            String upgradeDays = getFeature("UpgradeDays");
            int udays = 366;
            if (upgradeDays != null) {
                udays = Integer.parseInt(upgradeDays.trim());
            }

            long msSinceMajorRelease = majorVersionIssued.getTime() - licenseIssued.getTime();
            long secsSinceMajorRelease = msSinceMajorRelease / 1000;
            if (secsSinceMajorRelease > 60*60*24*udays) {
                throw new LicenseException("The installed license (serial number " +
                        getSerialNumber() + ") does not cover upgrade to this Saxon version",
                        LicenseException.EXPIRED);
            }

            Date minorVersionIssued = dtf.parse(Version.getReleaseDate());
            String maintenanceDays = getFeature("MaintenanceDays");
            int mdays = udays;
            if (maintenanceDays != null) {
                mdays = Integer.parseInt(maintenanceDays.trim());
            }
            long msSinceMinorRelease = minorVersionIssued.getTime() - licenseIssued.getTime();
            long secsSinceMinorRelease = msSinceMinorRelease / 1000;
            if (secsSinceMinorRelease > 60*60*24*mdays) {
                throw new LicenseException("The installed license (serial number " +
                        getSerialNumber() + ") does not cover this Saxon maintenance release",
                        LicenseException.EXPIRED);
            }
        } catch (LicenseException le) {
        	throw le;
        } catch (Exception parseException) {
            throw new LicenseException("Invalid date in license file", LicenseException.INVALID);
        }
    }
    
    /**
     * Get the serial number of the license
     * @return the serial number
     */
    private static String getSerialNumber() {
        return getFeature("Serial");
    }

    /**
     * Convert a Hex String into the corresponding byte array by encoding
     * each two hexadecimal digits as a byte.
     *
     * @param hex Hexadecimal digits representation
     * @return byte Byte array
     */
    private static byte[] convertHexToBinary(String hex) {
        int len = hex.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            char c1 = hex.charAt(i*2);
            char c2 = hex.charAt(i*2 + 1);
            if ((c1 >= '0') && (c1 <= '9')) {
                out[i] = (byte)((c1 - '0') * 16);
            } else if ((c1 >= 'A') && (c1 <= 'F')) {
                out[i] = (byte)((c1 - 'A' + 10) * 16);
            } else {
                throw new IllegalArgumentException();
            }
            if ((c2 >= '0') && (c2 <= '9')) {
                out[i] += (c2 - '0');
            } else if ((c2 >= 'A') && (c2 <= 'F')) {
                out[i] += (c2 - 'A' + 10);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return out;
    }

    /**
     * This method validates license data based on the the host domain name being
     * included in the domains list in the encrypted license file. Any www prefix
     * found in the domain name is ignored
     * @param the domain name obtained by from the JavaScript Location property
     * @return a boolean whether or the license is valid.
     */
    private static boolean verify(String domainName) {
    	// encrypted because modifying this provides a backdoor through the verification
    	String appendedDomains = getLicenseText("5E82E6B47FD17FB1F5B3F29B87B027CA50F176D92A" + 
               "87B1FA5F8446ADD4BDEB9884D822C84D953789B17D875F"); // " localhost 127.0.0.1";
    	
    	String domainList = getFeature("Domains") + appendedDomains; 
		domainName = (domainName.toLowerCase().startsWith("www.")) ? 
				domainName.substring(4) : domainName;
    	String domains[] = domainList.split("[ \n]");
    	for (int i = 0; i < domains.length; i++) {
    		String licensedDomain = domains[i];
    		licensedDomain = (licensedDomain.toLowerCase().startsWith("www.")) ? 
    				licensedDomain.substring(4) : licensedDomain;
    		if(domains[i].equalsIgnoreCase(domainName)) {
    			return true;
    		}
    	}
    	return false;
    }

}

// Copyright (C) 2004-2010 Saxonica Limited. All rights reserved.
// Reproduction, distribution, or modification of this code is prohibited.
