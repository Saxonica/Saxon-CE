package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.sort.DocumentSorter;
import client.net.sf.saxon.ce.js.IXSLFunction;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.style.StyleElement;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.PrependIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.SourceLocator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;

import java.util.Collections;
import java.util.Iterator;

/**
 * A Pattern represents the result of parsing an XSLT pattern string. <br>
 * Patterns are created by calling the static method Pattern.make(string). <br>
 * The pattern is used to test a particular node by calling match().
 */

public abstract class Pattern implements Container, SourceLocator {

    private String originalText;
    private Executable executable;
    private String systemId;      // the module where the pattern occurred
    private Expression variableBinding = null;      // local variable to which the current() node is bound


    /**
     * Static factory method to make a Pattern by parsing a String. <br>
     *
     * @param pattern   The pattern text as a String
     * @param env       An object defining the compile-time context for the expression
     * @param container The stylesheet element containing this pattern
     * @return The pattern object
     * @throws XPathException if the pattern cannot be converted
     */

    public static Pattern make(String pattern, StaticContext env, StyleElement container) throws XPathException {

        PatternParser parser = new PatternParser();
        parser.setLanguage(ExpressionParser.XSLT_PATTERN);
        parser.setDefaultContainer(container);
        Pattern pat = parser.parsePattern(pattern, env);
        pat.setSystemId(env.getSystemId());
        // System.err.println("Simplified [" + pattern + "] to " + pat.getClass() + " default prio = " + pat.getDefaultPriority());
        // set the pattern text for use in diagnostics
        pat.setOriginalText(pattern);
        pat.setExecutable(container.getExecutable());
        ExpressionVisitor visitor = ExpressionVisitor.make(env, container.getExecutable());
        pat = pat.simplify(visitor);
        return pat;
    }

    /**
     * Static factory method to make a pattern by converting an expression. The supplied
     * expression is the equivalent expression to the pattern, in the sense that it takes
     * the same syntactic form.
     *
     * <p>Note that this method does NOT check all the rules for XSLT patterns; it deliberately allows
     * a (slightly) wider class of expressions to be converted than XSLT allows.</p>
     *
     * <p>The expression root() at the start of the expression has a special meaning: it refers to
     * the root of the subtree in which the pattern must match, which can be supplied at run-time
     * during pattern matching. This is used for patterns that represent streamable path expressions.</p>
     *
     * @param expression the expression to be converted
     * @param config the Saxon configuration
     * @throws XPathException if the expression cannot be converted
     */

