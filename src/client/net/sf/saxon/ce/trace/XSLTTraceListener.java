package client.net.sf.saxon.ce.trace;

import client.net.sf.saxon.ce.expr.parser.CodeInjector;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StandardNames;

/**
 * A Simple trace listener for XSLT that writes messages (by default) to the developer console
 */

public class XSLTTraceListener extends AbstractTraceListener  {

    //@Override
    public CodeInjector getCodeInjector() {
        return new XSLTTraceCodeInjector();
    }

    /**
     * Generate attributes to be included in the opening trace element
     */

    protected String getOpeningAttributes() {
        return "xmlns:xsl=\"" + NamespaceConstant.XSLT + '\"' +
        	   " xmlns:ixsl=\"" + NamespaceConstant.IXSL + '\"' ;
    }

    /**
     * Get the trace element tagname to be used for a particular construct. Return null for
     * trace events that are ignored by this trace listener.
     */

    /*@Nullable*/ protected String tag(int construct) {
        return tagName(construct);
    }

    public static String tagName(int construct) {
        if (construct < 1024) {
            return StandardNames.getDisplayName(construct);
        }
        switch (construct) {
            case Location.LITERAL_RESULT_ELEMENT:
                return "LRE";
            case Location.LITERAL_RESULT_ATTRIBUTE:
                return "ATTR";
            case Location.LET_EXPRESSION:
                return "xsl:variable";
            case Location.EXTENSION_INSTRUCTION:
                return "extension-instruction";
            case Location.TRACE_CALL:
                return "user-trace";
            default:
                return null;
            }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.