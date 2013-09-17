package client.net.sf.saxon.ce;

import client.net.sf.saxon.ce.dom.HTMLWriter;
import client.net.sf.saxon.ce.event.*;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.XPathContextMajor;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.functions.Component;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.lib.ErrorListener;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StandardErrorListener;
import client.net.sf.saxon.ce.lib.TraceListener;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.Mode;
import client.net.sf.saxon.ce.trans.RuleManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.trans.update.PendingUpdateList;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.linked.LinkedTreeBuilder;
import client.net.sf.saxon.ce.value.DateTimeValue;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.user.client.Event;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

/**
 * The Controller is equivalent to Saxon-HE's implementation of the same name, and represents
 * an executing instance of a transformation or query. Multiple concurrent executions of
 * the same transformation or query will use different Controller instances. This class is
 * therefore not thread-safe.
 * <p>
 * The Controller is serially reusable, when one transformation or query
 * is finished, it can be used to run another. However, there is no advantage in doing this
 * rather than allocating a new Controller each time. An inert version of the controller can
 * be used to simply hold state for the benefit of the JavaScript API, controller settings
 * can then be copied to a 'live' Controller instance using importControllerSettings()
 * <p>
 * A dummy Controller is created by the JavaScript API for holding settings.
 * <p>
 * The Controller holds those parts of the dynamic context that do not vary during the course
 * of a transformation or query, or that do not change once their value has been computed.
 * This also includes those parts of the static context that are required at run-time.
 * <p>
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public class Controller {


    public Controller() {}

    private Configuration config;
    private Item initialContextItem;
    private Item contextForGlobalVariables;
    private Bindery bindery;                // holds values of global variables
    private RuleManager ruleManager;
    private HashMap<StructuredQName, Sequence> parameters;
    private String principalResultURI;
    private ErrorListener errorListener;
    private Executable executable;
    private Template initialTemplate = null;
    private HashSet<DocumentURI> allOutputDestinations;
    private HashMap<DocumentURI, Node> resultDocumentPool;
    private SequenceOutputter reusableSequenceOutputter = null;
    private HashMap<String, Object> userDataTable = new HashMap<String, Object>(20);
    private DateTimeValue currentDateTime;
    private boolean dateTimePreset = false;
    private StructuredQName initialMode = null;
    private NodeInfo lastRememberedNode = null;
    private int lastRememberedNumber = -1;
    private boolean inUse = false;
    private boolean stripSourceTrees = true;
    private PendingUpdateList pendingUpdateList;
    private String initialTemplateName = null;
    private boolean isInert;
    private Node targetNode;
    private APIcommand commandType;
    private static ArrayList<Xslt20ProcessorImpl> eventProcessors= null;
    private static ArrayList<Xslt20ProcessorImpl> nonDomEventProcessors= null;
    private HTMLWriter openHTMLWriter = null;
    private Node principalOutputNode = null;
    private NodeInfo sourceNode = null;

    /**
     * Create a Controller and initialise variables. Note: XSLT applications should
     * create the Controller by using the JAXP newTransformer() method, or in S9API
     * by using XsltExecutable.load()
     *
     * @param proc The processor
     */
    
    public static void addEventProcessor(Xslt20ProcessorImpl proc) {
    	eventProcessors = addProcessor(eventProcessors, proc);
    }
    
    public static void relayEvent(Node node, Event event) {
    	if (eventProcessors != null){
    		for (Xslt20ProcessorImpl p: eventProcessors){
    			if (p != null) {
    				p.bubbleApplyTemplates(node, event);
    			}
    		}
    	}
    }
    
    public static ArrayList<Xslt20ProcessorImpl> addProcessor(ArrayList<Xslt20ProcessorImpl> list, Xslt20ProcessorImpl proc){
    	if (list == null) {
    		list = new ArrayList<Xslt20ProcessorImpl>();
    	}
    	list.add(proc);
    	return list;
    }
    
    public static void addNonDomEventProcessor(Xslt20ProcessorImpl proc) {
    	nonDomEventProcessors = addProcessor(nonDomEventProcessors, proc);
    }
    
    public static void relayNonDomEvent(String name, JavaScriptObject target, JavaScriptObject event) {
    	if (nonDomEventProcessors != null){
    		for (Xslt20ProcessorImpl p: nonDomEventProcessors){
    			if (p != null) {
    				StructuredQName sqn = new StructuredQName("", NamespaceConstant.IXSL, name);
    				// context was set to host page
    				p.applyEventTemplates(sqn.getClarkName(), null, event, target);
    			}
    		}
    	}
    }
    
    public Controller(Configuration config, boolean isInert) {
    	this.isInert = isInert;
        this.config = config;
        // create a dummy executable
        executable = new Executable(config);
        reset();  	
    }
    
    public Controller(Configuration config) {
    	isInert = false;
        this.config = config;
        // create a dummy executable
        executable = new Executable(config);
        reset();
    }
    
    public enum APIcommand {
    	UPDATE_HTML, TRANSFORM_TO_DOCUMENT, TRANSFORM_TO_FRAGMENT, TRANSFORM_TO_HTML_FRAGMENT, NONE
    }

    /**
     * Create a Controller and initialise variables.
     *
     * @param config The Configuration used by this Controller
     * @param executable The executable used by this Controller
     */

    public Controller(Configuration config, Executable executable) {
    	isInert = false;
        this.config = config;
        this.executable = executable;
        this.errorListener = config.getErrorListener();
        reset();
    }

    /**
     * <p>Reset this <code>Transformer</code> to its original configuration.</p>
     * <p/>
     * <p><code>Transformer</code> is reset to the same state as when it was created with
     * {@link javax.xml.transform.TransformerFactory#newTransformer()},
     * {@link javax.xml.transform.TransformerFactory#newTransformer(javax.xml.transform.Source source)} or
     * {@link javax.xml.transform.Templates#newTransformer()}.
     * <code>reset()</code> is designed to allow the reuse of existing <code>Transformer</code>s
     * thus saving resources associated with the creation of new <code>Transformer</code>s.</p>
     * <p>
     * <p>The reset <code>Transformer</code> is not guaranteed to have the same {@link javax.xml.transform.URIResolver}
     * or {@link javax.xml.transform.ErrorListener} <code>Object</code>s, e.g. {@link Object#equals(Object obj)}.
     * It is guaranteed to have a functionally equal <code>URIResolver</code>
     * and <code>ErrorListener</code>.</p>
     *
     * @since 1.5
     */

    public void reset() {
        bindery = new Bindery();
        if (errorListener instanceof StandardErrorListener) {
            // if using a standard error listener, make a fresh one
            // for each transformation, because it is stateful - and also because the
            // host language is now known (a Configuration can serve multiple host languages)
            PrintStream ps = ((StandardErrorListener)errorListener).getErrorOutput();
            errorListener = ((StandardErrorListener)errorListener).makeAnother();
            ((StandardErrorListener)errorListener).setErrorOutput(ps);
        }

        contextForGlobalVariables = null;
        parameters = null;
        currentDateTime = null;
        dateTimePreset = false;
        initialContextItem = null;
        initialMode = null;
        initialTemplate = null;
        initialTemplateName = null;
        clearPerTransformationData();
        pendingUpdateList = new PendingUpdateList(config);
        targetNode = null;
        commandType = APIcommand.NONE;
        resultDocumentPool = null;
        openHTMLWriter = null;
    }
    
    public void importControllerSettings(Controller lc) throws XPathException {
    	this.setBaseOutputURI(lc.getBaseOutputURI());
    	this.setInitialMode(lc.getInitialMode());
    	this.setInitialTemplate(lc.getInitialTemplateName());
    	this.setParameters(lc.getParameters()); // shared reference only
    	this.setBaseOutputURI(lc.getBaseOutputURI());
    	this.setTargetNode(lc.getTargetNode());
    	this.setApiCommand(lc.getApiCommand());
    	this.setSourceNode(lc.getSourceNode());
    }

    /**
     * Reset variables that need to be reset for each transformation if the controller
     * is serially reused
     */

    private void clearPerTransformationData() {
        //userDataTable = new HashMap<String, Object>(20);
        //principalResult = null;
        //principalResultURI = null;
        allOutputDestinations = null;
        resultDocumentPool = null;
        //thereHasBeenAnExplicitResultDocument = false;
        lastRememberedNode = null;
        lastRememberedNumber = -1;
        openHTMLWriter = null;
    }

    /**
     * Get the Configuration associated with this Controller. The Configuration holds
     * settings that potentially apply globally to many different queries and transformations.
     * @return the Configuration object
     * @since 8.4
     */
    public Configuration getConfiguration() {
        return config;
    }
    
    public void setTargetNode(Node target) {
    	targetNode = target;
    }
    
    public Node getTargetNode() {
    	return targetNode;
    }
    
    public void setSourceNode(NodeInfo source) {
    	sourceNode = source;
    }
    
    public NodeInfo getSourceNode() {
    	return sourceNode;
    }
    
    
    public void setApiCommand(APIcommand command) {
    	commandType = command;
    }
    
    public APIcommand getApiCommand() {
    	return commandType;
    }
        
    /**
     * Set the initial mode for the transformation.
     * <p>
     * XSLT 2.0 allows a transformation to be started in a mode other than the default mode.
     * The transformation then starts by looking for the template rule in this mode that best
     * matches the initial context node.
     * <p>
     * This method may eventually be superseded by a standard JAXP method.
     *
     * @param expandedModeName the name of the initial mode.  The mode is
     *     supplied as an expanded QName, that is "localname" if there is no
     *     namespace, or "{uri}localname" otherwise. If the value is null or zero-length,
     *     the initial mode is reset to the unnamed default mode.
     * @since 8.4
     */

    public void setInitialMode(String expandedModeName) {
        if (expandedModeName == null || expandedModeName.length() == 0) {
            initialMode = null;
        } else {
            initialMode = StructuredQName.fromClarkName(expandedModeName);
        }
    }

    /**
     * Get the initial mode for the transformation
     * @return the initial mode, as a name in Clark format
     */

    public String getInitialMode() {
    	if (initialMode == null) {
    		return null;
    	} else {
            return initialMode.getClarkName();
    	}
    }
    
    public HashMap<StructuredQName, Sequence> getParameters() {
    	return parameters;
    }
    
    public void setParameters(HashMap<StructuredQName, Sequence> params) {
    	parameters = params;
    }
    
    public String getInitialTemplateName(){
    	return initialTemplateName;
    }


    /**
     * Set the base output URI.
     *
     * <p>This defaults to the system ID of the Result object for the principal output
     * of the transformation if this is known; if it is not known, it defaults
     * to the current directory.</p>
     *
     * <p> The base output URI is used for resolving relative URIs in the <code>href</code> attribute
     * of the <code>xsl:result-document</code> instruction.</p>

     *
     * @param uri the base output URI
     * @since 8.4
     */

    public void setBaseOutputURI(String uri) {
        principalResultURI = uri;
    }

    /**
     * Get the base output URI.
     *
     * <p>This returns the value set using the {@link #setBaseOutputURI} method. If no value has been set
     * explicitly, then the method returns null if called before the transformation, or the computed
     * default base output URI if called after the transformation.</p>
     *
     * <p> The base output URI is used for resolving relative URIs in the <code>href</code> attribute
     * of the <code>xsl:result-document</code> instruction.</p>
     *
     * @return the base output URI
     * @since 8.4
     */

    public String getBaseOutputURI() {
        return principalResultURI;
    }

    /**
     * Check that an output destination has not been used before, optionally adding
     * this URI to the set of URIs that have been used.
     * @param uri the URI to be used as the output destination
     * @return true if the URI is available for use; false if it has already been used.
     * <p>
     * This method is intended for internal use only.
     */

    public boolean checkUniqueOutputDestination(DocumentURI uri) {
        if (uri == null) {
            return true;    // happens when writing say to an anonymous StringWriter
        }
        if (allOutputDestinations == null) {
            allOutputDestinations = new HashSet<DocumentURI>(20);
        }

        return !(allOutputDestinations.contains(uri));
    }

    /**
     * Add a URI to the set of output destinations that cannot be written to, either because
     * they have already been written to, or because they have been read
     * @param uri A URI that is not available as an output destination
     */

    public void addUnavailableOutputDestination(DocumentURI uri) {
        if (allOutputDestinations == null) {
            allOutputDestinations = new HashSet<DocumentURI>(20);
        }
        allOutputDestinations.add(uri);
    }
    
    public void addToResultDocumentPool(DocumentURI uri, Node doc) {
    	addUnavailableOutputDestination(uri);
        if (resultDocumentPool == null) {
        	resultDocumentPool = new HashMap<DocumentURI,Node>(20);
        }
        resultDocumentPool.put(uri, doc);
    }
    
    public int getResultDocumentCount() {
    	return (resultDocumentPool == null)? 0 :resultDocumentPool.size();
    }
    
    public void importResults(Controller ctrl){
    	this.resultDocumentPool = ctrl.resultDocumentPool;
    	this.principalOutputNode = ctrl.principalOutputNode; // bugfix
    }
    
    public Node getResultDocument(String uri) {
    	if (uri == null || uri.length() == 0) {
    		return principalOutputNode;
    	}
    	DocumentURI docURI = new DocumentURI(uri);
        if (resultDocumentPool == null) {
        	return null;
        } else if (resultDocumentPool.containsKey(docURI)) {
        	return resultDocumentPool.get(docURI);
        } else {
        	return null;
        }
    }
    
    private JavaScriptObject getJsResultURIset() {   	
        	JavaScriptObject uriArray = IXSLFunction.jsArray(resultDocumentPool.size());
        	int poolSize = resultDocumentPool.size();
        	DocumentURI[] uris = new DocumentURI[poolSize];
            uris = resultDocumentPool.keySet().toArray(uris);
            poolSize--;
            for (int i = 0; i <= poolSize; i++) {
            	IXSLFunction.jsSetArrayItem(uriArray, poolSize - i, uris[i].toString());
            }
            return uriArray;
    }
    
    /**
     * For JavaScriptAPI
     * @return a JavaScript object with a property for each result document.
     * The property name is the URI and the property value the document object
     */
    public JavaScriptObject getResultDocURIArray() {
        if (resultDocumentPool == null) {
        	return IXSLFunction.jsArray(0);
        } else {
        	return getJsResultURIset();
        }
    }
    
    /**
     * Returns a JavaScript object with a property for each result document.
     * The property name is the URI and the property value the document object
     * @param keys A JavaScript array of result document keys (URIs)
     * @return A JavaScript object with similar functionality to a HashMap.
     */
    
   
    private static native JavaScriptObject createArray(JavaScriptObject keys) /*-{
	    var obj = new Array(keys.length);
	    for (var i = 0; i < keys.length; i++) {
	        obj[i] = keys[i];
	    }
	    return obj;
}-*/;
    

    /**
     * Check whether an XSLT implicit result tree can be written. This is allowed only if no xsl:result-document
     * has been written for the principal output URI
     */

    public void checkImplicitResultTree() throws XPathException {
        if (principalResultURI != null && !checkUniqueOutputDestination(new DocumentURI(principalResultURI))) {
            XPathException err = new XPathException(
                    "Cannot write an implicit result document if an explicit result document has been written to the same URI: " +
                    principalResultURI);
            err.setErrorCode("XTDE1490");
            throw err;
        }
    }

    /**
     * Test whether an explicit result tree has been written using xsl:result-document
     * @return true if the transformation has evaluated an xsl:result-document instruction
     */

    public boolean hasThereBeenAnExplicitResultDocument() {
    	return (resultDocumentPool != null && resultDocumentPool.size() > 0);
    }

    /**
     * Allocate a SequenceOutputter for a new output destination. Reuse the existing one
     * if it is available for reuse (this is designed to ensure that the TinyTree structure
     * is also reused, creating a forest of trees all sharing the same data structure)
     * @param size the estimated size of the output sequence
     * @return SequenceOutputter the allocated SequenceOutputter
     */

    public SequenceOutputter allocateSequenceOutputter(int size) {
        if (reusableSequenceOutputter != null) {
            SequenceOutputter out = reusableSequenceOutputter;
            out.setSystemId(null);    // Added 10.8.2009 - seems right, but doesn't solve EvaluateNodeTest problem
            reusableSequenceOutputter = null;
            return out;
        } else {
            return new SequenceOutputter(this, size);
        }
    }

    /**
     * Accept a SequenceOutputter that is now available for reuse
     * @param out the SequenceOutputter that is available for reuse
     */

    public void reuseSequenceOutputter(SequenceOutputter out) {
        reusableSequenceOutputter = out;
    }

    /**
     * Get the pending update list
     * @return the pending update list
     */

    public PendingUpdateList getPendingUpdateList() {
        return pendingUpdateList;
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Set the initial named template to be used as the entry point.
     * <p>
     * XSLT 2.0 allows a transformation to start by executing a named template, rather than
     * by matching an initial context node in a source document. This method may eventually
     * be superseded by a standard JAXP method once JAXP supports XSLT 2.0.
     * <p>
     * Note that any parameters supplied using {@link #setParameter} are used as the values
     * of global stylesheet parameters. There is no way to supply values for local parameters
     * of the initial template.
     *
     * @param expandedName The expanded name of the template in {uri}local format, or null
     * or a zero-length string to indicate that there should be no initial template.
     * @throws XPathException if there is no named template with this name
     * @since 8.4
     */

    public void setInitialTemplate(String expandedName) throws XPathException {
        if (expandedName == null || expandedName.length() == 0) {
            initialTemplate = null;
            initialTemplateName = null;
            return;
        }
        initialTemplateName = expandedName;
        if (isInert) {
        	return;
        }
        StructuredQName qName = StructuredQName.fromClarkName(expandedName);
        Template t = getExecutable().getNamedTemplate(qName);
        if (t == null) {
            XPathException err = new XPathException("The requested initial template, with expanded name "
                    + expandedName + ", does not exist", "XTDE0040");
            reportFatalError(err);
            throw err;
        } else if (t.hasRequiredParams()) {
            XPathException err = new XPathException("The named template "
                    + expandedName
                    + " has required parameters, so cannot be used as the entry point", "XTDE0060");
            reportFatalError(err);
            throw err;
        } else {
            initialTemplate = t;
        }
    }     

    /**
     * Get the initial template
     * @return the name of the initial template, as an expanded name in Clark format if set, or null otherwise
     * @since 8.7
     */

    public String getInitialTemplate() {
        if (initialTemplate == null) {
            return null;
        } else {
            return initialTemplate.getTemplateName().getClarkName();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Make a PipelineConfiguration based on the properties of this Controller.
     * <p>
     * This interface is intended primarily for internal use, although it may be necessary
     * for applications to call it directly if they construct pull or push pipelines
     * @return a newly constructed PipelineConfiguration holding a reference to this
     * Controller as well as other configuration information.
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(getConfiguration());
        pipe.setErrorListener(getErrorListener());
        pipe.setController(this);
        return pipe;
    }

	/**
	 * Set the error listener.
	 *
	 * @param listener the ErrorListener to be used
	 */

	public void setErrorListener(ErrorListener listener) {
		errorListener = listener;
	}

	/**
	 * Get the error listener.
	 *
	 * @return the ErrorListener in use
	 */

	public ErrorListener getErrorListener() {
		return errorListener;
	}

    /**
     * Report a fatal error
     * @param err the error to be reported
     */

    public void reportFatalError(XPathException err) {
        if (!err.hasBeenReported()) {
            getErrorListener().error(err);
            err.setHasBeenReported(true);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    // Methods for managing the various runtime control objects
    /////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Get the Executable object.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the Executable (which represents the compiled stylesheet)
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Get the document pool. This is used only for source documents, not for stylesheet modules.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the source document pool
     */

    public DocumentPool getDocumentPool() {
        return getConfiguration().getDocumentPool();
    }

    /**
     * Set the initial context item, when running XSLT invoked with a named template.
     * <p/>
     * When a transformation is invoked using the {@link #transform} method, the
     * initial context node is set automatically. This method is useful in XQuery,
     * to define an initial context node for evaluating global variables, and also
     * in XSLT 2.0, when the transformation is started by invoking a named template.
     *
     * <p>When an initial context item is set, it also becomes the context item used for
     * evaluating global variables. The two context items can only be different when the
     * {@link #transform} method is used to transform a document starting at a node other
     * than the root.</p>
     *
     * <p>In XQuery, the two context items are always
     * the same; in XSLT, the context node for evaluating global variables is the root of the
     * tree containing the initial context item.</p>
     *
     * @param item The initial context item. 
     * @since 8.7
     */

    public void setInitialContextItem(Item item) {
        initialContextItem = item;
        contextForGlobalVariables = item;
    }

    /**
     * Get the current bindery.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the Bindery (in which values of all variables are held)
     */

    public Bindery getBindery() {
        return bindery;
    }

    /**
     * Get the initial context item. This returns the item (often a document node)
     * previously supplied to the {@link #setInitialContextItem} method, or the
     * initial context node set implicitly using methods such as {@link #transform}.
     * @return the initial context item. Note that in XSLT this must be a node, but in
     * XQuery it may also be an atomic value.
     * @since 8.7
     */

    public Item getInitialContextItem() {
        return initialContextItem;
    }

    /**
     * Get the item used as the context for evaluating global variables. In XQuery this
     * is the same as the initial context item; in XSLT it is the root of the tree containing
     * the initial context node.
     * @return the context item for evaluating global variables, or null if there is none
     * @since 8.7
     */

    public Item getContextForGlobalVariables() {
        return contextForGlobalVariables;
        // See bug 5224, which points out that the rules for XQuery 1.0 weren't clearly defined
    }

    /**
     * Make a builder for the selected tree model.
     *
     * @return an instance of the Builder for the chosen tree model
     * @since 8.4
     */

    public Builder makeBuilder() {
        return new LinkedTreeBuilder();
    }

    /**
     * Say whether the transformation should perform whitespace stripping as defined
     * by the xsl:strip-space and xsl:preserve-space declarations in the stylesheet
     * in the case where a source tree is supplied to the transformation as a tree
     * (typically a DOMSource, or a Saxon NodeInfo).
     * The default is true. It is legitimate to suppress whitespace
     * stripping if the client knows that all unnecessary whitespace has already been removed
     * from the tree before it is processed. Note that this option applies to all source
     * documents for which whitespace-stripping is normally applied, that is, both the
     * principal source documents, and documents read using the doc(), document(), and
     * collection() functions. It does not apply to source documents that are supplied
     * in the form of a SAXSource or StreamSource, for which whitespace is stripped
     * during the process of tree construction.
     * <p>Generally, stripping whitespace speeds up the transformation if it is done
     * while building the source tree, but slows it down if it is applied to a tree that
     * has already been built. So if the same source tree is used as input to a number
     * of transformations, it is better to strip the whitespace once at the time of
     * tree construction, rather than doing it on-the-fly during each transformation.</p>
     * @param strip true if whitespace is to be stripped from supplied source trees
     * as defined by xsl:strip-space; false to suppress whitespace stripping
     * @since 9.3
     */

    public void setStripSourceTrees(boolean strip) {
        stripSourceTrees = strip;
    }

    /**
     * Ask whether the transformation will perform whitespace stripping for supplied source trees as defined
     * by the xsl:strip-space and xsl:preserve-space declarations in the stylesheet.
     * @return true unless whitespace stripping has been suppressed using
     * {@link #setStripSourceTrees(boolean)}.
     * @since 9.3
     */

    public boolean isStripSourceTree() {
        return stripSourceTrees;
    }

    /**
     * Add a document to the document pool, and check that it is suitable for use in this query or
     * transformation. This check rejects the document if document has been validated (and thus carries
     * type annotations) but the query or transformation is not schema-aware.
     * <p>
     * This method is intended for internal use only.
     *
     * @param doc the root node of the document to be added. Must not be null.
     * @param uri the document-URI property of this document. If non-null, the document is registered
     * in the document pool with this as its document URI.
     */
    public void registerDocument(DocumentInfo doc, DocumentURI uri) throws XPathException {
        if (doc == null) {
            throw new NullPointerException("null");
        }
        if (uri != null) {
            getConfiguration().getDocumentPool().add(doc, uri);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Methods for registering and retrieving handlers for template rules
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Set the RuleManager, used to manage template rules for each mode.
     * <p>
     * This method is intended for internal use only.
     *
     * @param r the Rule Manager
     */
    public void setRuleManager(RuleManager r) {
        ruleManager = r;
    }

    /**
     * Get the Rule Manager.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the Rule Manager, used to hold details of template rules for
     *     all modes
     */
    public RuleManager getRuleManager() {
        return ruleManager;
    }


    /**
     * Associate this Controller with a compiled stylesheet.
     * <p>
     * This method is intended for internal use only.
     *
     * @param sheet the compiled stylesheet
     */

    public void setPreparedStylesheet(Executable sheet) {
        executable = sheet;
    }
    
    public Executable getPreparedStylesheet(){
    	return executable;
    }

    /**
     * Associate this Controller with an Executable. This method is used by the XQuery
     * processor. The Executable object is overkill in this case - the only thing it
     * currently holds are copies of the collation table.
     * <p>
     * This method is intended for internal use only
     * @param exec the Executable
     */

    public void setExecutable(Executable exec) {
        executable = exec;
    }

    /**
     * Initialize the controller ready for a new transformation. This method should not normally be called by
     * users (it is done automatically when transform() is invoked). However, it is available as a low-level API
     * especially for use with XQuery.
     */

    private void initializeController() throws XPathException {
        if (executable != null) {
            setRuleManager(executable.getRuleManager());
        }
        //setDecimalFormatManager(executable.getDecimalFormatManager());

        // get a new bindery, to clear out any variables from previous runs

        bindery = new Bindery();
        executable.initializeBindery(bindery);

        // if parameters were supplied, set them up

        defineGlobalParameters();
    }

    /**
     * Register the global parameters of the transformation or query. This should be called after a sequence
     * of calls on {@link #setParameter}. It checks that all required parameters have been supplied, and places
     * the values of the parameters in the Bindery to make them available for use during the query or
     * transformation.
     * <p>
     * This method is intended for internal use only
     */

    public void defineGlobalParameters() throws XPathException {
        executable.checkAllRequiredParamsArePresent(parameters);
        bindery.defineGlobalParameters(parameters);
    }


    /////////////////////////////////////////////////////////////////////////
    // Allow user data to be associated with nodes on a tree
    /////////////////////////////////////////////////////////////////////////

    /**
     * Get user data associated with a key. To retrieve user data, two objects are required:
     * an arbitrary object that may be regarded as the container of the data (originally, and
     * typically still, a node in a tree), and a name. The name serves to distingush data objects
     * associated with the same node by different client applications.
     * <p>
     * This method is intended primarily for internal use, though it may also be
     * used by advanced applications.
     *
     * @param key an object acting as a key for this user data value. This must be equal
     * (in the sense of the equals() method) to the key supplied when the data value was
     * registered using {@link #setUserData}.
     * @param name the name of the required property
     * @return the value of the required property
     */

    public Object getUserData(Object key, String name) {
        String keyValue = key.hashCode() + " " + name;
        // System.err.println("getUserData " + name + " on object returning " + userDataTable.get(key));
        return userDataTable.get(keyValue);
    }

    /**
     * Set user data associated with a key. To store user data, two objects are required:
     * an arbitrary object that may be regarded as the container of the data (originally, and
     * typically still, a node in a tree), and a name. The name serves to distingush data objects
     * associated with the same node by different client applications.
     * <p>
     * This method is intended primarily for internal use, though it may also be
     * used by advanced applications.
     *
     * @param key an object acting as a key for this user data value. This can be any object, for example
     * a node or a string. If data for the given object and name already exists, it is overwritten.
     * @param name the name of the required property
     * @param data the value of the required property. If null is supplied, any existing entry
     * for the key is removed.
     */

    public void setUserData(Object key, String name, Object data)  {
        // System.err.println("setUserData " + name + " on object to " + data);
        String keyVal = key.hashCode() + " " + name;
        if (data==null) {
            userDataTable.remove(keyVal);
        } else {
            userDataTable.put(keyVal, data);
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // implement the javax.xml.transform.Transformer methods
    /////////////////////////////////////////////////////////////////////////

    /**
     * Perform a transformation from a Source document to a Result document.
     *
     * @exception XPathException if the transformation fails. As a
     *     special case, the method throws a TerminationException (a subclass
     *     of XPathException) if the transformation was terminated using
     *      xsl:message terminate="yes".
     * @param source The input for the source tree. May be null if and only if an
     * initial template has been supplied.
     * @return The root of the result tree.
     */

    public Node transform(NodeInfo source, com.google.gwt.dom.client.Node target) throws Exception {
        if (inUse) {
            throw new IllegalStateException(
                    "The Transformer is being used recursively or concurrently. This is not permitted.");
        }
        clearPerTransformationData();
        if (executable==null) {
            throw new XPathException("Stylesheet has not been prepared");
        }

        if (!dateTimePreset) {
            currentDateTime = null;     // reset at start of each transformation
        }

        // no longer used for expiry check - just XSLT context
        getCurrentDateTime();
        
        if (LogConfiguration.loggingIsEnabled()) {
        	LogController.openTraceListener();
        }
        boolean success = false;

        try {
            if (source == null) {
                if (initialTemplate == null) {
                    throw new XPathException("Either a source document or an initial template must be specified");
                }

            } else {

                Mode mode = executable.getRuleManager().getMode(initialMode, false);
                if (mode == null || (initialMode != null && mode.isEmpty())) {
                    throw new XPathException("Requested initial mode " +
                            (initialMode == null ? "" : initialMode.getDisplayName()) +
                            " does not exist", "XTDE0045");
                }

                if (source.getSystemId() != null) {
                    registerDocument(source.getDocumentRoot(), new DocumentURI(source.getSystemId()));
                }
            }
            // System.err.println("*** TransformDocument");
            if (executable==null) {
                throw new XPathException("Stylesheet has not been compiled");
            }

            openMessageEmitter();

            XPathContextMajor initialContext = newXPathContext();

            if (source != null) {

                initialContextItem = source;
                contextForGlobalVariables = source.getRoot();

                SequenceIterator currentIter = SingletonIterator.makeIterator(source);
                if (initialTemplate != null) {
                    initialContext.setSingletonFocus(initialContextItem);
                } else {
                    currentIter = initialContext.setCurrentIterator(currentIter);
                }
            }

            initializeController();

            PipelineConfiguration pipe = makePipelineConfiguration();
            Receiver result = openResult(pipe, initialContext, target, ResultDocument.APPEND_CONTENT);

            // Process the source document by applying template rules to the initial context node

            if (initialTemplate == null) {
                initialContextItem = source;
                Mode mode = getRuleManager().getMode(initialMode, false);
                if (mode == null || (initialMode != null && mode.isEmpty())) {
                    throw new XPathException("Requested initial mode " +
                            (initialMode == null ? "" : initialMode.getDisplayName()) +
                            " does not exist", "XTDE0045");
                }
                TailCall tc = ApplyTemplates.applyTemplates(
                                    initialContext.getCurrentIterator(),
                                    mode,
                                    null, null, initialContext, null);
                while (tc != null) {
                    tc = tc.processLeavingTail();
                }
            } else {
                Template t = initialTemplate;
                XPathContextMajor c2 = initialContext.newContext();
                c2.openStackFrame(t.getNumberOfSlots());
                c2.setLocalParameters(new ParameterSet());
                c2.setTunnelParameters(new ParameterSet());

                TailCall tc = t.expand(c2);
                while (tc != null) {
                    tc = tc.processLeavingTail();
                }
            }

            closeMessageEmitter();
            // the principalURI doesn't have significance because the output is a
            // standalone DOM object - unlike result-documents that are included in the
            // resultdocument pool, therefore don't check the URI:
            //checkPrincipalURI(result, initialContext);
            
            closeResult(result, initialContext);
            pendingUpdateList.apply(initialContext);
            success = true;
            principalOutputNode = openHTMLWriter.getNode();
            return principalOutputNode;
            // let caller handle exception
            
        } finally {
            inUse = false;
            principalResultURI = null;
            
            if (LogConfiguration.loggingIsEnabled()) {
            	LogController.closeTraceListener(success);
            }
        }
    }


    private void closeMessageEmitter() throws XPathException {
        //getMessageEmitter().close();
    }

    public void closeResult(Receiver result, XPathContext initialContext) throws XPathException {
        Receiver out = initialContext.getReceiver();
        out.endDocument();
        out.close();
    }
    
    private void checkPrincipalURI(Receiver result, XPathContext initialContext) throws XPathException {
    	Receiver out = initialContext.getReceiver();
        if (out instanceof ComplexContentOutputter && ((ComplexContentOutputter)out).contentHasBeenWritten()) {
            if (principalResultURI != null) {
                DocumentURI documentKey = new DocumentURI(principalResultURI);
                if (!checkUniqueOutputDestination(documentKey)) {
                    XPathException err = new XPathException(
                            "Cannot write more than one result document to the same URI, or write to a URI that has been read: " +
                            documentKey);
                    err.setErrorCode("XTDE1490");
                    throw err;
                } else {
                    addUnavailableOutputDestination(documentKey);
                }
            }
        }
    }

    public Receiver openResult(PipelineConfiguration pipe, XPathContext initialContext,
                               Node root, int method) throws XPathException {

//        if (method == ResultDocument.REPLACE_CONTENT) {
//            while (true) {
//                Node child = root.getFirstChild();
//                if (child == null) {
//                    break;
//                }
//                root.removeChild(child);
//            }
//        }
    	
        HTMLWriter writer = new HTMLWriter();
        writer.setPipelineConfiguration(pipe);
        NamespaceReducer reducer = new NamespaceReducer();
        reducer.setUnderlyingReceiver(writer);
        reducer.setPipelineConfiguration(pipe);
        writer.setNode(root);
        Receiver receiver = reducer;



        // if this is the implicit XSLT result document, and if the executable is capable
        // of creating a secondary result document, then add a filter to check the first write

        boolean openNow = false;
        if (getExecutable().createsSecondaryResult()) {
            receiver = new ImplicitResultChecker(receiver, this);
            receiver.setPipelineConfiguration(pipe);
        } else {
            openNow = true;
        }

        initialContext.changeOutputDestination(receiver, true);

        if (openNow) {
            Receiver out = initialContext.getReceiver();
            out.open();
            out.startDocument();
        }
        
        openHTMLWriter = writer;
        return receiver;
    }

    private void openMessageEmitter() throws XPathException {
//        if (getMessageEmitter() == null) {
//            Receiver me = makeMessageReceiver();
//            setMessageEmitter(me);
//        }
//        getMessageEmitter().open();
    }

    //////////////////////////////////////////////////////////////////////////
    // Handle parameters to the transformation
    //////////////////////////////////////////////////////////////////////////

    /**
     * Supply a parameter using Saxon-specific representations of the name and value
     * @param qName The structured representation of the parameter name
     * @param value The value of the parameter, or null to remove a previously set value
     */

    public void setParameter(StructuredQName qName, Sequence value) {
        if (parameters == null) {
            parameters = new HashMap<StructuredQName, Sequence>();
        }
        parameters.put(qName, value);
    }
    
    public void removeParameter(StructuredQName qName) {
        if (parameters != null) {
        	parameters.remove(qName);
        }        
    }

    /**
     * Reset the parameters to a null list.
     */

    public void clearParameters() {
        parameters = null;
    }

    /**
     * Get a parameter to the transformation. This returns the value of a parameter
     * that has been previously set using the {@link #setParameter} method. The value
     * is returned exactly as supplied, that is, before any conversion to an XPath value.
     *
     * @param qName the name of the required parameter
     * @return the value of the parameter, if it exists, or null otherwise
     */

    public Sequence getParameter(StructuredQName qName) {
        if (parameters==null) {
            return null;
        }
        return parameters.get(qName);
    }

    /**
     * Set the current date and time for this query or transformation.
     * This method is provided primarily for testing purposes, to allow tests to be run with
     * a fixed date and time. The supplied date/time must include a timezone, which is used
     * as the implicit timezone.
     *
     * <p>Note that comparisons of date/time values currently use the implicit timezone
     * taken from the system clock, not from the value supplied here.</p>
     *
     * @param dateTime the date/time value to be used as the current date and time
     * @throws IllegalStateException if a current date/time has already been
     * established by calling getCurrentDateTime(), or by a previous call on setCurrentDateTime()
     */

    public void setCurrentDateTime(DateTimeValue dateTime) throws XPathException {
        if (currentDateTime==null) {
            if (dateTime.getComponent(Component.TIMEZONE) == null) {
                throw new XPathException("No timezone is present in supplied value of current date/time");
            }
            currentDateTime = (DateTimeValue)dateTime.adjustTimezone(getConfiguration().getImplicitTimezone());
            dateTimePreset = true;
        } else {
            throw new IllegalStateException(
                    "Current date and time can only be set once, and cannot subsequently be changed");
        }
    }

    /**
     * Get the current date and time for this query or transformation.
     * All calls during one transformation return the same answer.
     *
     * @return Get the current date and time. This will deliver the same value
     *      for repeated calls within the same transformation
     */

    public DateTimeValue getCurrentDateTime() {
        if (currentDateTime==null) {
            currentDateTime = DateTimeValue.fromJavaDate(new Date());
        }
        return currentDateTime;
    }

    /**
     * Get the implicit timezone for this query or transformation
     * @return the implicit timezone as an offset in minutes
     */

    public int getImplicitTimezone() {
        return getCurrentDateTime().getTimezoneInMinutes();
    }

    /////////////////////////////////////////
    // Methods for handling dynamic context
    /////////////////////////////////////////

    /**
     * Make an XPathContext object for expression evaluation.
     * <p>
     * This method is intended for internal use.
     *
     * @return the new XPathContext
     */

    public XPathContextMajor newXPathContext() {
        return new XPathContextMajor(this);
    }

    /**
     * Set the last remembered node, for node numbering purposes.
     * <p>
     * This method is strictly for internal use only.
     *
     * @param node the node in question
     * @param number the number of this node
     */

    public void setRememberedNumber(NodeInfo node, int number) {
        lastRememberedNode = node;
        lastRememberedNumber = number;
    }

    /**
     * Get the number of a node if it is the last remembered one.
     * <p>
     * This method is strictly for internal use only.
     *
     * @param node the node for which remembered information is required
     * @return the number of this node if known, else -1.
     */

    public int getRememberedNumber(NodeInfo node) {
        if (lastRememberedNode == node) {
            return lastRememberedNumber;
        }
        return -1;
    }

    /**
     * Get a result tree, given its URI
     * @param uri the URI of the result tree
     */

//    public Document getResultTree(String uri) {
//        return resultTrees.get(uri);
//    }

    /**
     * Set a result tree (internal method)
     * @param uri the URI of the result tree
     * @param doc the document node of the result tree
     */

//    public void setResultTree(String uri, Document doc) {
//        resultTrees.put(uri, doc);
//    }
//
//    /**
//     * Get the set of all result tree URIs
//     */
//
//    public Set<String> getResultTreeUris() {
//        return resultTrees.keySet();
//    }
    
    /////////////////////////////////////////////////////////////////////////
    // Methods for tracing
    /////////////////////////////////////////////////////////////////////////

    /**
     * Set a TraceListener, replacing any existing TraceListener
     * <p>This method has no effect unless the stylesheet or query was compiled
     * with tracing enabled.</p>
     *
     * @param listener the TraceListener to be set. May be null, in which case
     *                 trace events will not be reported
     * @since 9.2
     */
    
    private TraceListener traceListener;

    public void setTraceListener(TraceListener listener) {
        this.traceListener = listener;
    }

    /**
     * Get the TraceListener. By default, there is no TraceListener, and this
     * method returns null. A TraceListener may be added using the method
     * {@link #addTraceListener}. If more than one TraceListener has been added,
     * this method will return a composite TraceListener. Because the form
     * this takes is implementation-dependent, this method is not part of the
     * stable Saxon public API.
     *
     * @return the TraceListener used for XSLT or XQuery instruction tracing, or null if absent.
     */
    /*@Nullable*/
    public TraceListener getTraceListener() { // e.g.
        return traceListener;
    }

    /**
     * Test whether instruction execution is being traced. This will be true
     * if (a) at least one TraceListener has been registered using the
     * {@link #addTraceListener} method, and (b) tracing has not been temporarily
     * paused using the {@link #pauseTracing} method.
     *
     * @return true if tracing is active, false otherwise
     * @since 8.4
     */

    public final boolean isTracing() { // e.g.
        return traceListener != null && !tracingPaused;
    }

    /**
     * Pause or resume tracing. While tracing is paused, trace events are not sent to any
     * of the registered TraceListeners.
     *
     * @param pause true if tracing is to pause; false if it is to resume
     * @since 8.4
     */
    
    private boolean tracingPaused = false;
    
    public final void pauseTracing(boolean pause) {
        tracingPaused = pause;
    }

    /**
     * Adds the specified trace listener to receive trace events from
     * this instance. Note that although TraceListeners can be added
     * or removed dynamically, this has no effect unless the stylesheet
     * or query has been compiled with tracing enabled.
     * Conversely, if this property has been set in the
     * Configuration or TransformerFactory, the TraceListener will automatically
     * be added to every Controller that uses that Configuration.
     *
     * @param trace the trace listener. If null is supplied, the call has no effect.
     * @since 8.4
     */

    public void addTraceListener(/*@Nullable*/ TraceListener trace) { // e.g.
        if (trace != null) {
            //traceListener = TraceEventMulticaster.add(traceListener, trace);
        }
    }

    /**
     * Removes the specified trace listener so that the listener will no longer
     * receive trace events.
     *
     * @param trace the trace listener.
     * @since 8.4
     */

    public void removeTraceListener(TraceListener trace) { // e.g.
        //traceListener = TraceEventMulticaster.remove(traceListener, trace);
    }
    
    
    //////////////////////

	public void setOutputProperties(Object props) {
		// TODO Auto-generated method stub - did use java.util.Properties
		
	}

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0. Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
