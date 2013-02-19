package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.PreparedStylesheet;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.functions.ConstructorFunctionLibrary;
import client.net.sf.saxon.ce.functions.FunctionLibraryList;
import client.net.sf.saxon.ce.functions.StandardFunction;
import client.net.sf.saxon.ce.functions.SystemFunctionLibrary;
import client.net.sf.saxon.ce.js.IXSLFunctionLibrary;
import client.net.sf.saxon.ce.om.DocumentURI;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.RuleManager;
import client.net.sf.saxon.ce.trans.StripSpaceRules;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.*;

import com.google.gwt.logging.client.LogConfiguration;

/**
 * Represents the stylesheet module at the root of the import tree, that is, the module
 * that includes or imports all the others. Note that this object is present at compile time only,
 * unlike the Executable, which also exists at run-time.
 */
public class PrincipalStylesheetModule extends StylesheetModule {

    private PreparedStylesheet preparedStylesheet;

    // library of functions that are in-scope for XPath expressions in this stylesheet
    private FunctionLibraryList functionLibrary;

    // version attribute on xsl:stylesheet element of principal stylesheet module
    private String version;

    // index of global variables and parameters, by StructuredQName
    // (overridden variables are excluded).
    // Used at compile-time only, except for debugging
    private HashMap<StructuredQName, Declaration> globalVariableIndex =
            new HashMap<StructuredQName, Declaration>(20);

    // table of named templates. Key is the integer fingerprint of the template name;
    // value is the XSLTemplate object in the source stylesheet.
    private HashMap<StructuredQName, Declaration> templateIndex =
            new HashMap<StructuredQName, Declaration>(20);

    // Table of named stylesheet functions. A two level lookup, using first the arity and then
    // the expanded name of the function.
    private HashMap<Integer, HashMap<StructuredQName, Declaration>> functionIndex =
            new HashMap(8);

    // map for allocating unique numbers to local parameter names. Key is a
    // StructuredQName; value is a boxed int.
    private HashMap<StructuredQName, Integer> localParameterNumbers = null;


    // namespace aliases. This information is needed at compile-time only
    private int numberOfAliases = 0;
    private List<Declaration> namespaceAliasList = new ArrayList<Declaration>(5);
    private HashMap<String, NamespaceBinding> namespaceAliasMap;
    private Set<String> aliasResultUriSet;

    // count of the maximum number of local variables in xsl:template match patterns
    private int largestPatternStackFrame = 0;

    // cache of stylesheet documents. Note that multiple imports of the same URI
    // lead to the stylesheet tree being reused
    private HashMap<DocumentURI, XSLStylesheet> moduleCache = new HashMap<DocumentURI, XSLStylesheet>(4);

    public PrincipalStylesheetModule(XSLStylesheet sourceElement, int precedence) {
        super(sourceElement, precedence);
    }

    public void setPreparedStylesheet(PreparedStylesheet preparedStylesheet) {
        this.preparedStylesheet = preparedStylesheet;
    }

    public PreparedStylesheet getPreparedStylesheet() {
        return preparedStylesheet;
    }

    public PrincipalStylesheetModule getPrincipalStylesheetModule() {
        return this;
    }

    /**
     * Create the function library
     */

    public FunctionLibraryList createFunctionLibrary() {
        Configuration config = getPreparedStylesheet().getConfiguration();
        functionLibrary = new FunctionLibraryList();
        int functionSet = StandardFunction.CORE | StandardFunction.XSLT;
        functionLibrary.addFunctionLibrary(
                SystemFunctionLibrary.getSystemFunctionLibrary(functionSet));
        functionLibrary.addFunctionLibrary(
                new StylesheetFunctionLibrary(this, true));
        functionLibrary.addFunctionLibrary(
                ConstructorFunctionLibrary.getInstance());
        functionLibrary.addFunctionLibrary(
                new IXSLFunctionLibrary());
        functionLibrary.addFunctionLibrary(
                new StylesheetFunctionLibrary(this, false));
        return functionLibrary;
    }

    /**
     * Get the function library. Available only on the principal stylesheet module
     * @return the function library
     */

