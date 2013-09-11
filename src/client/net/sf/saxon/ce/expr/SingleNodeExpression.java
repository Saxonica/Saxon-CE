package client.net.sf.saxon.ce.expr;
import com.google.gwt.logging.client.LogConfiguration;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;


/**
* A node set expression that will always return zero or one nodes
*/

public abstract class SingleNodeExpression extends Expression {

    /**
    * Type-check the expression.
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

    	if (contextItemType == null || contextItemType.isAtomicType()) {
    		String message = "";
    		String code = "";

        	if (LogConfiguration.loggingIsEnabled()) {
    	        if (contextItemType == null) {
    	        	code = "XPDY0002";
    	            message = noContextMessage() + ": the context item is undefined";
    	        } else if (contextItemType.isAtomicType()) {
    	        	code = "XPTY0020";    	        	
    	            message = noContextMessage() + ": the context item is an atomic value";
    	        }
        	}
    		typeError(visitor, message, code); // not reported if logging not enabled, but still throws exception
    	}
        return this;
    }
    

    


    /**
     * Customize the error message on type checking
     */

    protected abstract String noContextMessage();

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        // repeat the check: in XSLT insufficient information is available the first time
        return typeCheck(visitor, contextItemType);
    }


    /**
    * Specify that the expression returns a singleton
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_ONE;
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return Type.NODE
     */

    public ItemType getItemType() {
        return AnyNodeTest.getInstance();
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.CONTEXT_DOCUMENT_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
    }

    /**
    * Get the single node to which this expression refers. Returns null if the node-set is empty
    */

    public abstract NodeInfo getNode(XPathContext context) throws XPathException;

    /**
    * Evaluate the expression in a given context to return an iterator
    * @param context the evaluation context
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return SingletonIterator.makeIterator(getNode(context));
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        return getNode(context);
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
