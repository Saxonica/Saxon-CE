package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.*;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;


/**
 * An instruction that creates an element node. There are two subtypes, FixedElement
 * for use where the name is known statically, and Element where it is computed
 * dynamically. To allow use in both XSLT and XQuery, the class acts both as an
 * Instruction and as an Expression.
*/

public abstract class ElementCreator extends ParentNodeConstructor {

    /**
     * The inheritNamespaces flag indicates that the namespace nodes on the element created by this instruction
     * are to be inherited (copied) on the children of this element. That is, if this flag is false, the child
     * elements must carry a namespace undeclaration for all the namespaces on the parent, unless they are
     * redeclared in some way.
     */

    protected boolean inheritNamespaces = true;

    /**
     * Construct an ElementCreator. Exists for the benefit of subclasses.
     */

    public ElementCreator() { }

    /**
     * Get the item type of the value returned by this instruction
     * @return the item type
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.ELEMENT;
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
     * Determine (at run-time) the name code of the element being constructed
     * @param context the XPath dynamic evaluation context
     * @param copiedNode
     * @return the integer name code representing the element name
     * @throws XPathException if a failure occurs
     */

    public abstract int getNameCode(XPathContext context, NodeInfo copiedNode)
    throws XPathException;

    /**
     * Get the base URI for the element being constructed
     * @param context the XPath dynamic evaluation context
     * @param copiedNode the node being copied (for xsl:copy), otherwise null
     * @return the base URI of the constructed element
     */

    protected abstract String getNewBaseURI(XPathContext context, NodeInfo copiedNode);

    /**
     * Callback to output namespace nodes for the new element. This method is responsible
     * for ensuring that a namespace node is always generated for the namespace of the element
     * name itself.
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @param nameCode the name code of the element being created
     * @param copiedNode the node being copied (for xsl:copy) or null otherwise
     * @throws client.net.sf.saxon.ce.trans.XPathException
     */

    protected abstract void outputNamespaceNodes(XPathContext context, Receiver receiver, int nameCode, NodeInfo copiedNode)
    throws XPathException;

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.EVALUATE_METHOD;
    }

    /**
     * Evaluate the instruction to produce a new element node. This method is typically used when there is
     * a parent element or document in a result tree, to which the new element is added.
     * @param context XPath dynamic evaluation context
     * @return null (this instruction never returns a tail call)
     * @throws XPathException
     */
    public TailCall processLeavingTail(XPathContext context)
    throws XPathException {
        return processLeavingTail(context, null);
    }

    /**
     * Evaluate the instruction to produce a new element node. This method is typically used when there is
     * a parent element or document in a result tree, to which the new element is added.
     * @param context XPath dynamic evaluation context
     * @param copiedNode null except in the case of xsl:copy, when it is the node being copied
     * @return null (this instruction never returns a tail call)
     * @throws XPathException
     */
    protected final TailCall processLeavingTail(XPathContext context, NodeInfo copiedNode)
    throws XPathException {

        try {

            int nameCode = getNameCode(context, copiedNode);

            SequenceReceiver out = context.getReceiver();

            if (out.getSystemId() == null) {
                out.setSystemId(getNewBaseURI(context, copiedNode));
            }
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            out.startElement(nameCode, properties);

            // output the required namespace nodes via a callback

            outputNamespaceNodes(context, out, nameCode, copiedNode);

            // process subordinate instructions to generate attributes and content
            content.process(context);

            // output the element end tag (which will fail if validation fails)
            out.endElement();
            return null;

        } catch (XPathException e) {
            e.maybeSetLocation(getSourceLocator());
            e.maybeSetContext(context);
            throw e;
        }
    }

    /**
     * Evaluate the constructor, returning the constructed element node. If lazy construction
     * mode is in effect, then an UnconstructedParent object is returned instead.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return constructElement(context, null);
    }

    /**
     * Construct the element node as a free-standing (parentless) node in a tiny tree
     * @param context XPath dynamic evaluation context
     * @return the constructed element node
     * @throws XPathException
     */
    private NodeInfo constructElement(XPathContext context, NodeInfo copiedNode) throws XPathException {
        try {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            SequenceOutputter seq = controller.allocateSequenceOutputter(1);
            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            seq.setPipelineConfiguration(pipe);

            int nameCode = getNameCode(c2, copiedNode);

            c2.setTemporaryReceiver(seq);
            if (seq.getSystemId() == null) {
                seq.setSystemId(getNewBaseURI(c2, copiedNode));
            }

            seq.open();
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            seq.startElement(nameCode, properties);

            // output the namespace nodes for the new element
            outputNamespaceNodes(c2, seq, nameCode, null);

            content.process(c2);

            seq.endElement();
            seq.close();

            // the constructed element is the first and only item in the sequence
            NodeInfo result = (NodeInfo)seq.popLastItem();
            seq.reset();
            return result;

        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            err.maybeSetContext(context);
            throw err;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
