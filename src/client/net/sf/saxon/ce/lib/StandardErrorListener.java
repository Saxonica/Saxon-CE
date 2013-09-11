package client.net.sf.saxon.ce.lib;

import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import com.google.gwt.logging.client.LogConfiguration;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <B>StandardErrorListener</B> is the standard error handler for XSLT and XQuery processing
 * errors, used if no other ErrorListener is nominated.
 *
 * @author Michael H. Kay
 */

public class StandardErrorListener implements ErrorListener{

    private int warningCount = 0;
    protected transient PrintStream errorOutput = System.err;
    private static Logger logger = Logger.getLogger("StandardErrorListener");
    /**
     * Create a Standard Error Listener
     */

    public StandardErrorListener() {
    }

    /**
     * Make a clean copy of this ErrorListener. This is necessary because the
     * standard error listener is stateful (it remembers how many errors there have been)
     *
     * @return a copy of this error listener
     */

    public StandardErrorListener makeAnother() {
        StandardErrorListener sel = new StandardErrorListener();
        sel.errorOutput = errorOutput;
        return sel;
    }

    // Note, when the standard error listener is used, a new
    // one is created for each transformation, because it holds
    // the recovery policy and the warning count.

    /**
     * Set output destination for error messages (default is System.err)
     *
     * @param writer The PrintStream to use for error messages
     */

    public void setErrorOutput(PrintStream writer) {
        errorOutput = writer;
    }

    /**
     * Get the error output stream
     *
     * @return the error output stream
     */

    public PrintStream getErrorOutput() {
        return errorOutput;
    }

    /**
     * Receive notification of a warning.
     * <p/>
     * <p>Transformers can use this method to report conditions that
     * are not errors or fatal errors.  The default behaviour is to
     * take no action.</p>
     * <p/>
     * <p>After invoking this method, the Transformer must continue with
     * the transformation. It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  transformer exception.
     * @see javax.xml.transform.TransformerException
     */