    public FunctionLibraryList getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Add a module to the cache
     * @param key the key to be used (based on the absolute URI)
     * @param module the stylesheet document tree corresponding to this absolute URI
     */

    public void putStylesheetDocument(DocumentURI key, XSLStylesheet module) {
        moduleCache.put(key, module);
    }

    /**
     * Get a module from the cache
     * @param key the key to be used (based on the absolute URI)
     * @return the stylesheet document tree corresponding to this absolute URI

     */

    public XSLStylesheet getStylesheetDocument(DocumentURI key) {
        XSLStylesheet sheet = moduleCache.get(key);
        if (sheet != null) {
            XPathException warning = new XPathException(
                    "Stylesheet module " + key + " is included or imported more than once. " +
                            "This is permitted, but may lead to errors or unexpected behavior");
            getPreparedStylesheet().reportWarning(warning);
        }
        return sheet;
    }

    /**
     * Preprocess does all the processing possible before the source document is available.
     * It is done once per stylesheet, so the stylesheet can be reused for multiple source
     * documents. The method is called only on the XSLStylesheet element representing the
     * principal stylesheet module
     */

    public void preprocess() throws XPathException {

        // process any xsl:include and xsl:import elements

        spliceIncludes();

        // build indexes for selected top-level elements

        buildIndexes();

        // check for use of schema-aware constructs

        checkForSchemaAwareness();

        // process the attributes of every node in the tree

        processAllAttributes();

        // collect any namespace aliases

        collectNamespaceAliases();

        // fix up references from XPath expressions to variables and functions, for static typing

        for (int i = 0; i < topLevel.size(); i++) {
            Declaration decl = topLevel.get(i);
            StyleElement inst = decl.getSourceElement();
            if (!inst.isActionCompleted(StyleElement.ACTION_FIXUP)) {
                inst.setActionCompleted(StyleElement.ACTION_FIXUP);
//                if (inst instanceof XSLVariableDeclaration) {
//                    System.err.println("Fixup global variable " + ((XSLVariableDeclaration)inst).getVariableQName());
//                }
                inst.fixupReferences();
            }
        }

        // Validate the whole logical style sheet (i.e. with included and imported sheets)

        XSLStylesheet top = getSourceElement();
        setInputTypeAnnotations(top.getInputTypeAnnotationsAttribute());
        Declaration decl = new Declaration(this, top);
        if (!top.isActionCompleted(StyleElement.ACTION_VALIDATE)) {
            top.setActionCompleted(StyleElement.ACTION_VALIDATE);
            top.validate(decl);
            for (int i = 0; i < topLevel.size(); i++) {
                decl = topLevel.get(i);
                decl.getSourceElement().validateSubtree(decl);
            }
        }

    }

    /**
     * Build indexes for selected top-level declarations
     */

