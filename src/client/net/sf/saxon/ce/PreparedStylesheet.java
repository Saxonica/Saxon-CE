package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.event.CommentStripper;
import client.net.sf.saxon.ce.event.NamespaceReducer;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.event.StartTagBuffer;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.om.CopyOptions;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.style.*;
import client.net.sf.saxon.ce.trans.CompilerInfo;
import client.net.sf.saxon.ce.trans.DecimalFormatManager;
import client.net.sf.saxon.ce.trans.RuleManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.DocumentImpl;
import client.net.sf.saxon.ce.tree.linked.LinkedTreeBuilder;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.value.DecimalValue;

import java.util.HashMap;

/**
 * This <B>PreparedStylesheet</B> class represents a Stylesheet that has been
 * prepared for execution (or "compiled").
 * <p/>
 * Note that the PreparedStylesheet object does not contain a reference to the source stylesheet
 * tree (rooted at an XSLStyleSheet object). This allows the source tree to be garbage-collected
 * when it is no longer required.
 */

public class PreparedStylesheet extends Executable {

    private CompilerInfo compilerInfo;
    private transient StyleNodeFactory nodeFactory;
    private int errorCount = 0;

    // definitions of decimal formats
    private DecimalFormatManager decimalFormatManager;

    // definitions of template rules (XSLT only)
    private RuleManager ruleManager;

    // index of named templates.
    private HashMap<StructuredQName, Template> namedTemplateTable;

    /**
     * Constructor
     * @param config The Configuration set up by the TransformerFactory
     * @param info   Compilation options
     */

    public PreparedStylesheet(Configuration config, CompilerInfo info) {
        super(config);
        nodeFactory = new StyleNodeFactory(config);
        RuleManager rm = new RuleManager();
        setRuleManager(rm);
        compilerInfo = info;

    }

    /**
     * Make a Transformer from this Templates object.
     * @return the new Transformer (always a Controller)
     * @see client.net.sf.saxon.ce.Controller
     */

    public Controller newTransformer() {
        Controller c = new Controller(getConfiguration(), this);
        c.setPreparedStylesheet(this);
        if (compilerInfo.getDefaultInitialTemplate() != null) {
            try {
                c.setInitialTemplate(compilerInfo.getDefaultInitialTemplate().getClarkName());
            } catch (XPathException err) {
                // ignore error if there is no template with this name
            }

        }
        if (compilerInfo.getDefaultInitialMode() != null) {
            c.setInitialMode(compilerInfo.getDefaultInitialMode().getClarkName());
        }
        return c;
    }

    /**
     * Set the configuration in which this stylesheet is compiled.
     * Intended for internal use.
     * @param config the configuration to be used.
     */

    public void setConfiguration(Configuration config) {
        super.setConfiguration(config);
        this.compilerInfo = config.getDefaultXsltCompilerInfo();
    }

    /**
     * Get the StyleNodeFactory in use. The StyleNodeFactory determines which subclass of StyleElement
     * to use for each element node in the stylesheet tree.
     * @return the StyleNodeFactory
     */

    public StyleNodeFactory getStyleNodeFactory() {
        return nodeFactory;
    }

    /**
     * Set the DecimalFormatManager which handles decimal-format definitions
     * @param dfm the DecimalFormatManager containing the named xsl:decimal-format definitions
     */

    public void setDecimalFormatManager(DecimalFormatManager dfm) {
        decimalFormatManager = dfm;
    }

    /**
     * Get the DecimalFormatManager which handles decimal-format definitions
     * @return the DecimalFormatManager containing the named xsl:decimal-format definitions
     */

    public DecimalFormatManager getDecimalFormatManager() {
        if (decimalFormatManager == null) {
            decimalFormatManager = new DecimalFormatManager();
        }
        return decimalFormatManager;
    }


    /**
     * Prepare a stylesheet from a Source document
     * @param doc the source document containing the stylesheet
     * @throws XPathException if compilation of the stylesheet fails for any reason
     */

    public void prepare(DocumentInfo doc) throws XPathException {
        try {
            setStylesheetDocument(loadStylesheetModule(doc));
        } catch (XPathException e) {
            // TODO: error handling
            if (errorCount == 0) {
                errorCount++;
            }
            throw e;
        }

        if (errorCount > 0) {
            throw new XPathException(
                    "Failed to compile stylesheet. " +
                            errorCount +
                            (errorCount == 1 ? " error " : " errors ") +
                            "detected.");
        }
    }