    public void warning(XPathException exception) {

        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }
        String message = "";
        if (exception.getLocator() != null) {
            message = getLocationMessage(exception) + "\n  ";
        }
        message += wordWrap(getExpandedMessage(exception));
        logger.log(Level.WARNING, message);
        errorOutput.println("Warning: " + message);
        warningCount++;
        if (warningCount > 25) {
            errorOutput.println("No more warnings will be displayed");
            warningCount = 0;
        }

    }

    /**
     * Receive notification of a non-recoverable error.
     * <p/>
     * <p>The application must assume that the transformation cannot
     * continue after the Transformer has invoked this method,
     * and should continue (if at all) only to collect
     * addition error messages. In fact, Transformers are free
     * to stop reporting events once this method has been invoked.</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     * @throws XPathException if the application
     *                              chooses to discontinue the transformation.
     */

    public void error(XPathException exception) {
        if (exception.hasBeenReported()) {
            // don't report the same error twice
            return;
        }
        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }
        String message = "Error " +
                    getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(getExpandedMessage(exception));
        logger.log(Level.SEVERE, message);
        errorOutput.println(message);
        if (exception instanceof XPathException) {
            exception.setHasBeenReported(true);
            // probably redundant. It's the caller's job to set this flag, because there might be
            // a non-standard error listener in use.
        }        
    }

    /**
     * Get a string identifying the location of an error.
     *
     * @param err the exception containing the location information
     * @return a message string describing the location
     */

    public String getLocationMessage(XPathException err) {
        SourceLocator loc = err.getLocator();
        while (loc == null) {
            if (err.getCause() instanceof XPathException) {
                err = (XPathException)err.getCause();
                loc = err.getLocator();
            } else {
                return "";
            }
        }
        return getLocationMessageText(loc);
    }

    private static String getLocationMessageText(SourceLocator loc) {
        return "at " + loc.getLocation();
    }

    /**
     * Abbreviate a URI (if requested)
     * @param uri the URI to be abbreviated
     * @return the abbreviated URI, unless full path names were requested, in which case
     * the URI as supplied
     */

    public static String abbreviatePath(String uri) {
        if (uri == null) {
            return null;
        }
        int slash = uri.lastIndexOf('/');
        if (slash >= 0 && slash < uri.length()-1) {
            return uri.substring(slash+1);
        } else {
            return uri;
        }
    }
    
    private static String getCodeMessage(StructuredQName qCode) {
    	String codeText = "";
        if (qCode != null) {
        	String code = qCode.getLocalName();
        	if (code.startsWith("XTTE")){
        		code = code.substring(4);
        		int q = Integer.parseInt(code);
        		String suffix = " must match its declared type";
        		switch (q) {
        			case 570:
        				codeText = " The value of a variable" + suffix;
        				break;
        			case 600:
        				codeText = " Default value of a template paremeter" + suffix;
        				break;
        			case 590:
        				codeText = " Supplied value of a template parameter" + suffix;
        				break;
        			default:
        		}
        	} else if (code.startsWith("XPTY")) {
        		code = code.substring(4);
        		int q = Integer.parseInt(code);
        		switch (q) {
        			case 4:
        				codeText = " The expression value is not consistent with the context in which it appears";
        				break;
        			case 18:
        				codeText = " Last step in path expression contains both nodes and atomic values";
        				break;
        			case 19:
        				codeText = " A path expression step contains an atomic value";
        				break;
        			case 20:
        				codeText = " In an axis step, the context item is not a node";
        				break;
        			default:
        		}
        	}
        }
        return codeText;
    }

    /**
     * Get a string containing the message for this exception and all contained exceptions
     *
     * @param err the exception containing the required information
     * @return a message that concatenates the message of this exception with its contained exceptions,
     *         also including information about the error code and location.
     */
    public static String getExpandedMessage(XPathException err) {

        StructuredQName qCode;
        String additionalLocationText;
        qCode = err.getErrorCodeQName();
        additionalLocationText = err.getAdditionalLocationText();
        if (qCode == null && err.getCause() instanceof XPathException) {
            qCode = ((XPathException)err.getCause()).getErrorCodeQName();
        }
        String message = "";
        String codeText = "";
        if (qCode != null) {
            if (qCode.getNamespaceURI().equals(NamespaceConstant.ERR)) {
                message = qCode.getLocalName();
            } else {
                message = qCode.getDisplayName();
            }
        }

        if (additionalLocationText != null) {
            message += " " + additionalLocationText;
        }

        Throwable e = err;
        int msgLen = message.length();
        while (true) {
            if (e == null) {
                break;
            }
            String next = e.getMessage();
            if (next == null) {
                next = "";
            }
            if (next.startsWith("client.net.sf.saxon.ce.trans.XPathException: ")) {
                next = next.substring(next.indexOf(": ") + 2);
            }
            if (!message.endsWith(next)) {
                if (!"".equals(message) && !message.trim().endsWith(":")) {
                    message += ": ";
                }
                message += next;
            }
            if (e instanceof XPathException) {
                e = e.getCause();

            } else {
                // e.printStackTrace();
                break;
            }
        }
        if (LogConfiguration.loggingIsEnabled()) {
	        if (msgLen == message.length()) {
	        	String msg = getCodeMessage(qCode);
	        	if (msg.length() != 0) {
	        		message += ": " + msg;
	        	}
	        }
        }

        return message;
    }

    /**
     * Wordwrap an error message into lines of 72 characters or less (if possible)
     *
     * @param message the message to be word-wrapped
     * @return the message after applying word-wrapping
     */

    private static String wordWrap(String message) {
        int nl = message.indexOf('\n');
        if (nl < 0) {
            nl = message.length();
        }
        if (nl > 100) {
            int i = 90;
            while (message.charAt(i) != ' ' && i > 0) {
                i--;
            }
            if (i > 10) {
                return message.substring(0, i) + "\n  " + wordWrap(message.substring(i + 1));
            } else {
                return message;
            }
        } else if (nl < message.length()) {
            return message.substring(0, nl) + '\n' + wordWrap(message.substring(nl + 1));
        } else {
            return message;
        }
    }

}

// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
//
