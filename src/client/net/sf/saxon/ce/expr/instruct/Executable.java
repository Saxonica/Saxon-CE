package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.CommentStripper;
import client.net.sf.saxon.ce.event.NamespaceReducer;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.event.StartTagBuffer;
import client.net.sf.saxon.ce.functions.FunctionLibraryList;
import client.net.sf.saxon.ce.om.CopyOptions;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.style.*;
import client.net.sf.saxon.ce.trans.*;
import client.net.sf.saxon.ce.tree.linked.DocumentImpl;
import client.net.sf.saxon.ce.tree.linked.LinkedTreeBuilder;
import client.net.sf.saxon.ce.value.DecimalValue;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A compiled stylesheet or a query in executable form.
 * Note that the original stylesheet tree is not retained.
 */

public class Executable {

    // the Configuration options
    private transient Configuration config;

    // definitions of strip/preserve space action
    private StripSpaceRules stripperRules;

    // definitions of keys, including keys created by the optimizer
    private KeyManager keyManager;

    // the map of slots used for global variables and params
    private int numberOfGlobals;

    // list of functions available in the static context
    private FunctionLibraryList functionLibrary;

    // a list of required parameters, identified by the structured QName of their names
    private HashSet<StructuredQName> requiredParams = null;

    // a boolean, true if the executable represents a stylesheet that uses xsl:result-document
    private boolean createsSecondaryResult = false;
    private int errorCount = 0;
    // definitions of decimal formats
    private DecimalFormatManager decimalFormatManager;
    // definitions of template rules (XSLT only)
    private RuleManager ruleManager = new RuleManager();
    // index of named templates.
    private HashMap<StructuredQName, Template> namedTemplateTable;


    /**
     * Create a new Executable (a collection of stylesheet modules and/or query modules)
     * @param config the Saxon Configuration
     */

    public Executable(Configuration config) {
        setConfiguration(config);
    }

    /**
     * Get the configuration
     * @return the Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the library containing all the in-scope functions in the static context
     *
     * @return the function libary
     */

    public FunctionLibraryList getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Set the library containing all the in-scope functions in the static context
     *
     * @param functionLibrary the function libary
     */

    public void setFunctionLibrary(FunctionLibraryList functionLibrary) {
        //System.err.println("***" + this + " setFunctionLib to " + functionLibrary);
        this.functionLibrary = functionLibrary;
    }

     /**
     * Set the rules determining which nodes are to be stripped from the tree
     *
     * @param rules a Mode object containing the whitespace stripping rules. A Mode
     *              is generally a collection of template rules, but it is reused here to represent
     *              a collection of stripping rules.
     */

    public void setStripperRules(StripSpaceRules rules) {
        stripperRules = rules;
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     *
     * @return a Mode object containing the whitespace stripping rules. A Mode
     *         is generally a collection of template rules, but it is reused here to represent
     *         a collection of stripping rules.
     */

    public StripSpaceRules getStripperRules() {
        if (stripperRules == null) {
            stripperRules = new StripSpaceRules();
        }
        return stripperRules;
    }

    /**
     * Get the KeyManager which handles key definitions
     *
     * @return the KeyManager containing the xsl:key definitions
     */

    public KeyManager getKeyManager() {
        if (keyManager == null) {
            keyManager = new KeyManager();
        }
        return keyManager;
    }

    /**
     * Allocate a slot number for a global variable
     * @return the allocated slot number
     */

    public int allocateGlobalVariableSlot() {
        return numberOfGlobals++;
    }

    /**
     * Allocate space in bindery for all the variables needed
     * @param bindery The bindery to be initialized
     */

    public void initializeBindery(Bindery bindery) {
        bindery.allocateGlobals(numberOfGlobals);
    }

    /**
     * Add a required parameter. Used in XSLT only.
     * @param qName the name of the required parameter
     */

    public void addRequiredParam(StructuredQName qName) {
        if (requiredParams == null) {
            requiredParams = new HashSet<StructuredQName>(5);
        }
        requiredParams.add(qName);
    }

    /**
     * Check that all required parameters have been supplied. Used in XSLT only.
     * @param params the set of parameters that have been supplied
     * @throws XPathException if there is a required parameter for which no value has been supplied
     */

    public void checkAllRequiredParamsArePresent(HashMap<StructuredQName, Sequence> params) throws XPathException {
        if (requiredParams == null) {
            return;
        }
        for (StructuredQName req : requiredParams) {
            if (params == null || !params.containsKey(req)) {
                throw new XPathException("No value supplied for required parameter " +
                        req.getDisplayName(), "XTDE0050");
            }
        }
    }


    /**
     * Set whether this executable represents a stylesheet that uses xsl:result-document
     * to create secondary output documents
     * @param flag true if the executable uses xsl:result-document
     */

    public void setCreatesSecondaryResult(boolean flag) {
        createsSecondaryResult = flag;
    }

    /**
     * Ask whether this executable represents a stylesheet that uses xsl:result-document
     * to create secondary output documents
     * @return true if the executable uses xsl:result-document
     */

    public boolean createsSecondaryResult() {
        return createsSecondaryResult;
    }

    /**
     * Make a Transformer from this Templates object.
     * @return the new Transformer (always a Controller)
     * @see client.net.sf.saxon.ce.Controller
     */

    public Controller newTransformer() {
        Controller c = new Controller(getConfiguration(), this);
        c.setPreparedStylesheet(this);
        return c;
    }

    /**
     * Set the configuration in which this stylesheet is compiled.
     * Intended for internal use.
     * @param config the configuration to be used.
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
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
     * @throws client.net.sf.saxon.ce.trans.XPathException if compilation of the stylesheet fails for any reason
     */

    public void prepare(DocumentInfo doc) throws XPathException {
        String message = "";
        try {
            setStylesheetDocument(loadStylesheetModule(doc));
        } catch (XPathException e) {
            if (errorCount == 0) {
                errorCount++;
            }
            message = e.getMessage() + ". ";
        }

        if (errorCount > 0) {
            throw new XPathException(
                    "Failed to compile stylesheet. " + message +
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
     * @throws client.net.sf.saxon.ce.trans.XPathException if XML parsing or tree
     *                        construction fails
     */
    public DocumentImpl loadStylesheetModule(DocumentInfo rawDoc)
            throws XPathException {

        StyleNodeFactory nodeFactory = new StyleNodeFactory(config);

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
     * @throws client.net.sf.saxon.ce.trans.XPathException if the document supplied
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
        if (top.getEffectiveVersion().compareTo(DecimalValue.TWO) != 0) {
            getConfiguration().issueWarning("Running an XSLT " + top.getEffectiveVersion() + " stylesheet with an XSLT 2.0 processor");
        }

        PrincipalStylesheetModule psm = new PrincipalStylesheetModule(top, 0);
        psm.setExecutable(this);
        psm.setVersion(top.getAttributeValue("", "version"));
        psm.createFunctionLibrary();
        setFunctionLibrary(psm.getFunctionLibrary());

        // Preprocess the stylesheet, performing validation and preparing template definitions

        top.setPrincipalStylesheetModule(psm);
        psm.preprocess();

        // Compile the stylesheet, retaining the resulting executable

        psm.compileStylesheet();
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
     * @throws client.net.sf.saxon.ce.trans.XPathException if the ErrorListener decides that the
     *                              error should be reported
     */

    public void reportError(XPathException err) throws XPathException {
        if (!err.hasBeenReported()) {
            errorCount++;
            config.getErrorListener().error(err);
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

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
