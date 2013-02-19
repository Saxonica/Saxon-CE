package client.net.sf.saxon.ce.trans;

import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.value.Value;



/**
 * XPathException is used to indicate an error in an XPath expression.
 * It will generally be either a StaticError or a DynamicError;
 * ValidationExceptions (arising from schema validation) form a third category
*/

public class XPathException extends Exception {

    private boolean isTypeError = false;
    private boolean isStaticError = false;
    private boolean isGlobalError = false;
    private String locationText = null;
    private StructuredQName errorCode;
    private transient Value errorObject;
    private boolean hasBeenReported = false;
    transient XPathContext context;
    private String message = "";
    private transient SourceLocator locator;
    // declared transient because a compiled stylesheet might contain a "deferred action" dynamic error
    // and the EarlyEvaluationContext links back to the source stylesheet.

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
     * Create an XPathException that supplies an error message and wraps an underlying exception
     * and supplies location information
     * @param message the error message (which should generally explain what Saxon was doing when the
     * underlying exception occurred)
     * @param loc indicates where in the user-written query or stylesheet (or sometimes in a source
     * document) the error occurred
     * @param err the underlying exception (the cause of this exception)
     */

    public XPathException(String message, SourceLocator loc, Throwable err) {
        super(err);
        if (LogConfiguration.loggingIsEnabled()) {
        	this.message = message;
        }
        this.locator = loc;
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
    }

    /**
     * Create an XPathException that supplies an error message and an error code and provides the
     * dynamic context
     * @param message the error message
     * @param errorCode the error code - an eight-character code, which is taken to be in the standard
     * system error code namespace
     * @param context the dynamic evaluation context
     */

    public XPathException(String message, String errorCode, XPathContext context) {
        super();
        if (LogConfiguration.loggingIsEnabled()) {
        	this.message = message;
        }
        setErrorCode(errorCode);
        setXPathContext(context);
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
     * Set dynamic context information in the exception object
     * @param context the dynamic context at the time the exception occurred
     */

    public void setXPathContext(XPathContext context) {
        this.context = context;
    }

    /**
     * Get the dynamic context at the time the exception occurred
     * @return the dynamic context if known; otherwise null
     */

    public XPathContext getXPathContext() {
        return context;
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
     * Mark this exception to indicate that it originated while evaluating a global
     * variable reference, and is therefore to be reported regardless of the try/catch
     * context surrounding the variable reference
     * @param is true if this exception is a global variable error
     */

    public void setIsGlobalError(boolean is) {
        isGlobalError = is;
    }

    /**
     * Ask whether this exception originated while evaluating a global
     * variable reference, and is therefore to be reported regardless of the try/catch
     * context surrounding the variable reference
     * @return true if this exception is a global variable error
     */

    public boolean isGlobalError() {
        return isGlobalError;
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
     * Set the error object associated with this error. This is used by the standard XPath fn:error() function
     * @param value the error object, as supplied to the fn:error() function
     */

    public void setErrorObject(Value value) {
        errorObject = value;
    }

    /**
     * Get the error object associated with this error. This is used by the standard XPath fn:error() function
     * @return the error object, as supplied to the fn:error() function
     */

    public Value getErrorObject() {
        return errorObject;
    }

    /**
     * Mark this error to indicate that it has already been reported to the error listener, and should not be
     * reported again
     * @param reported
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
     * Set the context of a message, only if it is not already set
     * @param context the current XPath context (or null)
     */

    public void maybeSetContext(XPathContext context) {
        if (getXPathContext() == null) {
            setXPathContext(context);
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
