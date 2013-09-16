package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.StringFn;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.util.Orphan;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Cardinality;

import java.util.Iterator;

/**
 * Common superclass for XSLT instructions whose content template produces a text
 * value: xsl:attribute, xsl:comment, xsl:processing-instruction, xsl:namespace,
 * and xsl:text, and their XQuery equivalents
 */

public abstract class SimpleNodeConstructor extends Instruction {

    // The select expression is adjusted to return xs:string?
    // If the select expresion returns empty, then the node constructor returns empty.

    protected Expression select = null;

    /**
     * Default constructor used by subclasses
     */

    public SimpleNodeConstructor() {
    }

    /**
     * Set the select expression: the value of this expression determines the string-value of the node
     * @param select the expression that computes the string value of the node
     * @param config the Saxon configuration (used for example to do early validation of the content
     * of an attribute against the schema-defined type)
     * @throws XPathException
     */

    public void setSelect(Expression select, Configuration config) throws XPathException {
        this.select = select;
        adoptChildExpression(select);
    }

    /**
     * Get the expression that determines the string value of the constructed node
     * @return the select expression
     */

    public Expression getContentExpression() {
        return select;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     * @return the static cardinality
     */

    public int computeCardinality() {
        return select.getCardinality(); // may allow empty sequence
    }        

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        return this;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeSpecialProperties() {
        return super.computeSpecialProperties() |
                StaticProperty.SINGLE_DOCUMENT_NODESET;
    }

    /**
     * Method to perform type-checking specific to the kind of instruction
     * @param visitor an expression visitor
     * @param contextItemType the static type of the context item
     * @throws XPathException
     */

    public abstract void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException;

    /**
     * The typeCheck() method is called in XQuery, where node constructors
     * are implemented as Expressions. In this case the required type for the
     * select expression is a single string.
     * @param visitor an expression visitor
     * @return the rewritten expression
     * @throws XPathException if any static errors are found in this expression
     * or any of its children
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        localTypeCheck(visitor, contextItemType);

        if (select != null) {
            final TypeHierarchy th = TypeHierarchy.getInstance();
            select = visitor.typeCheck(select, contextItemType);
            if (select instanceof ValueOf) {
                Expression valSelect = ((ValueOf)select).getContentExpression();
                if (th.isSubType(valSelect.getItemType(), AtomicType.STRING) &&
                        !Cardinality.allowsMany(valSelect.getCardinality())) {
                    select = valSelect;
                }
            }

            // Don't bother converting untypedAtomic to string
            if (select instanceof StringFn) {
                StringFn fn = (StringFn)select;
                Expression arg = fn.getArguments()[0];
                if (arg.getItemType() == AtomicType.UNTYPED_ATOMIC && !Cardinality.allowsMany(arg.getCardinality())) {
                    select = arg;    
                }
            } else if (select instanceof CastExpression && ((CastExpression)select).getTargetType() == AtomicType.STRING) {
                Expression arg = ((CastExpression)select).getBaseExpression();
                if (arg.getItemType() == AtomicType.UNTYPED_ATOMIC && !Cardinality.allowsMany(arg.getCardinality())) {
                    select = arg;
                }
            }
            adoptChildExpression(select);
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        return this;
    }

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(select);
    }

    /**
    * Process this instruction
    * @param context the dynamic context of the transformation
    * @return a TailCall to be executed by the caller, always null for this instruction
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        CharSequence value = select.evaluateAsString(context);
        processValue(value, context);
        return null;
    }


    /**
     * Process the value of the node, to create the new node.
     * @param value the string value of the new node
     * @param context the dynamic evaluation context
     * @throws XPathException
     */

    public abstract void processValue(CharSequence value, XPathContext context) throws XPathException;

    /**
     * Evaluate as an expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item contentItem = select.evaluateItem(context);
        if (contentItem == null) {
            return null;
        }
        String content = contentItem.getStringValue();
        content = checkContent(content, context);
        final TypeHierarchy th = TypeHierarchy.getInstance();
        Orphan o = new Orphan();
        o.setNodeKind(((NodeTest)getItemType()).getRequiredNodeKind());
        o.setStringValue(content);
        o.setNodeName(evaluateNameCode(context));
        return o; 
    }

    /**
     * Check the content of the node, and adjust it if necessary. The checks depend on the node kind.
     * @param data the supplied content
     * @param context the dynamic context
     * @return the original content, unless adjustments are needed
     * @throws XPathException if the content is invalid
     */

    protected String checkContent(String data, XPathContext context) throws XPathException {
        return data;
    }

    /**
     * Run-time method to compute the name of the node being constructed. This is overridden
     * for nodes that have a name. The default implementation returns -1, which is suitable for
     * unnamed nodes such as comments
     *
     * @param context the XPath dynamic evaluation context
     * @return the name pool nameCode identifying the name of the constructed node
     * @throws XPathException if any failure occurs
     */

    public StructuredQName evaluateNameCode(XPathContext context) throws XPathException {
        return null;
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return SingletonIterator.makeIterator(evaluateItem(context));
    }

    /**
      * Offer promotion for subexpressions. The offer will be accepted if the subexpression
      * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
      * By default the offer is not accepted - this is appropriate in the case of simple expressions
      * such as constant values and variable references where promotion would give no performance
      * advantage. This method is always called at compile time.
      *
      * @param offer details of the offer, for example the offer to move
      *     expressions that don't depend on the context to an outer level in
      *     the containing expression
      * @exception XPathException if any error is detected
      */

     protected void promoteInst(PromotionOffer offer) throws XPathException {
         if (select != null) {
             select = doPromotion(select, offer);
         }
         super.promoteInst(offer);
     }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