    public static Pattern fromExpression(Expression expression, Configuration config) throws XPathException {
        Pattern result = null;
        if (expression instanceof DocumentSorter) {
            expression = ((DocumentSorter)expression).getBaseExpression();
        }
        if (expression instanceof VennExpression && ((VennExpression)expression).getOperator() == Token.UNION) {
            result = new UnionPattern(
                    fromExpression(((VennExpression)expression).getOperands()[0], config),
                    fromExpression(((VennExpression)expression).getOperands()[1], config));
        } else if (expression instanceof AxisExpression) {
            int axis = ((AxisExpression)expression).getAxis();
            NodeTest test = ((AxisExpression)expression).getNodeTest();

            if (test == null) {
                test = AnyNodeTest.getInstance();
            }
            if (test instanceof AnyNodeTest && (axis == Axis.CHILD || axis == Axis.DESCENDANT)) {
                test = AnyChildNodeTest.getInstance();
            }
            int kind = test.getRequiredNodeKind();
            if (axis == Axis.SELF && kind == Type.DOCUMENT) {
                result = new NodeTestPattern(test);
            } else if (axis == Axis.ATTRIBUTE) {
                if (kind == Type.NODE) {
                    // attribute::node() matches any attribute, and only an attribute
                    result = new NodeTestPattern(NodeKindTest.ATTRIBUTE);
                } else if (!Axis.containsNodeKind(axis, kind)) {
                    // for example, attribute::comment()
                    result = new NodeTestPattern(EmptySequenceTest.getInstance());
                } else {
                    result = new NodeTestPattern(test);
                }
            } else if (axis == Axis.CHILD || axis == Axis.DESCENDANT || axis == Axis.DESCENDANT_OR_SELF) {
                if (kind != Type.NODE && !Axis.containsNodeKind(axis, kind)) {
                     test = EmptySequenceTest.getInstance();
                }
                result = new NodeTestPattern(test);
            } else {
                throw new XPathException("Only downwards axes are allowed in a pattern", "XTSE0340");
            }
            // TODO: //A only matches an A element in a tree rooted at a document
        } else if (expression instanceof FilterExpression) {
            Expression base = ((FilterExpression)expression).getControllingExpression();
            Expression filter = ((FilterExpression)expression).getFilter();
            Pattern basePattern = fromExpression(base, config);
            if (basePattern instanceof NodeTestPattern) {
                LocationPathPattern path = new LocationPathPattern();
                path.setNodeTest(basePattern.getNodeTest());
                basePattern = path;
            }
            if (!(basePattern instanceof LocationPathPattern)) {
                throw new XPathException("The filtered expression in a pattern must be a simple step");
            }
            ((LocationPathPattern)basePattern).addFilter(filter);
            result = basePattern;
        } else if (expression instanceof SlashExpression) {
            Expression head = ((SlashExpression)expression).getLeadingSteps();
            Expression tail =  ((SlashExpression)expression).getLastStep();
            Pattern tailPattern = fromExpression(tail, config);
            if (tailPattern instanceof NodeTestPattern) {
                LocationPathPattern path = new LocationPathPattern();
                path.setNodeTest(tailPattern.getNodeTest());
                tailPattern = path;
            }
            if (!(tailPattern instanceof LocationPathPattern)) {
                throw new XPathException("The path in a pattern must contain simple steps: found " + tailPattern.toString());
            }
            if (((LocationPathPattern)tailPattern).getUpperPattern() != null) {
                throw new XPathException("The path in a pattern must contain simple steps");
            }
            byte axis = getAxisForPathStep(tail);
            Pattern headPattern = fromExpression(head, config);
            ((LocationPathPattern)tailPattern).setUpperPattern(axis, headPattern);
            result = tailPattern;
        } else if (expression instanceof RootExpression) {
            result = new NodeTestPattern(NodeKindTest.DOCUMENT);
        } else if (expression instanceof IXSLFunction) {
        	result = new JSObjectPattern(expression, config);
        } else {
            ItemType type = expression.getItemType();
            if (((expression.getDependencies() & StaticProperty.DEPENDS_ON_NON_DOCUMENT_FOCUS) == 0) &&
                    (type instanceof NodeTest || expression instanceof VariableReference)) {
                result = new NodeSetPattern(expression);
            }
        }
        if (result == null) {
            throw new XPathException("Cannot convert the expression {" + expression.toString() + "} to a pattern");
        } else {
            result.setOriginalText(expression.toString());
            return result;
        }
    }

    private static byte getAxisForPathStep(Expression step) throws XPathException {
        if (step instanceof AxisExpression) {
            return Axis.inverseAxis[((AxisExpression)step).getAxis()];
        } else if (step instanceof FilterExpression) {
            return getAxisForPathStep(((FilterExpression)step).getControllingExpression());
        } else if (step instanceof PathExpression) {
            return getAxisForPathStep(((PathExpression)step).getFirstStep());
        } else if (step instanceof ContextItemExpression) {
            return Axis.SELF;
        } else {
            throw new XPathException("The path in a pattern must contain simple steps");
        }
    }


    /**
     * Get the executable containing this pattern
     *
     * @return the executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the executable containing this pattern
     *
     * @param executable the executable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
    * Set an expression used to bind the variable that represents the value of the current() function
    * @param exp the expression that binds the variable
    */

   public void setVariableBindingExpression(Expression exp) {
       variableBinding = exp;
   }

   public Expression getVariableBindingExpression() {
       return variableBinding;
   }

    protected void bindCurrent(NodeInfo node, XPathContext context) {
        if (variableBinding != null) {
            XPathContext c2 = context;
            Item ci = context.getContextItem();
            if (!(ci instanceof NodeInfo && ((NodeInfo)ci).isSameNodeInfo(node))) {
                c2 = context.newContext();
                c2.setSingletonFocus(node);
            }
            try {
                variableBinding.evaluateItem(c2);
            } catch (XPathException e) {
                return; // errors in patterns are recoverable (and we don't expect a failure here anyway)
            }
        }
    }

    /**
     * Get the granularity of the container.
     *
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 1;
    }

    /**
     * Set the original text of the pattern for use in diagnostics
     *
     * @param text the original text of the pattern
     */

    public void setOriginalText(String text) {
        originalText = text;
    }

    /**
     * Simplify the pattern by applying any context-independent optimisations.
     * Default implementation does nothing.
     *
     * @param visitor the expression visitor
     * @return the optimised Pattern
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
     * Type-check the pattern.
     *
     * @param visitor         the expression visitor
     * @param contextItemType the type of the context item at the point where the pattern
     *                        is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     *
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        return 0;
    }

    /**
     * Iterate over the subexpressions within this pattern
     *
     * @return an iterator over the subexpressions. Default implementation returns an empty sequence
     */

