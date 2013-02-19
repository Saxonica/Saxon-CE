package client.net.sf.saxon.ce.functions;
import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.sort.GenericAtomicComparer;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.BooleanValue;

/**
* XSLT 2.0 deep-equal() function.
* Supports deep comparison of two sequences (of nodes and/or atomic values)
* optionally using a collation
*/

public class DeepEqual extends CollatingFunction {

    public DeepEqual newInstance() {
        return new DeepEqual();
    }

    private transient Configuration config = null;

    /**
    * preEvaluate: if all arguments are known statically, evaluate early
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        config = visitor.getConfiguration();
        return super.preEvaluate(visitor);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        GenericAtomicComparer collator = getAtomicComparer(2, context);

        SequenceIterator op1 = argument[0].iterate(context);
        SequenceIterator op2 = argument[1].iterate(context);

        Configuration config =
                (this.config!=null ? this.config : context.getConfiguration());
        try {
            return BooleanValue.get(deepEquals(op1, op2, collator, config));
        } catch (XPathException e) {
            e.maybeSetLocation(getSourceLocator());
            e.maybeSetContext(context);
            throw e;
        }
    }

    /**
     * Determine when two sequences are deep-equal
     * @param op1 the first sequence
     * @param op2 the second sequence
     * @param collator the collator to be used
     * @param config the configuration (gives access to the NamePool)
     * @return true if the sequences are deep-equal
     * @throws XPathException if either sequence contains a function item
     */

    private static boolean deepEquals(SequenceIterator op1, SequenceIterator op2,
                                     GenericAtomicComparer collator, Configuration config)
    throws XPathException {
        boolean result = true;

        try {

            while (true) {
                Item item1 = op1.next();
                Item item2 = op2.next();

                if (item1 == null && item2 == null) {
                    break;
                }

                if (item1 == null || item2 == null) {
                    result = false;
                    break;
                }

                if (item1 instanceof NodeInfo) {
                    if (item2 instanceof NodeInfo) {
                        if (!deepEquals((NodeInfo)item1, (NodeInfo)item2, collator, config)) {
                            result = false;
                            break;
                        }
                    } else {
                        result = false;
                        break;
                    }
                } else {
                    if (item2 instanceof NodeInfo) {
                        result = false;
                        break;
                    } else {
                        AtomicValue av1 = ((AtomicValue)item1);
                        AtomicValue av2 = ((AtomicValue)item2);
                        if (av1.isNaN() && av2.isNaN()) {
                            // treat as equal, no action
                        } else if (!collator.comparesEqual(av1, av2)) {
                            result = false;
                            break;
                        }
                    }
                }
            } // end while

        } catch (ClassCastException err) {
            // this will happen if the sequences contain non-comparable values
            // comparison errors are masked
            result = false;
        } catch (XPathException err) {
            // comparison errors are masked
            if ("FOTY0015".equals(err.getErrorCodeLocalPart()) && NamespaceConstant.ERR.equals(err.getErrorCodeNamespace())) {
                throw err;
            }
            result = false;
        }

        return result;
    }

    /**
    * Determine whether two nodes are deep-equal
    */

    private static boolean deepEquals(NodeInfo n1, NodeInfo n2,
                                      GenericAtomicComparer collator, Configuration config)
    throws XPathException {
        // shortcut: a node is always deep-equal to itself
        if (n1.isSameNodeInfo(n2)) return true;

        if (n1.getNodeKind() != n2.getNodeKind()) {
            return false;
        }

        final NamePool pool = config.getNamePool();
        switch (n1.getNodeKind()) {
            case Type.ELEMENT:
                if (n1.getFingerprint() != n2.getFingerprint()) {
                    return false;
                }
                AxisIterator a1 = n1.iterateAxis(Axis.ATTRIBUTE);
                AxisIterator a2 = n2.iterateAxis(Axis.ATTRIBUTE);
                if (Count.count(a1.getAnother()) != Count.count(a2)) {
                    return false;
                }
                while (true) {
                    NodeInfo att1 = (NodeInfo)a1.next();
                    if (att1 == null) break;

                    AxisIterator a2iter = n2.iterateAxis(Axis.ATTRIBUTE,
                                            new NameTest(Type.ATTRIBUTE, att1.getFingerprint(), pool));
                    NodeInfo att2 = (NodeInfo)a2iter.next();

                    if (att2==null) {
                        return false;
                    }
                    if (!deepEquals(att1, att2, collator, config)) {
                        return false;
                    }
                }

                // fall through
            case Type.DOCUMENT:
                AxisIterator c1 = n1.iterateAxis(Axis.CHILD);
                AxisIterator c2 = n2.iterateAxis(Axis.CHILD);
                while (true) {
                    NodeInfo d1 = (NodeInfo)c1.next();
                    while (d1 != null && isIgnorable(d1))  {
                        d1 = (NodeInfo)c1.next();
                    }
                    NodeInfo d2 = (NodeInfo)c2.next();
                    while (d2 != null && isIgnorable(d2))  {
                        d2 = (NodeInfo)c2.next();
                    }
                    if (d1 == null || d2 == null) {
                        return (d1 == d2);
                    }
                    if (!deepEquals(d1, d2, collator, config)) {
                        return false;
                    }
                }

            case Type.ATTRIBUTE:
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
            case Type.TEXT:
            case Type.COMMENT:
                return (n1.getFingerprint() == n2.getFingerprint() &&
                        collator.comparesEqual(n1.getTypedValue(), n2.getTypedValue()));


            default:
                throw new IllegalArgumentException("Unknown node type");
        }
    }

    private static boolean isIgnorable(NodeInfo node) {
        final int kind = node.getNodeKind();
        if (kind == Type.COMMENT) {
            return true;
        } else if (kind == Type.PROCESSING_INSTRUCTION) {
            return true;
        }
        return false;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.