    /**
     * Build the tree representation of a stylesheet module
     * @param rawDoc the stylesheet module, typically as a DOM, before stripping of
     * whitespace, comments, and PIs
     * @return the root Document node of the tree containing the stylesheet
     *         module
     * @throws XPathException if XML parsing or tree
     *                        construction fails
     */
    public DocumentImpl loadStylesheetModule(DocumentInfo rawDoc)
            throws XPathException {

        StyleNodeFactory nodeFactory = getStyleNodeFactory();

        LinkedTreeBuilder styleBuilder = new LinkedTreeBuilder();
        PipelineConfiguration pipe = getConfiguration().makePipelineConfiguration();
        styleBuilder.setPipelineConfiguration(pipe);
        styleBuilder.setSystemId(rawDoc.getSystemId());
        styleBuilder.setNodeFactory(nodeFactory);

        StartTagBuffer startTagBuffer = new StartTagBuffer();
        NamespaceReducer nsReducer = new NamespaceReducer();
        nsReducer.setUnderlyingReceiver(startTagBuffer);

        UseWhenFilter useWhenFilter = new UseWhenFilter(startTagBuffer, nsReducer);
        useWhenFilter.setUnderlyingReceiver(styleBuilder);

        startTagBuffer.setUnderlyingReceiver(useWhenFilter);

        StylesheetStripper styleStripper = new StylesheetStripper();
        styleStripper.setUnderlyingReceiver(nsReducer);

        CommentStripper commentStripper = new CommentStripper();
        commentStripper.setUnderlyingReceiver(styleStripper);
        commentStripper.setPipelineConfiguration(pipe);

        // build the stylesheet document
        commentStripper.open();
        rawDoc.copy(commentStripper, CopyOptions.ALL_NAMESPACES);
        commentStripper.close();

        DocumentImpl doc = (DocumentImpl)styleBuilder.getCurrentRoot();
        styleBuilder.reset();

        return doc;
    }


    /**
     * Create a PreparedStylesheet from a supplied DocumentInfo
     * Note: the document must have been built using the StyleNodeFactory
     * @param doc the document containing the stylesheet module
     * @throws XPathException if the document supplied
     *                        is not a stylesheet
     */

    protected void setStylesheetDocument(DocumentImpl doc)
            throws XPathException {

        DocumentImpl styleDoc = doc;

        // If top-level node is a literal result element, stitch it into a skeleton stylesheet

        StyleElement topnode = (StyleElement)styleDoc.getDocumentElement();
        if (topnode == null) {
        	throw new XPathException("Failed to parse stylesheet");
        }
        if (topnode instanceof LiteralResultElement) {
            styleDoc = ((LiteralResultElement)topnode).makeStylesheet(this);
        }

        if (!(styleDoc.getDocumentElement() instanceof XSLStylesheet)) {
            throw new XPathException(
                    "Outermost element of stylesheet is not xsl:stylesheet or xsl:transform or literal result element");
        }

        XSLStylesheet top = (XSLStylesheet)styleDoc.getDocumentElement();
        if (compilerInfo.isVersionWarning() &&
                top.getEffectiveVersion().compareTo(DecimalValue.TWO) != 0) {

            getConfiguration().issueWarning("Running an XSLT " + top.getEffectiveVersion() + " stylesheet with an XSLT 2.0 processor");

        }

        PrincipalStylesheetModule psm = new PrincipalStylesheetModule(top, 0);
        psm.setPreparedStylesheet(this);
        psm.setVersion(Navigator.getAttributeValue(top, "", "version"));
        psm.createFunctionLibrary();
        setFunctionLibrary(psm.getFunctionLibrary());

        // Preprocess the stylesheet, performing validation and preparing template definitions

        top.setPrincipalStylesheetModule(psm);
        psm.preprocess();

        // Compile the stylesheet, retaining the resulting executable

        psm.compileStylesheet();
    }

    /**
     * Get the associated executable
     * @return the Executable for this stylesheet
     */

    public Executable getExecutable() {
        return this;
    }

