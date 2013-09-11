package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.*;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.NamespaceIterator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.EmptySequence;

import java.util.Arrays;
import java.util.Iterator;

/**
* Handler for xsl:copy elements in stylesheet.
*/

public class Copy extends ElementCreator {

    private boolean copyNamespaces;
    private ItemType resultItemType;
    private Expression select;

    /**
     * Create a shallow copy instruction
     * @param select selects the node (or other item) to be copied
     * @param copyNamespaces true if namespace nodes are to be copied when copying an element
     * @param inheritNamespaces true if child elements are to inherit the namespace nodes of their parent
     */

    public Copy(Expression select,
                boolean copyNamespaces,
                boolean inheritNamespaces) {
        this.copyNamespaces = copyNamespaces;
        this.inheritNamespaces = inheritNamespaces;
        this.select = select;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during expression rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        return super.simplify(visitor);
    }


    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        try {
            select = visitor.typeCheck(select, contextItemType);
            adoptChildExpression(select);
        } catch (XPathException err) {
            if (err.getErrorCodeLocalPart().equals("XPDY0002")) {
                // See spec bug 7624, test case copy903err
                err.setErrorCode("XTTE0945");
                err.maybeSetLocation(getSourceLocator());
            }
            select = new Literal(EmptySequence.getInstance()); // to prevent duplicate error reporting
            throw err;
        }
        ItemType selectItemType = select.getItemType();

        if (selectItemType instanceof NodeTest) {
            switch (((NodeTest)selectItemType).getRequiredNodeKind()) {
                // For elements and attributes, assume the type annotation will change
                case Type.ELEMENT:
                    this.resultItemType = NodeKindTest.ELEMENT;
                    break;
                case Type.ATTRIBUTE:
                    this.resultItemType = NodeKindTest.ATTRIBUTE;
                    break;
                case Type.DOCUMENT:
                    this.resultItemType = NodeKindTest.DOCUMENT;
                    break;
                default:
                    this.resultItemType = selectItemType;
            }
        } else {
            this.resultItemType = selectItemType;
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     * @return a set of bit-significant flags identifying the dependencies of
     *         the expression
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

     /**
      *  Get the immediate sub-expressions of this expression.
      * @return an iterator containing the sub-expressions of this expression
      */

    public Iterator<Expression> iterateSubExpressions() {
        return Arrays.asList((new Expression[]{select, content})).iterator();
    }

    /**
     * Get the item type of the result of this instruction.
     * @return The context item type.
     */

    public ItemType getItemType() {
        if (resultItemType != null) {
            return resultItemType;
        } else {
            resultItemType = computeItemType();
            return resultItemType;
        }
    }

    private ItemType computeItemType() {
        return select.getItemType();
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp == this) {
            if (resultItemType == null) {
                resultItemType = computeItemType();
            }
            if (resultItemType.isAtomicType()) {
                return select;
            }
        }
        return exp;
    }

    /**
     * Evaluate as an expression. We rely on the fact that when these instructions
     * are generated by XQuery, there will always be a valueExpression to evaluate
     * the content
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        SequenceOutputter seq = controller.allocateSequenceOutputter(1);
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        seq.setPipelineConfiguration(pipe);
        c2.setTemporaryReceiver(seq);
        process(c2);
        seq.close();
        Item item = seq.getFirstItem();
        seq.reset();
        return item;
    }


    /**
     * Callback from ElementCreator when constructing an element
     *
     * @param context XPath dynamic evaluation context
     * @param copiedNode the node being copied
     * @return the namecode of the element to be constructed
     * @throws XPathException
     */

    public StructuredQName getNameCode(XPathContext context, NodeInfo copiedNode)
            throws XPathException {
        return copiedNode.getNodeName();
    }

    /**
     * Get the base URI of a copied element node (the base URI is retained in the new copy)
     * @param context XPath dynamic evaluation context
     * @param copiedNode
     * @return the base URI
     */

    public String getNewBaseURI(XPathContext context, NodeInfo copiedNode) {
        return copiedNode.getBaseURI();
    }

    /**
     * Callback to output namespace nodes for the new element.
     *
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @param nameCode
     * @param copiedNode
     * @throws XPathException
     */

    protected void outputNamespaceNodes(XPathContext context, Receiver receiver, StructuredQName nameCode, NodeInfo copiedNode)
    throws XPathException {
        if (copyNamespaces) {
            NamespaceIterator.sendNamespaces(copiedNode, receiver);
        } else {
            // Always output the namespace of the element name itself
            receiver.namespace(new NamespaceBinding(nameCode.getPrefix(), nameCode.getNamespaceURI()), 0);
        }
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        Item item = select.evaluateItem(context);
        if (!(item instanceof NodeInfo)) {
            out.append(item, NodeInfo.ALL_NAMESPACES);
            return null;
        }
        NodeInfo source = (NodeInfo)item;
        //out.getPipelineConfiguration().setBaseURI(source.getBaseURI());

        // Processing depends on the node kind.

        switch(source.getNodeKind()) {

        case Type.ELEMENT:
            // use the generic code for creating new elements
            return super.processLeavingTail(context, (NodeInfo)item);

        case Type.ATTRIBUTE:
            try {
                context.getReceiver().attribute(source.getNodeName(), source.getStringValue());
            } catch (NoOpenStartTagException err) {
                throw dynamicError(getSourceLocator(), err, context);
            }
            break;

        case Type.TEXT:
            out.characters(source.getStringValue());
            break;

        case Type.PROCESSING_INSTRUCTION:
            out.processingInstruction(source.getDisplayName(), source.getStringValue());
            break;

        case Type.COMMENT:
            out.comment(source.getStringValue());
            break;

        case Type.NAMESPACE:
            try {
                source.copy(out, 0);
            } catch (NoOpenStartTagException err) {
                dynamicError(err.getMessage(), err.getErrorCodeLocalPart());
            }
            break;

        case Type.DOCUMENT:
            out.startDocument();
            content.process(context);
            out.endDocument();
            break;

        default:
            throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());

        }
        return null;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
