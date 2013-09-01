package client.net.sf.saxon.ce.trace;

//import java.io.PrintStream;
import client.net.sf.saxon.ce.Version;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.parser.CodeInjector;
import client.net.sf.saxon.ce.lib.GenericLogHandler;
import client.net.sf.saxon.ce.lib.StandardErrorListener;
import client.net.sf.saxon.ce.lib.TraceListener;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * This is the standard trace listener equivalent to that used when the -T option is specified on the command line.
 * There are two variants, represented by subclasses: one for XSLT, and one for XQuery. The two variants
 * differ in that they present the trace output in terms of constructs used in the relevant host language.
 */

public abstract class AbstractTraceListener implements TraceListener {
    private int indent = 0;
    //private PrintStream out = System.err;
    private Logger logger = Logger.getLogger("Trace");
    /*@NotNull*/ private static StringBuffer spaceBuffer = new StringBuffer("                ");

    /**
     * Get the associated CodeInjector to be used at compile time to generate the tracing calls
     */

    public CodeInjector getCodeInjector() {
        return new TraceCodeInjector();
    }
    
    private long t_total;

    /**
     * Called at start
     */

    public void open() {
    	prevModule = "";
    	t_total = System.currentTimeMillis();
        logger.finest("<trace " +
                "saxon-version=\"" + Version.getProductVersion()+ "\" " +
                getOpeningAttributes() + '>');
        indent++;
    }

    protected abstract String getOpeningAttributes();

    /**
     * Called at end
     */

    public void close() {
    	t_total = t_total - System.currentTimeMillis();
        indent--;
        logger.finest("</trace>");
        GenericLogHandler.dumpTrace();
    }
    
    public void terminate() {
        indent = 0;
        // xml trace will have have been dumped already
    }
    
    /**
     * Called when an instruction in the stylesheet gets processed
     */
    
    public void enterChooseItem(String test) {
    	if (test.isEmpty()) {
    		logger.finest(AbstractTraceListener.spaces(indent) + "<xsl:otherwise>");
    	} else {
    		logger.finest(AbstractTraceListener.spaces(indent) + "<xsl:when test=\"" + escape(test) + "\">");
    	}
    	indent++;
    }
    
    public void leaveChooseItem(String test) {
    	if (test.isEmpty()) {
    		logger.finest(AbstractTraceListener.spaces(indent) + "</xsl:otherwise>");
    	} else {
    		logger.finest(AbstractTraceListener.spaces(indent) + "</xsl:when>");
    	}
    	indent--;
    }
    
    private static String prevModule = "";

    public void enter(/*@NotNull*/ InstructionInfo info, XPathContext context) {
        StructuredQName infotype = info.getConstructType();
        StructuredQName qName = info.getObjectName();
        String tag = tag(infotype);
        if (tag==null) {
            // this TraceListener ignores some events to reduce the volume of output
            return;
        }
        String file = StandardErrorListener.abbreviatePath(info.getSystemId());
        String msg = AbstractTraceListener.spaces(indent) + '<' + tag;
        String name = (String)info.getProperty("name");
        if (name!=null) {
            msg += " name=\"" + escape(name) + '"';
        } else if (qName != null) {
            msg += " name=\"" + escape(qName.getDisplayName()) + '"';
        }

        Iterator props = info.getProperties();
        
        while (props.hasNext()) {
            String prop = (String)props.next();
            Object val = info.getProperty(prop);
            if (prop.startsWith("{")) {
                // It's a QName in Clark notation: we'll strip off the namespace
                int rcurly = prop.indexOf('}');
                if (rcurly > 0) {
                    prop = prop.substring(rcurly+1);
                }
            }
            if (val != null && !prop.equals("name") && !prop.equals("expression")) {
                msg += ' ' + prop + "=\"" + escape(val.toString()) + '"';
            }
        }
        String newModule = escape(file);
        if (!newModule.equals(prevModule)) {
        	prevModule = newModule;
        	msg += " module=\"" + newModule + "\">";
        } else {
        	msg += ">";
        }

        logger.finest(msg);
        indent++;
    }

    /**
     * Escape a string for XML output (in an attribute delimited by double quotes).
     * This method also collapses whitespace (since the value may be an XPath expression that
     * was originally written over several lines).
     */

    public String escape(/*@Nullable*/ String in) {
        if (in==null) {
            return "";
        }
        CharSequence collapsed = Whitespace.collapseWhitespace(in);
        FastStringBuffer sb = new FastStringBuffer(collapsed.length() + 10);
        for (int i=0; i<collapsed.length(); i++) {
            char c = collapsed.charAt(i);
            if (c=='<') {
                sb.append("&lt;");
            } else if (c=='>') {
                sb.append("&gt;");
            } else if (c=='&') {
                sb.append("&amp;");
            } else if (c=='\"') {
                sb.append("&#34;");
            } else if (c=='\n') {
                sb.append("&#xA;");
            } else if (c=='\r') {
                sb.append("&#xD;");
            } else if (c=='\t') {
                sb.append("&#x9;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Called after an instruction of the stylesheet got processed
     */

    public void leave(/*@NotNull*/ InstructionInfo info) {
        StructuredQName infotype = info.getConstructType();
        String tag = tag(infotype);
        if (tag==null) {
            // this TraceListener ignores some events to reduce the volume of output
            return;
        }
        indent--;
      	logger.finest(AbstractTraceListener.spaces(indent) + "</" + tag + '>');
    }

    protected abstract String tag(StructuredQName construct);

    /**
    * Called when an item becomes the context item
    */

   public void startCurrentItem(Item item) {
       if (item instanceof NodeInfo) {
           NodeInfo curr = (NodeInfo) item;
           logger.finest(AbstractTraceListener.spaces(indent) + "<source node=\"" + Navigator.getPath(curr)
                   // + "\" line=\"" + curr.getLineNumber()
                   + "\" file=\"" + escape(StandardErrorListener.abbreviatePath(curr.getSystemId()))
                   + "\">");
       }
       indent++;
   }

    /**
     * Called after a node of the source tree got processed
     */

    public void endCurrentItem(Item item) {
        indent--;
        if (item instanceof NodeInfo) {
            NodeInfo curr = (NodeInfo) item;
            logger.finest(AbstractTraceListener.spaces(indent) + "</source><!-- " +
                    Navigator.getPath(curr) + " -->");
        }
    }

    /**
     * Get n spaces
     */

    private static String spaces(int n) {
        while (spaceBuffer.length() < n) {
            spaceBuffer.append(AbstractTraceListener.spaceBuffer);
        }
        return spaceBuffer.substring(0, n);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.