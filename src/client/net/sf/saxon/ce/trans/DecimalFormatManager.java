package client.net.sf.saxon.ce.trans;

import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;

import java.util.HashMap;

/**
 * DecimalFormatManager manages the collection of named and unnamed decimal formats, for use by the
 * format-number() function.
 *
 * <p>In XSLT, there is a single set of decimal formats shared by the whole stylesheet. In XQuery 1.1, however,
 * each query module has its own set of decimal formats. The DecimalFormatManager to use is therefore linked
 * from the format-number() call on the expression tree.</p>
 * @author Michael H. Kay
 */

public class DecimalFormatManager {

    private DecimalSymbols defaultDFS;
    private HashMap<StructuredQName, DecimalFormatInfo> formatTable;   // table for named decimal formats
    private boolean usingOriginalDefault = true;

    /**
    * create a DecimalFormatManager and initialise variables
    */

    public DecimalFormatManager() {
        formatTable = new HashMap<StructuredQName, DecimalFormatInfo>(10);
        defaultDFS = new DecimalSymbols();
    }

    /**
    * Register the default decimal-format.
    * Note that it is an error to register the same decimal-format twice, even with different
    * precedence
    */

    public void setDefaultDecimalFormat(DecimalSymbols dfs, int precedence)
    throws XPathException {
        if (!usingOriginalDefault) {
            if (!dfs.equals(defaultDFS)) {
                XPathException err = new XPathException("There are two conflicting definitions of the default decimal format");
                err.setErrorCode("XTSE1290");
                err.setIsStaticError(true);
                throw err;
            }
        }
        defaultDFS = dfs;
        usingOriginalDefault = false;
        setNamedDecimalFormat(DEFAULT_NAME, dfs, precedence);
            // this is to trigger fixup of calls
    }

    final public static StructuredQName DEFAULT_NAME = 
            new StructuredQName("saxon", NamespaceConstant.SAXON, "default-decimal-format");

    /**
    * Method called at the end of stylesheet compilation to fix up any format-number() calls
    * to the "default default" decimal format
    */

    public void fixupDefaultDefault() throws XPathException {
        if (usingOriginalDefault) {
            setNamedDecimalFormat(DEFAULT_NAME, defaultDFS, -1000);
        }
    }

    /**
    * Get the default decimal-format.
    */

    public DecimalSymbols getDefaultDecimalFormat() {
        return defaultDFS;
    }

    /**
    * Set a named decimal format.
    * Note that it is an error to register the same decimal-format twice, unless the values are
     * equal, or unless there is another of higher precedence. This method assumes that decimal-formats
     * are registered in order of decreasing precedence
    * @param qName the name of the decimal format
    */

    public void setNamedDecimalFormat(StructuredQName qName, DecimalSymbols dfs, int precedence)
    throws XPathException {
		Object o = formatTable.get(qName);
		if (o != null) {
//    		if (o instanceof List) {
//    		    // this indicates there are forwards references to this decimal format that need to be fixed up
//    		    for (Iterator iter = ((List)o).iterator(); iter.hasNext(); ) {
//    		        FormatNumber call = (FormatNumber)iter.next();
//    		        call.fixup(dfs);
//    		    }
//    		} else {
                DecimalFormatInfo info = (DecimalFormatInfo)o;
            	DecimalSymbols old = info.dfs;
                int oldPrecedence = info.precedence;
                if (precedence < oldPrecedence) {
                    return;
                }
                if (precedence==oldPrecedence && !dfs.equals(old)) {
                    XPathException err = new XPathException("There are two conflicting definitions of the named decimal-format");
                    err.setErrorCode("XTSE1290");
                    err.setIsStaticError(true);
                    throw err;
                }
//            }
        }
        DecimalFormatInfo dfi = new DecimalFormatInfo();
        dfi.dfs = dfs;
        dfi.precedence = precedence;
        formatTable.put(qName, dfi);
    }

    /**
    * Register a format-number() function call that uses a particular decimal format. This
    * allows early compile time resolution to a DecimalFormatSymbols object where possible,
    * even in the case of a forwards reference
    */

//    public void registerUsage(StructuredQName qName, FormatNumber call) {
//        Object o = formatTable.get(qName);
//        if (o == null) {
//            // it's a forwards reference
//            List list = new ArrayList(10);
//            list.add(call);
//            formatTable.put(qName, list);
//        } else if (o instanceof List) {
//            // it's another forwards reference
//            List list = (List)o;
//            list.add(call);
//        } else {
//            // it's a backwards reference
//            DecimalFormatInfo dfi = (DecimalFormatInfo)o;
//            call.fixup(dfi.dfs);
//        }
//    }

    /**
    * Get a named decimal-format registered using setNamedDecimalFormat
    * @param qName The  name of the decimal format
    * @return the DecimalFormatSymbols object corresponding to the named locale, if any
    * or null if not set.
    */

    public DecimalSymbols getNamedDecimalFormat(StructuredQName qName) {
        DecimalFormatInfo dfi = ((DecimalFormatInfo)formatTable.get(qName));
        if (dfi == null) {
            return null;
        }
        return dfi.dfs;
    }

    private static class DecimalFormatInfo {
        public DecimalSymbols dfs;
        public int precedence;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
