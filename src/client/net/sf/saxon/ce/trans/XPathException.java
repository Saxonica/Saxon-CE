package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import com.google.gwt.logging.client.LogConfiguration;
import org.xml.sax.Locator;


/**
 * XPathException is used to indicate an error in an XPath expression.
 * It will generally be either a StaticError or a DynamicError;
 * ValidationExceptions (arising from schema validation) form a third category
*/

public class XPathException extends Exception {

    private boolean isTypeError = false;
    private boolean isStaticError = false;
    private String locationText = null;
    private StructuredQName errorCode;
    private boolean hasBeenReported = false;
    private String message = "";
    private SourceLocator locator;

    /**
     * Create an XPathException with an error message
     * @param message the message explaining what is wrong. This should not include location information.
     */

    public XPathException(String message) {
        super();
        if (LogConfiguration.loggingIsEnabled()) {
        	this.message = message;
        }
    }

    /**
     * Create an XPathException that wraps another exception
     * @param err the wrapped error or exception
     */

    public XPathException(Throwable err) {
        super(err);
    }

    /**
     * Create an XPathException that supplies an error message and wraps an underlying exception
     * @param message the error message (which should generally explain what Saxon was doing when the
     * underlying exception occurred)
     * @param err the underlying exception (the cause of this exception)
     */

    public XPathException(String message, Throwable err) {
        super(err);
        if (LogConfiguration.loggingIsEnabled()) {
        	this.message = message;
        }
    }

    /**
     * Create an XPathException that supplies an error message and supplies location information
     * @param message the error message
     * @param loc indicates where in the user-written query or stylesheet (or sometimes in a source
     * document) the error occurred
     */

    public XPathException(String message, SourceLocator loc) {
        super();
        if (LogConfiguration.loggingIsEnabled()) {
        	this.message = message;
        }
        this.locator = loc;
    }
    
    public String getMessage() {
    	return message;
    }
    
    public String toString() {
    	return message;
    }

    /**
     * Create an XPathException that supplies an error message and an error code
     * @param message the error message
     * @param errorCode the error code - an eight-character code, which is taken to be in the standard
     * system error code namespace
     */

    public XPathException(String message, String errorCode) {
        super();
        if (LogConfiguration.loggingIsEnabled()) {
        	this.message = message;
        }
        setErrorCode(errorCode);
        if (errorCode.equals("XPTY0004")) {
            setIsTypeError(true);
        }
    }

    /**
     * Create an XPathException that supplies an error message and an error code and a locator
     * @param message the error message
     * @param errorCode the error code - an eight-character code, which is taken to be in the standard
     * system error code namespace
     * @param loc indicates where in the user-written query or stylesheet (or sometimes in a source
     * document) the error occurred
     */

    public XPathException(String message, String errorCode, SourceLocator loc) {
        super();
        if (LogConfiguration.loggingIsEnabled()) {
        	this.message = message;
        }
        setErrorCode(errorCode);
        this.locator = loc;
    }

    /**
     * Force an exception to a static error
     * @return this exception, marked as a static error
     */

    public XPathException makeStatic() {
        setIsStaticError(true);
        return this;
    }

    /**
     * Set additional location text. This gives extra information about the position of the error
     * in textual form. Where XPath is embedded within a host language such as XSLT, the
     * formal location information identifies the location of the error in the XSLT module,
     * while this string locates the error within a specific XPath expression. The information
     * is typically used only for static errors.
     * @param text additional information about the location of the error, designed to be output
     * as a prefix to the error message if desired. (It is not concatenated with the message, because
     * it may be superfluous in an IDE environment.)
     */

    public void setAdditionalLocationText(String text) {
        locationText = text;
    }

    /**
     * Get the additional location text, if any. This gives extra information about the position of the error
     * in textual form. Where XPath is embedded within a host language such as XSLT, the
     * formal location information identifies the location of the error in the XSLT module,
     * while this string locates the error within a specific XPath expression. The information
     * is typically used only for static errors.
     * @return additional information about the location of the error, designed to be output
     * as a prefix to the error message if desired. (It is not concatenated with the message, because
     * it may be superfluous in an IDE environment.)
     */

    public String getAdditionalLocationText() {
        return locationText;
    }

