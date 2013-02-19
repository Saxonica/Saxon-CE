package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.sort.DocumentOrderIterator;
import client.net.sf.saxon.ce.expr.sort.LocalOrderComparer;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.ListIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.tree.util.StringTokenizer;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.ArrayList;
import java.util.List;


/**
* The XPath id() or element-with-id() function (synonymous for a non-schema-aware processor)
* XPath 2.0 version: accepts any sequence as the first parameter; each item in the sequence
* is taken as an IDREFS value, that is, a space-separated list of ID values.
 * Also accepts an optional second argument to identify the target document, this
 * defaults to the context node.
*/


public class Id extends SystemFunction {

    public Id newInstance() {
        return new Id();
    }

    /**
    * Simplify: add a second implicit argument, the context document
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Id id = (Id)super.simplify(visitor);
        if (argument.length == 1) {
            id.addContextDocumentArgument(1, getFunctionName().getLocalName());
        }
        return id;
    }

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (argument[1] instanceof RootExpression && contextItemType != null && contextItemType.isAtomicType()) {
            // intercept this to get better diagnostics
            typeError(getFunctionName().getLocalName() +
                    "() function called when the context item is not a node", "XPTY0004", null);
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int prop = StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
        if ((getNumberOfArguments() == 1) ||
                (argument[1].getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            prop |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        return prop;
    }


    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        NodeInfo arg1;
        try {
            arg1 = (NodeInfo)argument[1].evaluateItem(context);
        } catch (XPathException e) {
            if (context.getContextItem() instanceof AtomicValue) {
                // Override the unhelpful message that trickles down..
                dynamicError("For the " + getFunctionName().getLocalName() +
                        "() function, the context item is not a node", "XPTY0004", context);
                return null;
            } else {
                throw e;
            }
        }
        arg1 = arg1.getRoot();
        if (arg1.getNodeKind() != Type.DOCUMENT) {
            dynamicError("In the " + getFunctionName().getLocalName() + "() function," +
                            " the tree being searched must be one whose root is a document node", "FODC0001", context);
            return null;
        }
        DocumentInfo doc = (DocumentInfo)arg1;
        SequenceIterator idrefs = argument[0].iterate(context);
        IdMappingFunction map = new IdMappingFunction();
        map.document = doc;
        SequenceIterator result = new MappingIterator(idrefs, map);
        return new DocumentOrderIterator(result, LocalOrderComparer.getInstance());
    }

    private static class IdMappingFunction implements MappingFunction {

        public DocumentInfo document;

        /**
        * Evaluate the function for a single string value
        * (implements the MappingFunction interface)
        */

        public SequenceIterator map(Item item) throws XPathException {

            String idrefs = Whitespace.trim(item.getStringValueCS());

            // If this value contains a space, we need to break it up into its
            // separate tokens; if not, we can process it directly

            if (Whitespace.containsWhitespace(idrefs)) {
                List<StringValue> refs = new ArrayList<StringValue>(10);
                StringTokenizer st = new StringTokenizer(idrefs);
                while (st.hasMoreTokens()) {
                    refs.add(StringValue.makeStringValue(st.nextToken()));
                }
                IdMappingFunction submap = new IdMappingFunction();
                submap.document = document;
                return new MappingIterator(new ListIterator(refs), submap);

            } else {
                return SingletonIterator.makeIterator(document.selectID(idrefs));
            }
        }
    }

}




// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