    public Iterator<Expression> iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(int nextFree) {
        return nextFree;
    }

    /**
     * If the pattern contains any calls on current(), this method is called to modify such calls
     * to become variable references to a variable declared in a specially-allocated local variable
     *
     * @param let      the expression that assigns the local variable. This returns a dummy result, and is executed
     *                 just before evaluating the pattern, to get the value of the context item into the variable.
     * @param offer    A PromotionOffer used to process the expressions and change the call on current() into
     *                 a variable reference
     * @param topLevel
     * @throws XPathException
     */

    public void resolveCurrent(LetExpression let, PromotionOffer offer, boolean topLevel) throws XPathException {
        // implemented in subclasses
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @param parent
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        // default implementation does nothing
    }

    /**
     * Set the system ID where the pattern occurred
     *
     * @param systemId the URI of the module containing the pattern
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Determine whether this Pattern matches the given Node. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
     *
     *
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise. A dynamic error while matching the
     * pattern is treated as recoverable, and causes the result to be returned as false.
     */

    public abstract boolean matches(NodeInfo node, XPathContext context);

    /**
     * Determine whether this Pattern matches the given Node. This is an internal interface used
     * for matching sub-patterns; it does not alter the value of current(). The default implementation
     * is identical to matches().
     *
     *
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise. A dynamic error while matching the
     * pattern is treated as recoverable, and causes the result to be returned as false.
     */

    protected boolean internalMatches(NodeInfo node, NodeInfo anchor, XPathContext context) {
        return matches(node, context);
    }

    /**
     * Select nodes in a document that match this Pattern.
     *
     * @param doc     the document node at the root of a tree
     * @param context the dynamic evaluation context
     * @return an iterator over the selected nodes in the document.
     */

    public SequenceIterator selectNodes(DocumentInfo doc, final XPathContext context) throws XPathException {
        final int kind = getNodeKind();
        switch (kind) {
            case Type.DOCUMENT:
                if (matches(doc, context)) {
                    return SingletonIterator.makeIterator(doc);
                } else {
                    return EmptyIterator.getInstance();
                }
            case Type.ATTRIBUTE: {
                UnfailingIterator allElements = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
                MappingFunction atts = new MappingFunction() {
                    public SequenceIterator map(Item item) {
                        return ((NodeInfo) item).iterateAxis(Axis.ATTRIBUTE, AnyNodeTest.getInstance());
                    }
                };
                SequenceIterator allAttributes = new MappingIterator(allElements, atts);
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if ((matches((NodeInfo) item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(allAttributes, test);
            }
            case Type.ELEMENT:
            case Type.COMMENT:
            case Type.TEXT:
            case Type.PROCESSING_INSTRUCTION: {
                UnfailingIterator allDescendants = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.makeNodeKindTest(kind));
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if ((matches((NodeInfo) item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(allDescendants, test);
            }
            case Type.NODE: {
                UnfailingIterator allChildren = doc.iterateAxis(Axis.DESCENDANT, AnyNodeTest.getInstance());
                MappingFunction attsOrSelf = new MappingFunction() {
                    public SequenceIterator map(Item item) {
                        return new PrependIterator((NodeInfo) item, ((NodeInfo) item).iterateAxis(Axis.ATTRIBUTE, AnyNodeTest.getInstance()));
                    }
                };
                SequenceIterator attributesOrSelf = new MappingIterator(allChildren, attsOrSelf);
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if ((matches((NodeInfo) item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(attributesOrSelf, test);
            }
            case Type.NAMESPACE:
                throw new UnsupportedOperationException("Patterns can't match namespace nodes");
            default:
                throw new UnsupportedOperationException("Unknown node kind");
        }
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Type.NODE
     *
     * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
     */

    public int getNodeKind() {
        return Type.NODE;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     *
     * @return a NodeTest, as specific as possible, which all the matching nodes satisfy
     */

    public abstract NodeTest getNodeTest();

    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
     *
     * @return the default priority for the pattern
     */

    public double getDefaultPriority() {
        return 0.5;
    }

    /**
     * Get the system id of the entity in which the pattern occurred
     */

    public String getSystemId() {
        return systemId;
    }

    public String getLocation() {
        return "pattern " + originalText + " in " + getSystemId();
    }

    public SourceLocator getSourceLocator() {
        return this;
    }

    /**
     * Get the original pattern text
     */

    public String toString() {
        if (originalText != null) {
            return originalText;
        } else {
            return "pattern matching " + getNodeTest().toString();
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
