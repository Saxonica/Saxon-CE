package client.net.sf.saxon.ce.pattern;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
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


    /**
    * Static factory method to make a Pattern by parsing a String. <br>
    * @param pattern The pattern text as a String
    * @param env An object defining the compile-time context for the expression
    * @param container The stylesheet element containing this pattern
    * @return The pattern object
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
     * Get the executable containing this pattern
     * @return the executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the executable containing this pattern
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
        // no action by default
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
	 * Set the original text of the pattern for use in diagnostics
     * @param text the original text of the pattern
	 */

	public void setOriginalText(String text) {
		originalText = text;
	}

    /**
     * Simplify the pattern by applying any context-independent optimisations.
     * Default implementation does nothing.
     * @return the optimised Pattern
     * @param visitor the expression visitor
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
     * Type-check the pattern.
     * @param visitor the expression visitor
     * @param contextItemType the type of the context item at the point where the pattern
     * is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
    */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        return 0;
    }

    /**
     * Iterate over the subexpressions within this pattern
     * @return an iterator over the subexpressions. Default implementation returns an empty sequence
     */

    public Iterator iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

   /**
     * Allocate slots to any variables used within the pattern
    * @param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
    */

    public int allocateSlots(int nextFree) {
        return nextFree;
    }

    /**
     * If the pattern contains any calls on current(), this method is called to modify such calls
     * to become variable references to a variable declared in a specially-allocated local variable
     *
     * @param let   the expression that assigns the local variable. This returns a dummy result, and is executed
     *              just before evaluating the pattern, to get the value of the context item into the variable.
     * @param offer A PromotionOffer used to process the expressions and change the call on current() into
     *              a variable reference
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
     *
     * <p>Unlike the corresponding method on {@link Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        // default implementation does nothing
    }

    /**
     * Set the system ID where the pattern occurred
     * @param systemId the URI of the module containing the pattern
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Determine whether this Pattern matches the given Node. This is the main external interface
    * for matching patterns: it sets current() to the node being tested
    * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
    * @param context The dynamic context. Only relevant if the pattern
    * uses variables, or contains calls on functions such as document() or key().
    * @return true if the node matches the Pattern, false otherwise
    */

    public abstract boolean matches(NodeInfo node, XPathContext context) throws XPathException;

    /**
     * Determine whether this pattern matches a given Node within the subtree rooted at a given
     * anchor node. This method is used when the pattern is used for streaming.
     * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor The anchor node, which must match any AnchorPattern subpattern
     * @param context The dynamic context. Only relevant if the pattern
     * uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        return matches(node, context);
    }

    /**
    * Determine whether this Pattern matches the given Node. This is an internal interface used
    * for matching sub-patterns; it does not alter the value of current(). The default implementation
    * is identical to matches().
    * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
    * @param anchor
     *@param context The dynamic context. Only relevant if the pattern
     * uses variables, or contains calls on functions such as document() or key(). @return true if the node matches the Pattern, false otherwise
    */

    protected boolean internalMatches(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        return matches(node, context);
    }

   /**
     * Select nodes in a document using this PatternFinder.
     * @param doc the document node at the root of a tree
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
                        return ((NodeInfo)item).iterateAxis(Axis.ATTRIBUTE, AnyNodeTest.getInstance());
                    }
                };
                SequenceIterator allAttributes = new MappingIterator(allElements, atts);
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if ((matches((NodeInfo)item, context))) {
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
                UnfailingIterator allChildren = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.makeNodeKindTest(kind));
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if ((matches((NodeInfo)item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(allChildren, test);
            }
            case Type.NODE: {
                UnfailingIterator allChildren = doc.iterateAxis(Axis.DESCENDANT, AnyNodeTest.getInstance());
                MappingFunction attsOrSelf = new MappingFunction() {
                    public SequenceIterator map(Item item) {
                        return new PrependIterator((NodeInfo)item, ((NodeInfo)item).iterateAxis(Axis.ATTRIBUTE, AnyNodeTest.getInstance()));
                    }
                };
                SequenceIterator attributesOrSelf = new MappingIterator(allChildren, attsOrSelf);
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if ((matches((NodeInfo)item, context))) {
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
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getNodeKind() {
        return Type.NODE;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     * @return a NodeTest, as specific as possible, which all the matching nodes satisfy
    */

    public abstract NodeTest getNodeTest();

    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
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