    /**
     * Mark this exception to indicate that it represents (or does not represent) a static error
     * @param is true if this exception is a static error
     */

    public void setIsStaticError(boolean is) {
        isStaticError = is;
    }

    /**
     * Ask whether this exception represents a static error
     * @return true if this exception is a static error
     */

    public boolean isStaticError() {
        return isStaticError;
    }

    /**
     * Mark this exception to indicate that it represents (or does not represent) a type error
     * @param is true if this exception is a type error
     */

    public void setIsTypeError(boolean is) {
        isTypeError = is;
    }

    /**
     * Ask whether this exception represents a type error
     * @return true if this exception is a type error
     */

    public boolean isTypeError() {
        return isTypeError;
    }

    /**
     * Set the error code. The error code is a QName; this method sets the local part of the name,
     * setting the namespace of the error code to the standard system namespace {@link client.net.sf.saxon.ce.lib.NamespaceConstant#ERR}
     * @param code The local part of the name of the error code
     */

    public void setErrorCode(String code) {
        if (code != null) {
            errorCode = new StructuredQName("err", NamespaceConstant.ERR, code);
        }
    }

    /**
     * Set the error code, provided it has not already been set.
     * The error code is a QName; this method sets the local part of the name,
     * setting the namespace of the error code to the standard system namespace {@link NamespaceConstant#ERR}
     * @param code The local part of the name of the error code
     */

    public void maybeSetErrorCode(String code) {
        if (errorCode == null && code != null) {
            errorCode = new StructuredQName("err", NamespaceConstant.ERR, code);
        }
    }

    /**
     * Set the error code. The error code is a QName; this method sets both parts of the name.
     * @param code The error code as a QName
     */

    public void setErrorCodeQName(StructuredQName code) {
        errorCode = code;
    }

    /**
     * Get the error code as a QName
     * @return the error code as a QName
     */

    public StructuredQName getErrorCodeQName() {
        return errorCode;
    }

    /**
     * Get the local part of the name of the error code
     * @return the local part of the name of the error code
     */

    public String getErrorCodeLocalPart() {
        return (errorCode == null ? null : errorCode.getLocalName());
    }

    /**
     * Get the namespace URI part of the name of the error code
     * @return the namespace URI part of the name of the error code
     */

    public String getErrorCodeNamespace() {
        return (errorCode == null ? null : errorCode.getNamespaceURI());
    }

    /**
     * Mark this error to indicate that it has already been reported to the error listener, and should not be
     * reported again
     * @param reported true if the error has been reported to the error listener
     */

    public void setHasBeenReported(boolean reported) {
        hasBeenReported = reported;
    }

    /**
     * Ask whether this error is marked to indicate that it has already been reported to the error listener,
     * and should not be reported again
     * @return true if this error has already been reported
     */

    public boolean hasBeenReported() {
        return hasBeenReported;
    }

    /**
     * Get the location of the exception
     */

    public SourceLocator getLocator() {
        return locator;
    }

    /**
     * Set the location of a message
     * @param locator the current location (or null)
     */

    public void setLocator(SourceLocator locator) {
        this.locator = locator;
    }


    /**
     * Set the location of a message, only if it is not already set
     * @param locator the current location (or null)
     */

    public void maybeSetLocation(SourceLocator locator) {
        if (this.locator == null) {
            this.locator = locator;
        }
    }

    /**
     * Tests whether this is a dynamic error that may be reported statically if it is detected statically
     * @return true if the error can be reported statically
     */

    public boolean isReportableStatically() {
        if (isStaticError() || isTypeError()) {
            return true;
        }
        if (errorCode != null && errorCode.getNamespaceURI().equals(NamespaceConstant.ERR)) {
            String local = errorCode.getLocalName();
            return local.equals("XTDE1260") ||
                    local.equals("XTDE1280") ||
                    local.equals("XTDE1390") ||
                    local.equals("XTDE1400") ||
                    local.equals("XDTE1428") ||
                    local.equals("XTDE1440") ||
                    local.equals("XTDE1460"); 
        }
        return false;
    }

    /**
     * Subclass of XPathException used to report circularities
     */

    public static class Circularity extends XPathException {

        /**
         * Create an exception indicating that a circularity was detected
         * @param message the error message
         */
        public Circularity(String message) {
            super(message);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