    private void buildIndexes() throws XPathException {
        // Scan the declarations in reverse order, that is, highest precedence first
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            Declaration decl = topLevel.get(i);
            decl.getSourceElement().index(decl, this);
        }

    }

    /**
     * Process the attributes of every node in the stylesheet
     */

    public void processAllAttributes() throws XPathException {
        getSourceElement().processDefaultCollationAttribute("");
        getSourceElement().prepareAttributes();
        for (Declaration decl : topLevel) {
            StyleElement inst = decl.getSourceElement();
            if (!inst.isActionCompleted(StyleElement.ACTION_PROCESS_ATTRIBUTES)) {
                inst.setActionCompleted(StyleElement.ACTION_PROCESS_ATTRIBUTES);
                try {
                    inst.processAllAttributes();
                } catch (XPathException err) {
                    decl.getSourceElement().compileError(err);
                }
            }
        }
    }

    /**
     * Add a stylesheet function to the index
     * @param decl The declaration wrapping an XSLFunction object
     * @throws XPathException
     */
    protected void indexFunction(Declaration decl) throws XPathException {
        XSLFunction function = (XSLFunction)decl.getSourceElement();
        StructuredQName qName = function.getObjectName();
        int arity = function.getNumberOfArguments();

        // see if there is already a named function with this precedence
        Declaration other = getFunctionDeclaration(qName, arity);
        if (other == null) {
            // this is the first
            putFunction(decl);
        } else {
            // check the precedences
            int thisPrecedence = decl.getPrecedence();
            int otherPrecedence = other.getPrecedence();
            if (thisPrecedence == otherPrecedence) {
                StyleElement f2 = other.getSourceElement();
                if (decl.getSourceElement() == f2) {
                     function.compileError(
                             "Function " + qName.getDisplayName() + " is declared more than once " +
                             "(caused by including the containing module more than once)",
                             "XTSE0770");
                } else {
                    function.compileError("Duplicate function declaration (see line " +
                            f2.getLineNumber() + " of " + f2.getSystemId() + ')', "XTSE0770");
                }
            } else if (thisPrecedence < otherPrecedence) {
                //
            } else {
                // can't happen, but we'll play safe
                putFunction(decl);
            }
        }
    }

    protected Declaration getFunctionDeclaration(StructuredQName name, int arity) {
        HashMap<StructuredQName, Declaration> m = functionIndex.get(arity);
        return (m == null ? null : m.get(name));
    }

    /**
     * Get the function with a given name and arity
     * @param name the name of the function
     * @param arity the arity of the function, or -1 if any arity will do
     * @return the requested function, or null if none can be found
     */

    protected XSLFunction getFunction(StructuredQName name, int arity) {
        if (arity == -1) {
            // supports the single-argument function-available() function
            for (Iterator<Integer> arities = functionIndex.keySet().iterator(); arities.hasNext();) {
                int a = arities.next();
                Declaration decl = getFunctionDeclaration(name, a);
                if (decl != null) {
                    return (XSLFunction)decl.getSourceElement();
                }
            }
            return null;
        } else {
            Declaration decl = getFunctionDeclaration(name, arity);
            return (decl == null ? null : (XSLFunction)decl.getSourceElement());
        }
    }

    protected void putFunction(Declaration decl) {
        XSLFunction function = (XSLFunction)decl.getSourceElement();
        StructuredQName qName = function.getObjectName();
        int arity = function.getNumberOfArguments();
        HashMap<StructuredQName, Declaration> m = functionIndex.get(arity);
        if (m == null) {
            m = new HashMap<StructuredQName, Declaration>();
            functionIndex.put(arity, m);
        }
        m.put(qName, decl);
    }


    /**
     * Index a global xsl:variable or xsl:param element
     * @param decl The Declaration referencing the XSLVariable or XSLParam element
     * @throws XPathException
     */

    protected void indexVariableDeclaration(Declaration decl) throws XPathException {
        XSLVariableDeclaration var = (XSLVariableDeclaration)decl.getSourceElement();
        StructuredQName qName = var.getVariableQName();
        if (qName != null) {
            // see if there is already a global variable with this precedence
            Declaration other = globalVariableIndex.get(qName);
            if (other == null) {
                // this is the first
                globalVariableIndex.put(qName, decl);
            } else {
                // check the precedences
                int thisPrecedence = decl.getPrecedence();
                int otherPrecedence = other.getPrecedence();
                if (thisPrecedence == otherPrecedence) {
                    StyleElement v2 = other.getSourceElement();
                    if (v2 == var) {
                        var.compileError(
                                 "Global variable " + qName.getDisplayName() + " is declared more than once " +
                                 "(caused by including the containing module more than once)",
                                 "XTSE0630");
                    } else {
                        var.compileError("Duplicate global variable declaration (see line " +
                                v2.getLineNumber() + " of " + v2.getSystemId() + ')', "XTSE0630");
                    }
                } else if (thisPrecedence < otherPrecedence && var != other.getSourceElement()) {
                    var.setRedundant();
                } else if (var != other.getSourceElement()) {
                    ((XSLVariableDeclaration)other.getSourceElement()).setRedundant();
                    globalVariableIndex.put(qName, decl);
                }
            }
        }
    }

    /**
     * Get the global variable or parameter with a given name (taking
     * precedence rules into account)
     * @param qName name of the global variable or parameter
     * @return the variable declaration, or null if it does not exist
     */

    public XSLVariableDeclaration getGlobalVariable(StructuredQName qName) {
        Declaration decl = globalVariableIndex.get(qName);
        return (decl == null ? null : (XSLVariableDeclaration)decl.getSourceElement());
    }

    /**
     * Allocate a unique number to a local parameter name. This should only be called on the principal
     * stylesheet module.
     * @param qName the local parameter name
     * @return an integer that uniquely identifies this parameter name within the stylesheet
     */

    public int allocateUniqueParameterNumber(StructuredQName qName) {
        if (localParameterNumbers == null) {
            localParameterNumbers = new HashMap<StructuredQName, Integer>(50);
        }
        Integer x = localParameterNumbers.get(qName);
        if (x == null) {
            x = Integer.valueOf(localParameterNumbers.size());
            localParameterNumbers.put(qName, x);
        }
        return x.intValue();
    }

    /**
     * Add a named template to the index
     * @param decl the declaration of the Template object
     * @throws XPathException
     */
    protected void indexNamedTemplate(Declaration decl) throws XPathException {
        XSLTemplate template = (XSLTemplate)decl.getSourceElement();
        StructuredQName qName = template.getTemplateName();
        if (qName != null) {
            // see if there is already a named template with this precedence
            Declaration other = templateIndex.get(qName);
            if (other == null) {
                // this is the first
                templateIndex.put(qName, decl);
                getPreparedStylesheet().putNamedTemplate(qName, template.getCompiledTemplate());
            } else {
                // check the precedences
                int thisPrecedence = decl.getPrecedence();
                int otherPrecedence = other.getPrecedence();
                if (thisPrecedence == otherPrecedence) {
                    StyleElement t2 = other.getSourceElement();
                    template.compileError("Duplicate named template (see line " +
                            t2.getLineNumber() + " of " + t2.getSystemId() + ')', "XTSE0660");
                } else if (thisPrecedence < otherPrecedence) {
                    //template.setRedundantNamedTemplate();
                } else {
                    // can't happen, but we'll play safe
                    //other.setRedundantNamedTemplate();
                    templateIndex.put(qName, decl);
                    getPreparedStylesheet().putNamedTemplate(qName, template.getCompiledTemplate());
                }
            }
        }
    }

    /**
     * Get the named template with a given name
     * @param name the name of the required template
     * @return the template with the given name, if there is one, or null otherwise. If there
     * are several templates with the same name, the one with highest import precedence
     * is returned.
     */

    public XSLTemplate getNamedTemplate(StructuredQName name) {
        Declaration decl = templateIndex.get(name);
        return (decl == null ? null : (XSLTemplate)decl.getSourceElement());
    }


    /**
     * Check for schema-awareness.
     * Typed input nodes are recognized if and only if the stylesheet contains an import-schema declaration.
     */

    private void checkForSchemaAwareness() {
        // no-op
    }


    protected void addNamespaceAlias(Declaration node) {
        namespaceAliasList.add(node);
        numberOfAliases++;
    }

    /**
     * Get the declared namespace alias for a given namespace URI code if there is one.
     * If there is more than one, we get the last.
     * @param uri The code of the uri used in the stylesheet.
     * @return The namespace code to be used (prefix in top half, uri in bottom half): return -1
     * if no alias is defined
     */

    protected NamespaceBinding getNamespaceAlias(String uri) {
        return namespaceAliasMap.get(uri);
    }

    /**
     * Determine if a namespace is included in the result-prefix of a namespace-alias
     * @param uri the  URI
     * @return true if an xsl:namespace-alias has been defined for this namespace URI
     */

    protected boolean isAliasResultNamespace(String uri) {
        return aliasResultUriSet.contains(uri);
    }

    /**
     * Collect any namespace aliases
     */

    private void collectNamespaceAliases() throws XPathException {
        namespaceAliasMap = new HashMap(numberOfAliases);
        aliasResultUriSet = new HashSet(numberOfAliases);
        HashSet<String> aliasesAtThisPrecedence = new HashSet<String>();
        //aliasSCodes = new short[numberOfAliases];
        //aliasNCodes = new int[numberOfAliases];
        //int precedenceBoundary = 0;
        int currentPrecedence = -1;
        // Note that we are processing the list in reverse stylesheet order,
        // that is, highest precedence first.
        for (int i = 0; i < numberOfAliases; i++) {
            Declaration decl = namespaceAliasList.get(i);
            XSLNamespaceAlias xna = (XSLNamespaceAlias)decl.getSourceElement();
            String scode = xna.getStylesheetURI();
            NamespaceBinding ncode = xna.getResultNamespaceBinding();
            int prec = decl.getPrecedence();

            // check that there isn't a conflict with another xsl:namespace-alias
            // at the same precedence

            if (currentPrecedence != prec) {
                currentPrecedence = prec;
                aliasesAtThisPrecedence.clear();
                //precedenceBoundary = i;
            }
            if (aliasesAtThisPrecedence.contains(scode)) {
                if (!namespaceAliasMap.get(scode).equals(ncode.getURI())) {
                    xna.compileError("More than one alias is defined for the same namespace", "XTSE0810");
                }
            }
            if (namespaceAliasMap.get(scode) == null) {
                namespaceAliasMap.put(scode, ncode);
                aliasResultUriSet.add(ncode.getURI());
            }
            aliasesAtThisPrecedence.add(scode);
        }
        namespaceAliasList = null;  // throw it in the garbage
    }

    protected boolean hasNamespaceAliases() {
        return numberOfAliases > 0;
    }


    /**
     * Compile the stylesheet to create an executable.
     */

    public void compileStylesheet() throws XPathException {

        try {

            PreparedStylesheet pss = getPreparedStylesheet();
            //Configuration config = pss.getConfiguration();
            Executable exec = pss.getExecutable();

            // Register template rules with the rule manager

            for (int i = 0; i < topLevel.size(); i++) {
                Declaration decl = topLevel.get(i);
                StyleElement snode = decl.getSourceElement();
                if (snode instanceof XSLTemplate) {
                    ((XSLTemplate)snode).register(decl);
                }
            }

            // Call compile method for each top-level object in the stylesheet
            // Note, some declarations (templates) need to be compiled repeatedly if the module
            // is imported repeatedly; others (variables, functions) do not

            for (int i = 0; i < topLevel.size(); i++) {
                Declaration decl = topLevel.get(i);
                StyleElement snode = decl.getSourceElement();
                if (!snode.isActionCompleted(StyleElement.ACTION_COMPILE)) {
                    snode.setActionCompleted(StyleElement.ACTION_COMPILE);
                    Expression inst = snode.compile(exec, decl);
                    if (inst != null) {
                        inst.setSourceLocator(snode);
                    }
                }
            }

            // Call type-check method for each user-defined function in the stylesheet. This is no longer
            // done during the optimize step, to avoid functions being inlined before they are type-checked.

//            for (int i = 0; i < topLevel.size(); i++) {
//                NodeInfo node = (NodeInfo) topLevel.get(i);
//                if (node instanceof XSLFunction) {
//                    ((XSLFunction) node).typeCheckBody();
//                }
//            }

            for (Iterator<Integer> arities = functionIndex.keySet().iterator(); arities.hasNext();) {
                for (Iterator<Declaration> fi = functionIndex.get(arities.next()).values().iterator(); fi.hasNext();) {
                    Declaration decl = fi.next();
                    StyleElement node = decl.getSourceElement();
                    if (!node.isActionCompleted(StyleElement.ACTION_TYPECHECK)) {
                            node.setActionCompleted(StyleElement.ACTION_TYPECHECK);
                        ((XSLFunction)node).typeCheckBody();
                    }
                }
            }

            if (getPreparedStylesheet().getErrorCount() > 0) {
                // not much point carrying on
                return;
            }

            // Call optimize method for each top-level object in the stylesheet
            // But for functions, do it only for those of highest precedence.

            for (int i = 0; i < topLevel.size(); i++) {
                Declaration decl = topLevel.get(i);
                StyleElement node = decl.getSourceElement();
                if (node instanceof StylesheetProcedure && !(node instanceof XSLFunction) &&
                        !node.isActionCompleted(StyleElement.ACTION_OPTIMIZE)) {
                    node.setActionCompleted(StyleElement.ACTION_OPTIMIZE);
                    ((StylesheetProcedure) node).optimize(decl);
                }
            }

            for (Iterator<Integer> arities = functionIndex.keySet().iterator(); arities.hasNext();) {
                for (Iterator<Declaration> fi = functionIndex.get(arities.next()).values().iterator(); fi.hasNext();) {
                    Declaration decl = fi.next();
                    StyleElement node = decl.getSourceElement();
                    if (!node.isActionCompleted(StyleElement.ACTION_OPTIMIZE)) {
                        node.setActionCompleted(StyleElement.ACTION_OPTIMIZE);
                        ((StylesheetProcedure) node).optimize(decl);
                    }
                }
            }

            // Fix up references to the default default decimal format

            if (pss.getDecimalFormatManager() != null) {
                try {
                    pss.getDecimalFormatManager().fixupDefaultDefault();
                } catch (XPathException err) {
                    compileError(err.getMessage(), err.getErrorCodeLocalPart());
                }
            }

            // Finish off the lists of template rules

            RuleManager ruleManager = getPreparedStylesheet().getRuleManager();
            ruleManager.computeRankings();

        } catch (RuntimeException err) {
        // if syntax errors were reported earlier, then exceptions may occur during this phase
        // due to inconsistency of data structures. We can ignore these exceptions as they
        // will go away when the user corrects the stylesheet
            if (getPreparedStylesheet().getErrorCount() == 0) {
                // rethrow the exception
                throw err;
            }
        }

    }

    /**
     * Get the list of attribute-set declarations associated with a given QName.
     * This is used for xsl:element, xsl:copy, xsl:attribute-set, and on literal
     * result elements
     *
     * @param name  the name of the required attribute set
     * @param list a list to hold the list of XSLAttributeSet elements in the stylesheet tree.
     * @return true if any declarations were found and added to the list; false if none were found
     */

    protected boolean getAttributeSets(StructuredQName name, List<Declaration> list)
            throws XPathException {

        boolean found = false;

        // search for the named attribute set, using all of them if there are several with the
        // same name

        for (int i = 0; i < topLevel.size(); i++) {
            Declaration decl = topLevel.get(i);
            if (decl.getSourceElement() instanceof XSLAttributeSet) {
                XSLAttributeSet t = (XSLAttributeSet)decl.getSourceElement();
                if (t.getAttributeSetName().equals(name)) {
                    t.incrementReferenceCount();
                    list.add(decl);
                    found = true;
                }
            }
        }
        return found;
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     * @return the Mode object holding the whitespace stripping rules. The stripping
     * rules defined in xsl:strip-space are managed in the same way as template rules,
     * hence the use of a special Mode object
     */

    protected StripSpaceRules getStripperRules() {
        Executable exec = getPreparedStylesheet().getExecutable();
        if (exec.getStripperRules() == null) {
            exec.setStripperRules(new StripSpaceRules());
        }
        return exec.getStripperRules();
    }

    /**
     * Set the value of the version attribute on the xsl:stylesheet element of the
     * principal stylesheet module
     * @param version the value of the version attribute
     */

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the value of the version attribute on the xsl:stylesheet element of the
     * principal stylesheet module
     * @return the value of the version attribute
     */

    public String getVersion() {
        return version;
    }

    /**
     * Ensure there is enough space for local variables or parameters when evaluating the match pattern of
     * template rules
     * @param n the number of slots to be allocated
     */

    public void allocatePatternSlots(int n) {
        if (n > largestPatternStackFrame) {
            largestPatternStackFrame = n;
        }
    }



    /**
     * Compile time error, specifying an error code
     * @param message   the error message
     * @param errorCode the error code. May be null if not known or not defined
     * @throws XPathException
     */

    protected void compileError(String message, String errorCode) throws XPathException {
        XPathException tce = new XPathException(message);
        tce.setErrorCode(errorCode);
        compileError(tce);
    }

    /**
     * Report an error with diagnostic information
     * @param error contains information about the error
     * @throws XPathException always, after reporting the error to the ErrorListener
     */

    protected void compileError(XPathException error)
            throws XPathException {
        error.setIsStaticError(true);
        PreparedStylesheet pss = getPreparedStylesheet();
        if (pss == null) {
            // it is null before the stylesheet has been fully built
            throw error;
        } else {
            pss.reportError(error);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