    /**
     * Set the RuleManager that handles template rules
     *
     * @param rm the RuleManager containing details of all the template rules
     */

    public void setRuleManager(RuleManager rm) {
        ruleManager = rm;
    }

    /**
     * Get the RuleManager which handles template rules
     *
     * @return the RuleManager registered with setRuleManager
     */

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /**
     * Get the named template with a given name.
     *
     * @param qName The template name
     * @return The template (of highest import precedence) with this name if there is one;
     *         null if none is found.
     */

    public Template getNamedTemplate(StructuredQName qName) {
        if (namedTemplateTable == null) {
            return null;
        }
        return namedTemplateTable.get(qName);
    }

    /**
     * Register the named template with a given name
     * @param templateName the name of a named XSLT template
     * @param template the template
     */

    public void putNamedTemplate(StructuredQName templateName, Template template) {
        if (namedTemplateTable == null) {
            namedTemplateTable = new HashMap(32);
        }
        namedTemplateTable.put(templateName, template);
    }

    /**
     * Report a compile time error. This calls the errorListener to output details
     * of the error, and increments an error count.
     * @param err the exception containing details of the error
     * @throws XPathException if the ErrorListener decides that the
     *                              error should be reported
     */

    public void reportError(XPathException err) throws XPathException {
        if (!err.hasBeenReported()) {
            errorCount++;
            compilerInfo.getErrorListener().error(err);
            err.setHasBeenReported(true);
        }

    }

    /**
     * Get the number of errors reported so far
     * @return the number of errors reported
     */

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Report a compile time warning. This calls the errorListener to output details
     * of the warning.
     * @param err an exception holding details of the warning condition to be
     *            reported
     */

    public void reportWarning(XPathException err) {
        getConfiguration().issueWarning(err.getMessage());
    }

    /**
     * Get the CompilerInfo containing details of XSLT compilation options
     * @return the CompilerInfo containing compilation options
     * @since 9.2
     */

    public CompilerInfo getCompilerInfo() {
        return compilerInfo;
    }

//
//    /**
//     * Get the stylesheet specification(s) associated
//     * via the xml-stylesheet processing instruction (see
//     * http://www.w3.org/TR/xml-stylesheet/) with the document
//     * document specified in the source parameter, and that match
//     * the given criteria.  Note that it is possible to return several
//     * stylesheets, in which case they are applied as if they were
//     * a list of imports or cascades.
//     * @param config  The Saxon Configuration
//     * @param source  The XML source document.
//     * @param media   The media attribute to be matched.  May be null, in which
//     *                case the prefered templates will be used (i.e. alternate = no).
//     * @param title   The value of the title attribute to match.  May be null.
//     * @param charset The value of the charset attribute to match.  May be null.
//     * @return A Source object suitable for passing to the TransformerFactory.
//     * @throws TransformerConfigurationException
//     *          if any problems occur
//     */
//
//
//    public static Source getAssociatedStylesheet(
//            Configuration config, Source source, String media, String title, String charset)
//            throws TransformerConfigurationException {
//        PIGrabber grabber = new PIGrabber();
//        grabber.setFactory(config);
//        grabber.setCriteria(media, title, charset);
//        grabber.setBaseURI(source.getSystemId());
//        grabber.setURIResolver(config.getURIResolver());
//        grabber.setPipelineConfiguration(config.makePipelineConfiguration());
//
//        try {
//            Sender.send(source, grabber, null);
//            // this parse will be aborted when the first start tag is found
//        } catch (XPathException err) {
//            if (grabber.isTerminated()) {
//                // do nothing
//            } else {
//                throw new TransformerConfigurationException(
//                        "Failed while looking for xml-stylesheet PI", err);
//            }
//        }
//
//        try {
//            Source[] sources = grabber.getAssociatedStylesheets();
//            if (sources == null) {
//                throw new TransformerConfigurationException(
//                        "No matching <?xml-stylesheet?> processing instruction found");
//            }
//            return compositeStylesheet(config, source.getSystemId(), sources);
//        } catch (TransformerException err) {
//            if (err instanceof TransformerConfigurationException) {
//                throw (TransformerConfigurationException)err;
//            } else {
//                throw new TransformerConfigurationException(err);
//            }
//        }
//    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
