package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.sort.DocumentOrderIterator;
import client.net.sf.saxon.ce.expr.sort.LocalOrderComparer;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.style.ExpressionContext;
import client.net.sf.saxon.ce.trans.KeyDefinitionSet;
import client.net.sf.saxon.ce.trans.KeyManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.Cardinality;


public class KeyFn extends SystemFunction {

    public KeyFn newInstance() {
        return new KeyFn();
    }

    private NamespaceResolver nsContext = null;
    private KeyDefinitionSet staticKeySet = null; // null if name resolution is done at run-time
    private transient boolean checked = false;
    private transient boolean internal = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate



    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        try {
            return super.typeCheck(visitor, contextItemType);
        } catch (XPathException err) {
            if ("XPDY0002".equals(err.getErrorCodeLocalPart())) {
                dynamicError("Cannot call the key() function when there is no context node", "XTDE1270");
            }
            throw err;
        }
    }

    /**
     * Simplify: add a third implicit argument, the context document
     * @param visitor the expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        if (!internal && !(visitor.getStaticContext() instanceof ExpressionContext)) {
            throw new XPathException("The key() function is available only in XPath expressions within an XSLT stylesheet");
        }
        KeyFn f = (KeyFn)super.simplify(visitor);
        if (argument.length == 2) {
            f.addContextDocumentArgument(2, "key");
        }
        return f;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
        argument[1] = ExpressionTool.unsorted(visitor.getConfiguration(), argument[1], false);
        if (argument[0] instanceof StringLiteral) {
            // common case, key name is supplied as a constant
            StructuredQName keyName;
            try {
                keyName = StructuredQName.fromLexicalQName(((StringLiteral)argument[0]).getStringValue(), "", nsContext);
            } catch (XPathException e) {
                XPathException err = new XPathException("Error in key name " +
                        ((StringLiteral)argument[0]).getStringValue() + ": " + e.getMessage());
                err.setLocator(getSourceLocator());
                err.setErrorCode("XTDE1260");
                throw err;
            }
            staticKeySet = visitor.getExecutable().getKeyManager().getKeyDefinitionSet(keyName);
            if (staticKeySet == null) {
                XPathException err = new XPathException("Key " +
                        ((StringLiteral)argument[0]).getStringValue() + " has not been defined");
                err.setLocator(getSourceLocator());
                err.setErrorCode("XTDE1260");
                throw err;
            }
        } else {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
        }
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * a property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int prop = StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
        if ((getNumberOfArguments() == 2) ||
                (argument[2].getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            prop |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        return prop;
    }

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor the expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }


    /**
    * Enumerate the results of the expression
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        Controller controller = context.getController();

        Item arg2;
        try {
            arg2 = argument[2].evaluateItem(context);
        } catch (XPathException e) {
            String code = e.getErrorCodeLocalPart();
            if ("XPDY0002".equals(code)) {
                dynamicError("Cannot call the key() function when there is no context item", "XTDE1270");
                return null;
            } else if ("XPDY0050".equals(code)) {
                dynamicError("In the key() function," +
                            " the node supplied in the third argument (or the context node if absent)" +
                            " must be in a tree whose root is a document node", "XTDE1270");
                return null;
            } else if ("XPTY0020".equals(code)) {
                dynamicError("Cannot call the key() function when the context item is an atomic value",
                        "XTDE1270");
                return null;
            }
            throw e;
        }

        NodeInfo origin = (NodeInfo)arg2;
        NodeInfo root = origin.getRoot();
        if (root.getNodeKind() != Type.DOCUMENT) {
            dynamicError("In the key() function," +
                            " the node supplied in the third argument (or the context node if absent)" +
                            " must be in a tree whose root is a document node", "XTDE1270");
            return null;
        }
        DocumentInfo doc = (DocumentInfo)root;

        final KeyManager keyManager = controller.getExecutable().getKeyManager();
        KeyDefinitionSet selectedKeySet = staticKeySet;
        if (selectedKeySet == null) {
            String givenkeyname = argument[0].evaluateItem(context).getStringValue();
            StructuredQName qName = null;
            try {
                qName = StructuredQName.fromLexicalQName(givenkeyname, "", nsContext);
            } catch (XPathException err) {
                dynamicError("Invalid key name: " + err.getMessage(), "XTDE1260");
            }
            selectedKeySet = keyManager.getKeyDefinitionSet(qName);
            if (selectedKeySet == null) {
                dynamicError("Key '" + givenkeyname + "' has not been defined", "XTDE1260");
                return null;
            }
        }

//        if (internal) {
//            System.err.println("Using key " + fprint + " on doc " + doc);
//        }

        // If the second argument is a singleton, we evaluate the function
        // directly; otherwise we recurse to evaluate it once for each Item
        // in the sequence.

        Expression expression = argument[1];
        SequenceIterator allResults;
        if (Cardinality.allowsMany(expression.getCardinality())) {
            final XPathContext keyContext = context;
            final DocumentInfo document = doc;
            final KeyDefinitionSet keySet = selectedKeySet;
            MappingFunction map = new MappingFunction() {
                // Map a value to the sequence of nodes having that value as a key value
                public SequenceIterator map(Item item) throws XPathException {
                    return keyManager.selectByKey(
                            keySet, document, (AtomicValue)item, keyContext);
                }
            };

            SequenceIterator keys = argument[1].iterate(context);
            SequenceIterator allValues = new MappingIterator(keys, map);
            allResults = new DocumentOrderIterator(allValues, LocalOrderComparer.getInstance());
        } else {
            try {
                AtomicValue keyValue = (AtomicValue)argument[1].evaluateItem(context);
                if (keyValue == null) {
                    return EmptyIterator.getInstance();
                }
                allResults = keyManager.selectByKey(selectedKeySet, doc, keyValue, context);
            } catch (XPathException e) {
                e.maybeSetLocation(getSourceLocator());
                throw e;
            }
        }
        if (origin == doc) {
            return allResults;
        }
        SubtreeFilter filter = new SubtreeFilter();
        filter.origin = origin;
        return new ItemMappingIterator(allResults, filter);
    }


    /**
     * Mapping class to filter nodes that have the origin node as an ancestor-or-self
     */

    private static class SubtreeFilter implements ItemMappingFunction {

        public NodeInfo origin;

        public Item mapItem(Item item) throws XPathException {
            if (Navigator.isAncestorOrSelf(origin, (NodeInfo)item)) {
                return item;
            } else {
                return null;
            }
        }

    }

}





// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
