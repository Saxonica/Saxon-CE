package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.expr.parser.CodeInjector;
import client.net.sf.saxon.ce.expr.sort.SortKeyDefinition;
import client.net.sf.saxon.ce.functions.Current;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.trace.Location;
import client.net.sf.saxon.ce.trace.XSLTTraceListener;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.linked.ElementImpl;
import client.net.sf.saxon.ce.tree.linked.NodeImpl;
import client.net.sf.saxon.ce.tree.util.NamespaceIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.DecimalValue;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.*;

/**
 * Abstract superclass for all element nodes in the stylesheet.
 * <p>Note: this class implements Locator. The element retains information about its own location
 * in the stylesheet, which is useful when an XSLT static error is found.</p>
 */

public abstract class StyleElement extends ElementImpl
        implements Container, SourceLocator {

    protected String[] extensionNamespaces = null;        // a list of URI codes
    private String[] excludedNamespaces = null;           // a list of URI codes
    protected DecimalValue version = null;                 // the effective version of this element
    protected StaticContext staticContext = null;
    protected XPathException validationError = null;
    protected int reportingCircumstances = REPORT_ALWAYS;
    protected String defaultXPathNamespace = null;
    protected String defaultCollationName = null;
    private StructuredQName objectName;
    // for instructions that define an XSLT named object, the name of that object
    private XSLStylesheet containingStylesheet;

    // Conditions under which an error is to be reported

    public static final int REPORT_ALWAYS = 1;
    public static final int REPORT_UNLESS_FORWARDS_COMPATIBLE = 2;
    public static final int REPORT_IF_INSTANTIATED = 3;
    public static final int REPORT_UNLESS_FALLBACK_AVAILABLE = 4;

    protected int actionsCompleted = 0;
    public static final int ACTION_VALIDATE = 1;
    public static final int ACTION_COMPILE = 2;
    public static final int ACTION_TYPECHECK = 4;
    public static final int ACTION_OPTIMIZE = 8;
    public static final int ACTION_FIXUP = 16;
    public static final int ACTION_PROCESS_ATTRIBUTES = 32;


    /**
     * Constructor
     */

    public StyleElement() {
    }

    public Executable getExecutable() {
        return getPreparedStylesheet();
    }

    public Configuration getConfiguration() {
        return getPreparedStylesheet().getConfiguration();
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public SourceLocator getSourceLocator() {
        return this;
    }

    public String getLocation() {
        return Navigator.getPath(this) + " in " + getBaseURI();
    }

    /**
     * Get the static context for expressions on this element
     * @return the static context
     */

    public StaticContext getStaticContext() {
        if (staticContext == null) {
            staticContext = new ExpressionContext(this);
        }
        return staticContext;
    }

    /**
     * Get the granularity of the container.
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 1;
    }

    /**
     * Make an expression visitor
     * @return the expression visitor
     */

    public ExpressionVisitor makeExpressionVisitor() {
        return ExpressionVisitor.make(getStaticContext(), getExecutable());
    }

    /**
     * Make this node a substitute for a temporary one previously added to the tree. See
     * StyleNodeFactory for details. "A node like the other one in all things but its class".
     * Note that at this stage, the node will not yet be known to its parent, though it will
     * contain a reference to its parent; and it will have no children.
     * @param temp the element which this one is substituting for
     */

    public void substituteFor(StyleElement temp) {
        setRawParent(temp.getRawParent());
        setAttributeList(temp.getAttributeList());
        setNamespaceList(temp.getNamespaceList());
        setNodeName(temp.getNodeName());
        setRawSequenceNumber(temp.getRawSequenceNumber());
        extensionNamespaces = temp.extensionNamespaces;
        excludedNamespaces = temp.excludedNamespaces;
        version = temp.version;
        validationError = temp.validationError;
        reportingCircumstances = temp.reportingCircumstances;
        //lineNumber = temp.lineNumber;
    }

    /**
     * Set a validation error. This is an error detected during construction of this element on the
     * stylesheet, but which is not to be reported until later.
     * @param reason        the details of the error
     * @param circumstances a code identifying the circumstances under which the error is to be reported
     */

    public void setValidationError(XPathException reason,
                                   int circumstances) {
        validationError = (reason);
        reportingCircumstances = circumstances;
    }

    /**
     * Ask whether this node is an instruction. The default implementation says it isn't.
     * @return true if this element is an instruction
     */

    public boolean isInstruction() {
        return false;
    }

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import). The default implementation returns false
     * @return true if the element is a permitted child of xsl:stylesheet or xsl:transform
     */

    public boolean isDeclaration() {
        return false;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return AnyItemType.getInstance();
    }

    /**
     * Get the most general type of item returned by the children of this instruction
     * @return the lowest common supertype of the item types returned by the children
     */

    protected ItemType getCommonChildItemType() {
        ItemType t = EmptySequenceTest.getInstance();
        for (NodeImpl child : allChildren()) {
            if (child instanceof StyleElement) {
                ItemType ret = ((StyleElement)child).getReturnedItemType();
                if (ret != null) {
                    t = Type.getCommonSuperType(t, ret);
                }
            } else {
                t = Type.getCommonSuperType(t, NodeKindTest.TEXT);
            }
            if (t == AnyItemType.getInstance()) {
                return t;       // no point looking any further
            }
        }
        return t;
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this returns false.
     * @return true if one or more tail calls were identified
     */

    protected boolean markTailCalls() {
        return false;
    }

    /**
     * Determine whether this type of element is allowed to contain a sequence constructor
     * @return true if this instruction is allowed to contain a sequence constructor
     */

    protected boolean mayContainSequenceConstructor() {
        return false;
    }

    /**
     * Determine whether this type of element is allowed to contain an xsl:param element
     * @param attName if null, the method tests whether an xsl:param child is allowed.
     *                If non-null, it tests whether an xsl:param child with the given attribute name is allowed
     * @return true if this element is allowed to contain an xsl:param
     */

    protected boolean mayContainParam(String attName) {
        return false;
    }

    /**
     * Get the containing XSLStylesheet element
     * @return the XSLStylesheet element representing the outermost element of the containing
     *         stylesheet module. Exceptionally, return null if there is no containing XSLStylesheet element
     */

    public XSLStylesheet getContainingStylesheet() {
        if (containingStylesheet == null) {
            NodeInfo node = this;
            while (node != null && !(node instanceof XSLStylesheet)) {
                node = node.getParent();
            }
            containingStylesheet = (XSLStylesheet)node;
        }
        return containingStylesheet;
    }

    /**
     * Make a structured QName, using this Element as the context for namespace resolution, and
     * registering the code in the namepool. If the name is unprefixed, the
     * default namespace is <b>not</b> used.
     * @param lexicalQName The lexical QName as written, in the form "[prefix:]localname". The name must have
     *                     already been validated as a syntactically-correct QName. Leading and trailing whitespace
     *                     will be trimmed
     * @return the StructuredQName representation of this lexical QName
     * @throws XPathException     if the qname is not a lexically-valid QName, or if the name
     *                            is in a reserved namespace.
     * @throws NamespaceException if the prefix of the qname has not been declared
     */

    public final StructuredQName makeQName(String lexicalQName)
            throws XPathException, NamespaceException {

        StructuredQName qName;
        try {
            qName = StructuredQName.fromLexicalQName(lexicalQName, "", new InscopeNamespaceResolver(this));
        } catch (XPathException e) {
            e.setIsStaticError(true);
            String code = e.getErrorCodeLocalPart();
            if ("FONS0004".equals(code)) {
                e.setErrorCode("XTSE0280");
            } else if ("FOCA0002".equals(code)) {
                e.setErrorCode("XTSE0020");
            } else if (code == null) {
                e.setErrorCode("XTSE0020");
            }
            throw e;
        }
        if (NamespaceConstant.isReserved(qName.getNamespaceURI())) {
            XPathException err = new XPathException("Namespace prefix " +
                    qName.getPrefix() + " refers to a reserved namespace");
            err.setIsStaticError(true);
            err.setErrorCode("XTSE0080");
            throw err;
        }
        return qName;
    }


    /**
     * Process the attributes of this element and all its children
     * @throws XPathException in the event of a static error being detected
     */

    protected void processAllAttributes() throws XPathException {
        if (!(this instanceof LiteralResultElement)) {
            processDefaultCollationAttribute("");
        }
        getStaticContext();
        processAttributes();
        for (NodeImpl child : allChildren()) {
            if (child instanceof StyleElement) {
                ((StyleElement)child).processAllAttributes();
            }
        }
    }

    /**
     * Process the standard attributes such as [xsl:]default-collation
     * @param namespace either "" to find the attributes in the null namespace,
     *                  or NamespaceConstant.XSLT to find them in the XSLT namespace
     */

    public void processStandardAttributes(String namespace) throws XPathException {
        processDefaultCollationAttribute(namespace);
        processExtensionElementAttribute(namespace);
        processExcludedNamespaces(namespace);
        processVersionAttribute(namespace);
        processDefaultXPathNamespaceAttribute(namespace);
    }

    /**
     * Process the attribute list for the element. This is a wrapper method that calls
     * prepareAttributes (provided in the subclass) and traps any exceptions
     */

    protected final void processAttributes() throws XPathException {
        try {
            prepareAttributes();
        } catch (XPathException err) {
            compileError(err);
        }
    }

    private Set<String> permittedAttributes = new HashSet<String>(8);

    protected Object checkAttribute(String name, String flags) throws XPathException {
        permittedAttributes.add(name);
        String val = getAttributeList().getValue("", name);
        if (val == null) {
            if (flags.contains("1")) {
                reportAbsence(getDisplayName() + "/" + name);
            }
        } else {
            for (int i=0; i<flags.length(); i++) {
                switch (flags.charAt(i)) {
                    case 'a': // attribute value template
                        return makeAttributeValueTemplate(val);
                    case 'b': // boolean yes/no
                        String yesNo = Whitespace.trim(val);
                        if ("yes".equals(yesNo)) {
                            return true;
                        } else if ("no".equals(yesNo)) {
                            return false;
                        } else {
                            compileError("The @" + name + " attribute must have the value 'yes' or 'no'", "XTSE0020");
                        }
                    case 'e': // expression
                        return makeExpression(val);
                    case 'p': // pattern
                        return makePattern(val);
                    case 'q': // QName
                        try {
                            return makeQName(val);
                        } catch (NamespaceException e) {
                            compileError(e.getMessage(), "XTSE0280");
                        }
                    case 's': // string
                        return val;
                    case 't': // type attribute
                        compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
                    case 'v': // validation attribute
                        if (!val.equals("strip")) {
                            compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
                        }
                        return null;
                    case 'w': // whitespace-normalized string
                        return Whitespace.collapseWhitespace(val).toString();
                    case 'z': // sequence type
                        return makeSequenceType(val);

                }
            }
        }
        return null;
    }

    protected void checkForUnknownAttributes() throws XPathException {
        AttributeCollection atts = getAttributeList();
        for (int a = 0; a < atts.getLength(); a++) {
            StructuredQName qn = atts.getStructuredQName(a);
            if (qn.getNamespaceURI().equals("") && !permittedAttributes.contains(qn.getLocalName())) {
                checkUnknownAttribute(qn);
            }
        }
    }

    /**
     * Check whether an unknown attribute is permitted.
     * @param nc The name code of the attribute name
     * @throws XPathException (and reports the error) if this is an attribute
     *                        that is not permitted on the containing element
     */

    private void checkUnknownAttribute(StructuredQName nc) throws XPathException {

        if (forwardsCompatibleModeIsEnabled()) {
            // then unknown attributes are permitted and ignored
            return;
        }

        String attributeURI = nc.getNamespaceURI();
        String elementURI = getURI();
        String localName = nc.getLocalName();

        if ((localName.equals("default-collation") ||
                        localName.equals("xpath-default-namespace") ||
                        localName.equals("extension-element-prefixes") ||
                        localName.equals("exclude-result-prefixes") ||
                        localName.equals("version") ||
                        localName.equals("use-when"))) {
            if (elementURI.equals(NamespaceConstant.XSLT)) {
                if ("".equals(attributeURI)) {
                    return;
                }
            } else if (attributeURI.equals(NamespaceConstant.XSLT) && isInstruction()) {
                return;
            }
        }

        if ("".equals(attributeURI) || NamespaceConstant.XSLT.equals(attributeURI)) {
            compileError("Attribute " + Err.wrap(nc.getDisplayName(), Err.ATTRIBUTE) +
                    " is not allowed on element " + Err.wrap(getDisplayName(), Err.ELEMENT), "XTSE0090");
        }
    }


    /**
     * Set the attribute list for the element. This is called to process the attributes (note
     * the distinction from processAttributes in the superclass).
     * Must be supplied in a subclass
     */

    protected abstract void prepareAttributes() throws XPathException;

    /**
     * Find the last child instruction of this instruction. Returns null if
     * there are no child instructions, or if the last child is a text node.
     * @return the last child instruction, or null if there are no child instructions
     */

    protected StyleElement getLastChildInstruction() {
        StyleElement last = null;
        for (NodeImpl child : allChildren()) {
            if (child instanceof StyleElement) {
                last = (StyleElement)child;
            } else {
                last = null;
            }
        }
        return last;
    }

    /**
     * Compile an XPath expression in the context of this stylesheet element
     * @param expression the source text of the XPath expression
     * @return the compiled expression tree for the XPath expression
     */

    public Expression makeExpression(String expression)
            throws XPathException {
        try {
            return ExpressionTool.make(expression,
                    getStaticContext(),
                    this, 0, Token.EOF,
                    this
            );
        } catch (XPathException err) {
            err.setLocator(this);
            compileError(err);
            ErrorExpression erexp = new ErrorExpression(err);
            erexp.setSourceLocator(this);
            erexp.setContainer(this);
            return erexp;
        }
    }

    /**
     * Make a pattern in the context of this stylesheet element
     * @param pattern the source text of the pattern
     * @return the compiled pattern
     */

    public Pattern makePattern(String pattern)
            throws XPathException {
        try {
            return Pattern.make(pattern, getStaticContext(), this);
        } catch (XPathException err) {
            compileError(err);
            return new NodeTestPattern(AnyNodeTest.getInstance());
        }
    }

    /**
     * Make an attribute value template in the context of this stylesheet element
     * @param expression the source text of the attribute value template
     * @return a compiled XPath expression that computes the value of the attribute (including
     *         concatenating the results of embedded expressions with any surrounding fixed text)
     */

    protected Expression makeAttributeValueTemplate(String expression)
            throws XPathException {
        try {
            return AttributeValueTemplate.make(expression, this, getStaticContext());
        } catch (XPathException err) {
            compileError(err);
            return new StringLiteral(expression);
        }
    }

    /**
     * Process an attribute whose value is a SequenceType
     * @param sequenceType the source text of the attribute
     * @return the processed sequence type
     * @throws XPathException if the syntax is invalid or for example if it refers to a type
     *                        that is not in the static context
     */

    public SequenceType makeSequenceType(String sequenceType)
            throws XPathException {
        try {
            ExpressionParser parser = new ExpressionParser();
            parser.setLanguage(ExpressionParser.XPATH);
            return parser.parseSequenceType(sequenceType, getStaticContext());
        } catch (XPathException err) {
            compileError(err);
            // recovery path after reporting an error, e.g. undeclared namespace prefix
            return SequenceType.ANY_SEQUENCE;
        }
    }

    /**
     * Process the [xsl:]extension-element-prefixes attribute if there is one
     * @param ns the namespace URI of the attribute - either the XSLT namespace or "" for the null namespace
     * @throws XPathException in the event of a bad prefix
     */

    protected void processExtensionElementAttribute(String ns) throws XPathException {
        String ext = getAttributeValue(ns, "extension-element-prefixes");
        if (ext != null) {
            extensionNamespaces = processPrefixList(ext, false);
        }
    }

    /**
     * Process the [xsl:]exclude-result-prefixes attribute if there is one
     * @param ns the namespace URI of the attribute required, either the XSLT namespace or ""
     * @throws XPathException in the event of a bad prefix
     */

    protected void processExcludedNamespaces(String ns) throws XPathException {
        String ext = getAttributeValue(ns, "exclude-result-prefixes");
        if (ext != null) {
            excludedNamespaces = processPrefixList(ext, true);
        }
    }

    /**
     * Process a string containing a whitespace-separated sequence of namespace prefixes
     * @param in  the input string
     * @param allowAll true if the token #all is permitted
     * @return the list of corresponding namespace URIs
     * @throws XPathException if there is a bad prefix
     */

    private String[] processPrefixList(String in, boolean allowAll) throws XPathException {
        NamespaceResolver resolver = new InscopeNamespaceResolver(this);
        if (allowAll && "#all".equals(Whitespace.trim(in))) {
            Iterator<NamespaceBinding> codes = NamespaceIterator.iterateNamespaces(this);
            List<String> result = new ArrayList<String>();
            while (codes.hasNext()) {
                result.add(codes.next().getURI());
            }
            return result.toArray(new String[result.size()]);
        } else {
            List<String> tokens = Whitespace.tokenize(in);
            int count = tokens.size();
            String[] result = new String[count];
            count = 0;
            for (String s : tokens) {
                if ("#default".equals(s)) {
                    s = "";
                } else if (allowAll && "#all".equals(s)) {
                    compileError("In exclude-result-prefixes, cannot mix #all with other values", "XTSE0020");
                }
                String uri = resolver.getURIForPrefix(s, true);
                if (uri == null) {
                    compileError("Prefix " + s + " is undeclared", "XTSE1430");
                }
                result[count++] = uri;
            }
            return result;
        }
    }


    /**
     * Process the [xsl:]version attribute if there is one
     * @param ns the namespace URI of the attribute required, either the XSLT namespace or ""
     * @throws XPathException if the value is invalid
     */

    protected void processVersionAttribute(String ns) throws XPathException {
        String v = Whitespace.trim(getAttributeValue(ns, "version"));
        if (v != null) {
            ConversionResult val = DecimalValue.makeDecimalValue(v);
            if (val instanceof ValidationFailure) {
                compileError("The version attribute must be a decimal literal", "XTSE0110");
                version = DecimalValue.TWO;
            } else {
                // Note this will normalize the decimal so that trailing spaces are not significant
                version = (DecimalValue)val;
            }
        }
    }

    /**
     * Get the numeric value of the version number appearing as an attribute on this element,
     * or inherited from its ancestors
     * @return the version number as a decimal
     */

    public DecimalValue getEffectiveVersion() {
        if (version == null) {
            NodeInfo node = getParent();
            if (node instanceof StyleElement) {
                version = ((StyleElement)node).getEffectiveVersion();
            } else {
                return DecimalValue.TWO;    // defensive programming
            }
        }
        return version;
    }

    /**
     * Determine whether forwards-compatible mode is enabled for this element
     * @return true if forwards-compatible mode is enabled
     */

    public boolean forwardsCompatibleModeIsEnabled() {
        return getEffectiveVersion().compareTo(DecimalValue.TWO) > 0;
    }

    /**
     * Determine whether 1.0-compatible mode is enabled for this element
     * @return true if 1.0 compatable mode is enabled, that is, if this or an enclosing
     *         element specifies an [xsl:]version attribute whose value is less than 2.0
     */

    public boolean xPath10ModeIsEnabled() {
        return getEffectiveVersion().compareTo(DecimalValue.TWO) < 0;
    }

    /**
     * Process the [xsl:]default-xpath-namespace attribute if there is one
     * @param ns the namespace of the attribute required, either the XSLT namespace or ""
     * @throws XPathException if the value is invalid
     */

    protected void processDefaultCollationAttribute(String ns) throws XPathException {
        String v = getAttributeValue(ns, "default-collation");
        if (v != null) {
            for (String uri : Whitespace.tokenize(v)) {
                if (uri.equals(NamespaceConstant.CODEPOINT_COLLATION_URI) || uri.startsWith("http://saxon.sf.net/")) {
                    defaultCollationName = uri;
                    return;
                } else {
                    URI collationURI;
                    try {
                        collationURI = new URI(uri, true);
                        if (!collationURI.isAbsolute()) {
                            URI base = new URI(getBaseURI());
                            collationURI = base.resolve(collationURI.toString());
                            uri = collationURI.toString();
                        }
                    } catch (URI.URISyntaxException err) {
                        compileError("default collation '" + uri + "' is not a valid URI");
                        uri = NamespaceConstant.CODEPOINT_COLLATION_URI;
                    }

                    if (getConfiguration().getNamedCollation(uri) != null) {
                        defaultCollationName = uri;
                        return;
                    }
                }
                // if not recognized, try the next URI in order
            }
            compileError("No recognized collation URI found in default-collation attribute", "XTSE0125");
        }
    }

    /**
     * Get the default collation for this stylesheet element. If no default collation is
     * specified in the stylesheet, return the Unicode codepoint collation name.
     * @return the name of the default collation
     */

    protected String getDefaultCollationName() {
        NodeInfo e = this;
        while (e instanceof StyleElement) {
            if (((StyleElement)e).defaultCollationName != null) {
                return ((StyleElement)e).defaultCollationName;
            }
            e = e.getParent();
        }
        return NamespaceConstant.CODEPOINT_COLLATION_URI;
    }

    /**
     * Check whether a particular extension element namespace is defined on this node.
     * This checks this node only, not the ancestor nodes.
     * The implementation checks whether the prefix is included in the
     * [xsl:]extension-element-prefixes attribute.
     * @param uri the namespace URI being tested
     * @return true if this namespace is defined on this element as an extension element namespace
     */

    protected boolean definesExtensionElement(String uri) {
        if (extensionNamespaces == null) {
            return false;
        }
        for (String extensionNamespace : extensionNamespaces) {
            if (extensionNamespace.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a namespace uri defines an extension element. This checks whether the
     * namespace is defined as an extension namespace on this or any ancestor node.
     * @param uri the namespace URI being tested
     * @return true if the URI is an extension element namespace URI
     */

    public boolean isExtensionNamespace(String uri) {
        NodeInfo p = getParent();
        return definesExtensionElement(uri) || (p instanceof StyleElement && ((StyleElement)p).isExtensionNamespace(uri));
    }

    /**
     * Check whether this node excludes a particular namespace from the result.
     * This method checks this node only, not the ancestor nodes.
     * @param uri the namespace URI being tested
     * @return true if the namespace is excluded by virtue of an [xsl:]exclude-result-prefixes attribute
     */

    protected boolean definesExcludedNamespace(String uri) {
        if (excludedNamespaces == null) {
            return false;
        }
        for (String excludedNamespace : excludedNamespaces) {
            if (excludedNamespace.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a namespace uri defines an namespace excluded from the result.
     * This checks whether the namespace is defined as an excluded namespace on this
     * or any ancestor node.
     * @param uri the namespace URI being tested
     * @return true if this namespace URI is a namespace excluded by virtue of exclude-result-prefixes
     *         on this element or on an ancestor element
     */

    public boolean isExcludedNamespace(String uri) {
        if (uri.equals(NamespaceConstant.XSLT) || uri.equals(NamespaceConstant.XML)) {
            return true;
        }
        NodeInfo p = getParent();
        return definesExcludedNamespace(uri) || (p instanceof StyleElement && ((StyleElement)p).isExcludedNamespace(uri));
    }

    /**
     * Process the [xsl:]xpath-default-namespace attribute if there is one
     * @param ns the namespace URI of the attribute required  (the default namespace or the XSLT namespace.)
     */

    protected void processDefaultXPathNamespaceAttribute(String ns) {
        String v = getAttributeValue(ns, "xpath-default-namespace");
        if (v != null) {
            defaultXPathNamespace = v;
        }
    }

    /**
     * Get the default XPath namespace for elements and types
     * @return the default namespace for elements and types.
     *         Return {@link NamespaceConstant#NULL} for the non-namespace
     */

    protected String getDefaultXPathNamespace() {
        NodeInfo anc = this;
        while (anc instanceof StyleElement) {
            String x = ((StyleElement)anc).defaultXPathNamespace;
            if (x != null) {
                return x;
            }
            anc = anc.getParent();
        }
        return NamespaceConstant.NULL;
        // indicates that the default namespace is the null namespace
    }

    /**
     * Check that the stylesheet element is valid. This is called once for each element, after
     * the entire tree has been built. As well as validation, it can perform first-time
     * initialisation. The default implementation does nothing; it is normally overriden
     * in subclasses.
     * @param decl
     */

    public void validate(Declaration decl) throws XPathException {
    }

    /**
     * Hook to allow additional validation of a parent element immediately after its
     * children have been validated.
     */

    public void postValidate() throws XPathException {
    }

    /**
     * Method supplied by declaration elements to add themselves to a stylesheet-level index
     * @param decl the Declaration being indexed. (This corresponds to the StyleElement object
     * except in cases where one module is imported several times with different precedence.)
     * @param top  the outermost XSLStylesheet element
     */

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
    }

    /**
     * Type-check an expression. This is called to check each expression while the containing
     * instruction is being validated. It is not just a static type-check, it also adds code
     * to perform any necessary run-time type checking and/or conversion.
     * @param exp  the expression to be checked
     * @return the (possibly rewritten) expression after type checking
     */

    // Note: the typeCheck() call is done at the level of individual path expression; the optimize() call is done
    // for a template or function as a whole. We can't do it all at the function/template level because
    // the static context (e.g. namespaces) changes from one XPath expression to another.
    public Expression typeCheck(Expression exp) throws XPathException {

        if (exp == null) {
            return null;
        }

        exp.setContainer(this);
        // temporary, until the instruction is compiled

        try {
            exp = makeExpressionVisitor().typeCheck(exp, Type.ITEM_TYPE);
            exp = ExpressionTool.resolveCallsToCurrentFunction(exp);
            if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
            	CodeInjector injector = ((XSLTTraceListener)LogController.getTraceListener()).getCodeInjector();
            	String name = "";
            	exp = injector.inject(exp, getStaticContext(), new StructuredQName("xsl", NamespaceConstant.XSLT, "text"), new StructuredQName("", "", name));
            }
            return exp;
        } catch (XPathException err) {
            // we can't report a dynamic error such as divide by zero unless the expression
            // is actually executed.
            if (err.isStaticError() || err.isTypeError()) {
                compileError(err);
                return exp;
            } else {
                ErrorExpression erexp = new ErrorExpression(err);
                erexp.setSourceLocator(this);
                return erexp;
            }
        }
    }

    /**
     * Allocate slots in the local stack frame to range variables used in an XPath expression
     * @param exp the XPath expression for which slots are to be allocated
     */

//    public void allocateSlots(Expression exp) {
//        SlotManager slotManager = getContainingSlotManager();
//        int firstSlot = slotManager.getNumberOfVariables();
//        int highWater = ExpressionTool.allocateSlots(exp, firstSlot);
//        if (highWater > firstSlot) {
//            slotManager.setNumberOfVariables(highWater);
//            // This algorithm is not very efficient because it never reuses
//            // a slot when a variable goes out of scope. But at least it is safe.
//            // Note that range variables within XPath expressions need to maintain
//            // a slot until the instruction they are part of finishes, e.g. in
//            // xsl:for-each.
//        }
//    }

    /**
     * Allocate space for range variables within predicates in the match pattern. The xsl:template
     * element has no XPath expressions among its attributes, so if this method is called on this
     * object it can only be because there are variables used in the match pattern. We work out
     * how many slots are needed for the match pattern in each template rule, and then apply-templates
     * can allocate a stack frame that is large enough for the most demanding match pattern in the
     * entire stylesheet.
     * @param slots the number of slots required
     */

    public void allocatePatternSlots(int slots) {
        getPrincipalStylesheetModule().allocatePatternSlots(slots);
    }

    /**
     * Type-check a pattern. This is called to check each pattern while the containing
     * instruction is being validated. It is not just a static type-check, it also adds code
     * to perform any necessary run-time type checking and/or conversion.
     * @param name    the name of the attribute holding the pattern, for example "match": used in
     *                diagnostics
     * @param pattern the compiled pattern
     * @return the original pattern, or a substitute pattern if it has been rewritten
     */

    public Pattern typeCheck(String name, Pattern pattern) throws XPathException {
        if (pattern == null) {
            return null;
        }
        try {
            pattern = pattern.analyze(makeExpressionVisitor(), Type.NODE_TYPE);
            boolean usesCurrent = false;

            Iterator sub = pattern.iterateSubExpressions();
            while (sub.hasNext()) {
                Expression filter = (Expression)sub.next();
                if (ExpressionTool.callsFunction(filter, Current.FN_CURRENT)) {
                    usesCurrent = true;
                    break;
                }
            }
            if (usesCurrent) {
                LetExpression let = new LetExpression();
                let.setVariableQName(new StructuredQName("saxon", NamespaceConstant.SAXON, "current" + hashCode()));
                let.setRequiredType(SequenceType.SINGLE_ITEM);
                let.setSequence(new ContextItemExpression());
                let.setAction(Literal.makeEmptySequence());
                PromotionOffer offer = new PromotionOffer();
                offer.action = PromotionOffer.REPLACE_CURRENT;
                offer.containingExpression = let;
                pattern.resolveCurrent(let, offer, true);
            }

            return pattern;
        } catch (XPathException err) {
            // we can't report a dynamic error such as divide by zero unless the pattern
            // is actually executed. We don't have an error pattern available, so we
            // construct one
            if (err.isReportableStatically()) {
                XPathException e2 = new XPathException("Error in " + name + " pattern", err);
                e2.setLocator(this);
                e2.setErrorCodeQName(err.getErrorCodeQName());
                throw e2;
            } else {
                LocationPathPattern errpat = new LocationPathPattern();
                errpat.setExecutable(getExecutable());
                errpat.addFilter(new ErrorExpression(err));
                return errpat;
            }
        }
    }

    /**
     * Fix up references from XPath expressions. Overridden for function declarations
     * and variable declarations
     */

    public void fixupReferences() throws XPathException {
        for (NodeImpl child : allChildren()) {
            if (child instanceof StyleElement) {
                ((StyleElement)child).fixupReferences();
            }
        }
    }

    /**
     * Get the SlotManager for the containing Procedure definition
     * @return the SlotManager associated with the containing Function, Template, etc,
     *         or null if there is no such containing Function, Template etc.
     */

//    public SlotManager getContainingSlotManager() {
//        NodeInfo node = this;
//        while (true) {
//            NodeInfo next = node.getParent();
//            if (next instanceof XSLStylesheet) {
//                if (node instanceof StylesheetProcedure) {
//                    return ((StylesheetProcedure)node).getSlotManager();
//                } else {
//                    return null;
//                }
//            }
//            node = next;
//        }
//    }

    /**
     * Recursive walk through the stylesheet to validate all nodes
     * @param decl
     */

    public void validateSubtree(Declaration decl) throws XPathException {
        if (isActionCompleted(StyleElement.ACTION_VALIDATE)) {
            return;
        }
        setActionCompleted(StyleElement.ACTION_VALIDATE);
        if (validationError != null) {
            if (reportingCircumstances == REPORT_ALWAYS) {
                compileError(validationError);
            } else if (reportingCircumstances == REPORT_UNLESS_FORWARDS_COMPATIBLE
                    && !forwardsCompatibleModeIsEnabled()) {
                compileError(validationError);
            } else if (reportingCircumstances == REPORT_UNLESS_FALLBACK_AVAILABLE) {
                boolean hasFallback = false;
                for (NodeImpl child : allChildren()) {
                    if (child instanceof XSLFallback) {
                        hasFallback = true;
                        ((XSLFallback)child).validateSubtree(decl);
                    }
                }
                if (!hasFallback) {
                    compileError(validationError);
                }

            }
        } else {
            try {
                validate(decl);
            } catch (XPathException err) {
                compileError(err);
            }
            validateChildren(decl);
            postValidate();
        }
    }

    /**
     * Validate the children of this node, recursively. Overridden for top-level
     * data elements.
     * @param decl
     */

    protected void validateChildren(Declaration decl) throws XPathException {
        boolean containsInstructions = mayContainSequenceConstructor();
        for (NodeImpl child : allChildren()) {
            if (child instanceof StyleElement) {
                if (containsInstructions && !((StyleElement)child).isInstruction()
                        && !isPermittedChild((StyleElement)child)) {
                    ((StyleElement)child).compileError("An " + getDisplayName() + " element must not contain an " +
                            child.getDisplayName() + " element", "XTSE0010");
                }
                ((StyleElement)child).validateSubtree(decl);
            }
        }
    }

    /**
     * Check whether a given child is permitted for this element. This method is used when a non-instruction
     * child element such as xsl:sort is encountered in a context where instructions would normally be expected.
     * @param child the child that may or may not be permitted
     * @return true if the child is permitted.
     */

    protected boolean isPermittedChild(StyleElement child) {
        return false;
    }

    /**
     * Get the PreparedStylesheet object.
     * @return the PreparedStylesheet to which this stylesheet element belongs.
     *         Exceptionally (with early errors in a simplified stylesheet module) return null.
     */

    public Executable getPreparedStylesheet() {
        XSLStylesheet xss = getContainingStylesheet();
        return (xss==null ? null : xss.getPreparedStylesheet());
    }

    /**
     * Get the principal stylesheet module
     * @return the principal stylesheet module
     */

    public PrincipalStylesheetModule getPrincipalStylesheetModule() {
        return getContainingStylesheet().getPrincipalStylesheetModule();
    }

    /**
     * Check that among the children of this element, any xsl:sort elements precede any other elements
     * @param sortRequired true if there must be at least one xsl:sort element
     * @throws XPathException if invalid
     */

    protected void checkSortComesFirst(boolean sortRequired) throws XPathException {
        boolean sortFound = false;
        boolean nonSortFound = false;
        for (NodeImpl child : allChildren()) {
            if (child instanceof XSLSort) {
                if (nonSortFound) {
                    ((XSLSort)child).compileError("Within " + getDisplayName() +
                            ", xsl:sort elements must come before other instructions", "XTSE0010");
                }
                sortFound = true;
            } else if (child.getNodeKind() == Type.TEXT) {
                // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValue())) {
                    nonSortFound = true;
                }
            } else {
                nonSortFound = true;
            }
        }
        if (sortRequired && !sortFound) {
            compileError(getDisplayName() + " must have at least one xsl:sort child", "XTSE0010");
        }
    }

    /**
     * Convenience method to check that the stylesheet element is at the top level
     * @param errorCode the error to throw if it is not at the top level; defaults to XTSE0010
     *                  if the value is null
     * @throws XPathException if not at top level
     */

    public void checkTopLevel(String errorCode) throws XPathException {
        if (!(getParent() instanceof XSLStylesheet)) {
            compileError("Element must be used only at top level of stylesheet", (errorCode == null ? "XTSE0010" : errorCode));
        }
    }

    /**
     * Convenience method to check that the stylesheet element is empty
     * @throws XPathException if it is not empty
     */

    public void checkEmpty() throws XPathException {
        if (hasChildNodes()) {
            compileError("Element must be empty", "XTSE0260");
        }
    }

    /**
     * Validate the children against a list of allowed local names
     * @param localName the allowed local names, which must be in alphabetical order
     * @throws XPathException
     */

    public void onlyAllow(String... localName) throws XPathException {
        for (NodeImpl child : allChildren()) {
            if (Arrays.binarySearch(localName, child.getLocalPart()) >= 0) {
                // OK;
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValue())) {
                    compileError("No character data is allowed within " + getDisplayName(), "XTSE0010");
                }
            } else {
                compileError("Child element " + child.getDisplayName() +
                        " is not allowed as a child of " + getDisplayName(), "XTSE0010");
            }
        }
    }

    /**
     * Convenience method to report the absence of a mandatory attribute
     * @param attribute the name of the attribute whose absence is to be reported
     * @throws XPathException if the attribute is missing
     */

    public void reportAbsence(String attribute)
            throws XPathException {
        compileError("Element must have an @" + attribute + " attribute", "XTSE0010");
    }


    /**
     * Compile the instruction on the stylesheet tree into an executable instruction
     * for use at run-time.
     * @param exec the Executable
     * @param decl the containing top-level declaration, for example xsl:function or xsl:template
     * @return either a ComputedExpression, or null. The value null is returned when compiling an instruction
     *         that returns a no-op, or when compiling a top-level object such as an xsl:template that compiles
     *         into something other than an instruction.
     */

    public abstract Expression compile(Executable exec, Declaration decl) throws XPathException;

    /**
     * Compile the children of this instruction on the stylesheet tree, adding the
     * subordinate instructions to the parent instruction on the execution tree.
     * @param exec          the Executable
     * @param decl          the Declaration of the containing top-level stylesheet element
     */

    public Expression compileSequenceConstructor(Executable exec, Declaration decl) throws XPathException {
        return compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD, AnyNodeTest.getInstance()));
    }

    private Expression compileSequenceConstructor(Executable exec, Declaration decl, SequenceIterator iter)
            throws XPathException {

        List<Expression> contents = new ArrayList<Expression>(10);
        while (true) {
            NodeInfo node = ((NodeInfo)iter.next());
            if (node == null) {
                //return result;
                break;
            }
            if (node.getNodeKind() == Type.TEXT) {
                // handle literal text nodes by generating an xsl:value-of instruction
                UnfailingIterator lookahead = node.iterateAxis(Axis.FOLLOWING_SIBLING, AnyNodeTest.getInstance());
                NodeInfo sibling = (NodeInfo)lookahead.next();
                if (!(sibling instanceof XSLParam || sibling instanceof XSLSort)) {
                    // The test for XSLParam and XSLSort is to eliminate whitespace nodes that have been retained
                    // because of xml:space="preserve"
                    Instruction text = new ValueOf(new StringLiteral(node.getStringValue()), false);
                    text.setSourceLocator(this);
                    if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                    	CodeInjector injector = ((XSLTTraceListener)LogController.getTraceListener()).getCodeInjector();
                        Expression tracer = injector.inject(
                                text, getStaticContext(), new StructuredQName("xsl", NamespaceConstant.XSLT, "text"), null);
                        tracer.setSourceLocator(this);
                        if (tracer instanceof Instruction) {
                        	text = (Instruction)tracer;
                        }
                    }
                    contents.add(text);
                }

            } else if (node instanceof XSLVariable) {
                Expression var = ((XSLVariable)node).compileLocalVariable(exec, decl);
                if (var == null) {
                    // this means that the variable declaration is redundant
                    //continue;
                } else {
                    LocalVariable lv = (LocalVariable)var;
                    Expression tail = compileSequenceConstructor(exec, decl, iter);
                    if (tail == null || Literal.isEmptySequence(tail)) {
                        // this doesn't happen, because if there are no instructions following
                        // a variable, we'll have taken the var==null path above
                        //return result;
                    } else {
                        LetExpression let = new LetExpression();
                        let.setRequiredType(lv.getRequiredType());
                        let.setVariableQName(lv.getVariableQName());
                        let.setSequence(lv.getSelectExpression());
                        let.setAction(tail);
                        ((XSLVariable)node).fixupBinding(let);
                        let.setSourceLocator(this);
                        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {                        
							TraceExpression t = new TraceExpression(let);
							t.setConstructType(Location.LET_EXPRESSION);
							t.setObjectName(lv.getVariableQName());
							contents.add(t);
                        } else {
                        	contents.add(let);
                        }
                    }
                }

            } else if (node instanceof StyleElement) {
                StyleElement snode = (StyleElement)node;
                Expression child;
                if (snode.validationError != null && !(this instanceof AbsentExtensionElement)) {
                    child = fallbackProcessing(exec, decl, snode);

                } else {
                    child = snode.compile(exec, decl);
                    if (child != null) {
                        if (child.getContainer() == null) {
                            // for the time being, the XSLT stylesheet element acts as the container
                            // for the XPath expressions within. This will later be replaced by a
                            // compiled template, variable, or other top-level construct
                            child.setContainer(this);
                        }
                        child.setSourceLocator(this);
						if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
							child = makeTraceInstruction(snode, child); // no check for includeParams
						}
                    }
                }
                if (child != null) {
                    contents.add(child);
                }
            }
        }
        Expression block = Block.makeBlock(contents);
        block.setSourceLocator(this);
        return block;
    }
    
	/**
	 * Create a trace instruction to wrap a real instruction
     *
     * @param source the parent element
     * @param child  the compiled expression tree for the instruction to be traced
     * @return a wrapper instruction that performs the tracing (if activated at run-time)
	 */

	public static Expression makeTraceInstruction(StyleElement source, Expression child) {
		if (child instanceof TraceExpression && !(source instanceof StylesheetProcedure)) {
			return child;
			// this can happen, for example, after optimizing a compile-time xsl:if
		}
        CodeInjector injector = ((XSLTTraceListener)LogController.getTraceListener()).getCodeInjector();

        StructuredQName construct = source.getNodeName();
        StructuredQName qName;
        if (source instanceof LiteralResultElement) {
            construct = Location.LITERAL_RESULT_ELEMENT;
            qName = source.getNodeName();
        } else {
        	qName = source.getObjectName();
        }
        Expression tracer = injector.inject(child, source.getStaticContext(), construct, qName);
        tracer.setSourceLocator(source);
        return tracer;
	}

    
    

    /**
     * Perform fallback processing. Generate fallback code for an extension
     * instruction that is not recognized by the implementation.
     * @param exec        the Executable
     * @param decl        the Declaration of the top-level element containing the extension instruction
     *@param instruction The unknown extension instruction @return the expression tree representing the fallback code
     */

    protected Expression fallbackProcessing(Executable exec, Declaration decl, StyleElement instruction)
            throws XPathException {
        // process any xsl:fallback children; if there are none,
        // generate code to report the original failure reason
        Expression fallback = null;
        for (NodeImpl child : instruction.allChildren()) {
            if (child instanceof XSLFallback) {
                //fallback.setLocationId(allocateLocationId(getSystemId(), child.getLineNumber()));
                //((XSLFallback)child).compileChildren(exec, fallback, true);
                Expression b = ((XSLFallback)child).compileSequenceConstructor(exec, decl);
                if (b == null) {
                    b = Literal.makeEmptySequence();
                }
                if (fallback == null) {
                    fallback = b;
                } else {
                    fallback = Block.makeBlock(fallback, b);
                    fallback.setSourceLocator(this);
                }
            }
        }
        if (fallback != null) {
            return fallback;
        } else {
            return new ErrorExpression(instruction.validationError);
        }

    }

    /**
     * Construct sort keys for a SortedIterator
     * @return an array of SortKeyDefinition objects if there are any sort keys;
     *         or null if there are none.
     * @param decl
     */

    protected SortKeyDefinition[] makeSortKeys(Declaration decl) throws XPathException {
        // handle sort keys if any

        int numberOfSortKeys = 0;
        for (NodeImpl child : allChildren()) {
            if (child instanceof XSLSort) {
                ((XSLSort)child).compile(getExecutable(), decl);
                if (numberOfSortKeys != 0 && ((XSLSort)child).getStable() != null) {
                    compileError("stable attribute may appear only on the first xsl:sort element", "XTSE1017");
                }
                numberOfSortKeys++;
            }
        }

        if (numberOfSortKeys > 0) {
            SortKeyDefinition[] keys = new SortKeyDefinition[numberOfSortKeys];
            int k = 0;
            for (NodeImpl child : allChildren()) {
                if (child instanceof XSLSort) {
                    keys[k++] = ((XSLSort)child).getSortKeyDefinition().simplify(makeExpressionVisitor());
                }
            }
            return keys;

        } else {
            return null;
        }
    }

    /**
     * Get the list of attribute-sets associated with this element.
     * This is used for xsl:element, xsl:copy, xsl:attribute-set, and on literal
     * result elements
     * @param use  the original value of the [xsl:]use-attribute-sets attribute
     * @param list an empty list to hold the list of XSLAttributeSet elements in the stylesheet tree.
     *             Or null, if these are not required.
     * @return an array of AttributeList instructions representing the compiled attribute sets
     * @throws XPathException if, for example, an attribute set name is an invalid QName
     */

    protected AttributeSet[] getAttributeSets(String use, List<Declaration> list)
            throws XPathException {

        if (list == null) {
            list = new ArrayList<Declaration>(4);
        }
        PrincipalStylesheetModule psm = getPrincipalStylesheetModule();
        for (String asetname : Whitespace.tokenize(use)) {
            StructuredQName name;
            try {
                name = makeQName(asetname);
            } catch (NamespaceException err) {
                compileError(err.getMessage(), "XTSE0710");
                name = null;
            } catch (XPathException err) {
                compileError(err.getMessage(), "XTSE0710");
                name = null;
            }
            boolean found = psm.getAttributeSets(name, list);
            if (!found) {
                compileError("No attribute-set exists named " + asetname, "XTSE0710");
            }
        }
        AttributeSet[] array = new AttributeSet[list.size()];
        for (int i=0; i<list.size(); i++) {
            XSLAttributeSet aset = (XSLAttributeSet)list.get(i).getSourceElement();
            array[i] = aset.getInstruction();
        }
        return array;
    }

    /**
     * Get the list of xsl:with-param elements for a calling element (apply-templates,
     * call-template, apply-imports, next-match). This method can be used to get either
     * the tunnel parameters, or the non-tunnel parameters.
     * @param exec   the Executable
     * @param decl
     *@param tunnel true if the tunnel="yes" parameters are wanted, false to get
     * @param caller the calling instruction (for example xsl:apply-templates), used
 *               only for its location information @return an array of WithParam objects for either the ordinary parameters
     *         or the tunnel parameters
     */

    protected WithParam[] getWithParamInstructions(Executable exec, Declaration decl, boolean tunnel, Expression caller)
            throws XPathException {
        List<WithParam> params = new ArrayList<WithParam>();
        for (NodeImpl child : allChildren()) {
            if (child instanceof XSLWithParam) {
                XSLWithParam wp = (XSLWithParam)child;
                if (wp.isTunnelParam() == tunnel) {
                    WithParam p = (WithParam)wp.compile(exec, decl);
                    ExpressionTool.copyLocationInfo(caller, p);
                    params.add(p);
                }

            }
        }
        WithParam[] array = new WithParam[params.size()];
        return params.toArray(array);
    }

    /**
     * Report an error with diagnostic information
     * @param error contains information about the error
     * @throws XPathException always, after reporting the error to the ErrorListener
     */

    protected void compileError(XPathException error)
            throws XPathException {
        error.setIsStaticError(true);
        if (error.getLocator() == null) {
        	error.setLocator(this);
        }
        Executable pss = getPreparedStylesheet();
        if (pss == null) {
            // it is null before the stylesheet has been fully built
            throw error;
        } else {
            pss.reportError(error);
        }
    }

    /**
     * Report a static error in the stylesheet
     * @param message the error message
     * @throws XPathException always, after reporting the error to the ErrorListener
     */

    protected void compileError(String message)
            throws XPathException {
        XPathException tce = new XPathException(message);
        tce.setLocator(this);
        compileError(tce);
    }

    /**
     * Compile time error, specifying an error code
     * @param message   the error message
     * @param errorCode the error code. May be null if not known or not defined
     * @throws XPathException
     */

    protected void compileError(String message, StructuredQName errorCode) throws XPathException {
        // only use message for debug version of Saxon-CE file-size reduced by approx 6KB
    	XPathException tce;
        if (LogConfiguration.loggingIsEnabled()) {
        	tce = new XPathException(message);
        } else {
        	tce = new XPathException("");
        }
        tce.setErrorCodeQName(errorCode);
        tce.setLocator(this);
        compileError(tce);
    }

    /**
     * Compile time error, specifying an error code
     * @param message   the error message
     * @param errorCode the error code. May be null if not known or not defined
     * @throws XPathException
     */

    protected void compileError(String message, String errorCode) throws XPathException {
    	// only use message for debug version of Saxon-CE file-size reduced by approx 6KB
    	XPathException tce;
    	if (LogConfiguration.loggingIsEnabled()) {
        tce = new XPathException(message);
    	} else {
    		tce = new XPathException("");
    	}
        tce.setErrorCode(errorCode);
        tce.setLocator(this);
        compileError(tce);
    }

    protected void undeclaredNamespaceError(String prefix, String errorCode) throws XPathException {
        if (errorCode == null) {
            errorCode = "XTSE0280";
        }
        compileError("Undeclared namespace prefix " + Err.wrap(prefix), errorCode);
    }

    /**
     * Test whether this is a top-level element
     * @return true if the element is a child of the xsl:stylesheet element
     */

    public boolean isTopLevel() {
        return (getParent() instanceof XSLStylesheet);
    }

    /**
     * Bind a variable used in this element to the compiled form of the XSLVariable element in which it is
     * declared
     * @param qName The name of the variable
     * @return the XSLVariableDeclaration (that is, an xsl:variable or xsl:param instruction) for the variable,
     *         or null if no declaration of the variable can be found
     */

    public XSLVariableDeclaration bindVariable(StructuredQName qName) {
        NodeInfo curr = this;
        NodeInfo prev = this;

        // first search for a local variable declaration
        if (!isTopLevel()) {
            UnfailingIterator preceding = curr.iterateAxis(Axis.PRECEDING_SIBLING, NodeKindTest.ELEMENT);
            while (true) {
                curr = (NodeInfo)preceding.next();
                while (curr == null) {
                    curr = prev.getParent();
                    while (curr instanceof StyleElement && !((StyleElement)curr).seesAvuncularVariables()) {
                        // a local variable is not visible within a sibling xsl:fallback or saxon:catch element
                        curr = curr.getParent();
                    }
                    prev = curr;
                    if (curr.getParent() instanceof XSLStylesheet) {
                        break;   // top level
                    }
                    preceding = curr.iterateAxis(Axis.PRECEDING_SIBLING, NodeKindTest.ELEMENT);
                    curr = (NodeInfo)preceding.next();
                }
                if (curr.getParent() instanceof XSLStylesheet) {
                    break;
                }
                if (curr instanceof XSLVariableDeclaration) {
                    XSLVariableDeclaration var = (XSLVariableDeclaration)curr;
                    if (var.getVariableQName().equals(qName)) {
                        return var;
                    }
                }
            }
        }

        // Now check for a global variable
        // we rely on the search following the order of decreasing import precedence.

        return getPrincipalStylesheetModule().getGlobalVariable(qName);
    }

    /**
     * Ask whether variables declared in an "uncle" element are visible.
     * @return true for all elements except xsl:fallback and saxon:catch
     */

    protected boolean seesAvuncularVariables() {
        return true;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be null.
     * @return the name of the object declared in this element, if any
     */

    public StructuredQName getObjectName() {
        if (objectName == null) {
            try {
                objectName = (StructuredQName)checkAttribute("name", "q");
                if (objectName == null) {
                    objectName = new StructuredQName("saxon", NamespaceConstant.SAXON, "unnamed-" + getLocalPart());
                }
            } catch (XPathException err) {
                objectName = new StructuredQName("saxon", NamespaceConstant.SAXON, "unknown-" + getLocalPart());
            }
        }
        return objectName;
    }

    /**
     * Set the object name, for example the name of a function, variable, or template declared on this element
     * @param qName the object name as a QName
     */

    public void setObjectName(StructuredQName qName) {
        objectName = qName;
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property.
     */

    public Iterator<String> getProperties() {
        List<String> list = new ArrayList<String>(10);
        UnfailingIterator it = iterateAxis(Axis.ATTRIBUTE, AnyNodeTest.getInstance());
        while (true) {
            NodeInfo a = (NodeInfo)it.next();
            if (a == null) {
                break;
            }
            list.add(a.getNodeName().getClarkName());
        }
        return list.iterator();
    }

    /**
     * Ask if an action on this StyleElement has been completed
     * @param action for example ACTION_VALIDATE
     * @return true if the action has already been performed
     */

    public boolean isActionCompleted(int action) {
        return (actionsCompleted & action) != 0;
    }

    /**
     * Say that an action on this StyleElement has been completed
     * @param action for example ACTION_VALIDATE
     */

    public void setActionCompleted(int action) {
        actionsCompleted |= action;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
