package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.event.NoOpenStartTagException;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.type.TypeHierarchy;

import java.util.Iterator;


/**
 * An xsl:copy-of element in the stylesheet.
 */

public class CopyOf extends Instruction {

    private Expression select;
    private boolean copyNamespaces;
    private String staticBaseUri;

    /**
     * Create an xsl:copy-of instruction (also used in XQuery for implicit copying)
     * @param select expression that selects the nodes to be copied
     * @param copyNamespaces true if namespaces are to be copied
     * if duplicates are handled by discarding all but the first (XSLT).
     */

    public CopyOf(Expression select,
                  boolean copyNamespaces) {
        this.select = select;
        this.copyNamespaces = copyNamespaces;
        adoptChildExpression(select);
    }

     /**
     * Set the static base URI of the xsl:copy-of instruction
     * @param base the static base URI
     */

    public void setStaticBaseUri(String base) {
        staticBaseUri = base;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * The result depends on the type of the select expression.
     */

    public final boolean createsNewNodes() {
        Executable exec = getExecutable();
        if (exec == null) {
            return true;    // This shouldn't happen, but we err on the safe side
        }
        final TypeHierarchy th = exec.getConfiguration().getTypeHierarchy();
        return !select.getItemType(th).isAtomicType();
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return PROCESS_METHOD;
    }


    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        return this;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return select.getItemType(th);
    }

    public int getCardinality() {
        return select.getCardinality();
    }

    public int getDependencies() {
        return select.getDependencies();
    }

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.typeCheck(select, contextItemType);
        adoptChildExpression(select);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (select.getItemType(th).isAtomicType()) {
            return select;
        }
        return this;
    }


    public Iterator<Expression> iterateSubExpressions() {
        return monoIterator(select);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        return found;
    }

    /**
     * Process this xsl:copy-of instruction
     *
     * @param context the dynamic context for the transformation
     * @return null - this implementation of the method never returns a TailCall
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        SequenceReceiver out = context.getReceiver();
        boolean copyBaseURI = (out.getSystemId() == null);
            // if the copy is being attached to an existing parent, it inherits the base URI of the parent

        int copyOptions = CopyOptions.TYPE_ANNOTATIONS;
        if (copyNamespaces) {
            copyOptions |= CopyOptions.ALL_NAMESPACES;
        }

        //int whichNamespaces = (copyNamespaces ? NodeInfo.ALL_NAMESPACES : NodeInfo.NO_NAMESPACES);

        SequenceIterator iter = select.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                NodeInfo source = (NodeInfo) item;
                int kind = source.getNodeKind();

                switch (kind) {

                    case Type.ELEMENT: {
                        if (copyBaseURI) {
                            out.setSystemId(computeNewBaseUri(source));
                        }

                        source.copy(out, copyOptions);
                        break;
                    }
                    case Type.ATTRIBUTE:
                        try {
                            context.getReceiver().attribute(source.getNodeName(), source.getStringValue());
                        } catch (NoOpenStartTagException err) {
                            dynamicError(err.getMessage(), err.getErrorCodeLocalPart(), context);
                        }
                        break;
                    case Type.TEXT:
                        out.characters(source.getStringValue());
                        break;

                    case Type.PROCESSING_INSTRUCTION:
                        if (copyBaseURI) {
                            out.setSystemId(source.getBaseURI());
                        }
                        out.processingInstruction(source.getDisplayName(), source.getStringValue());
                        break;

                    case Type.COMMENT:
                        out.comment(source.getStringValue());
                        break;

                    case Type.NAMESPACE:
                        try {
                            source.copy(out, 0);
                        } catch (NoOpenStartTagException err) {
                            dynamicError(err.getMessage(), err.getErrorCodeLocalPart(), context);
                        }
                        break;

                    case Type.DOCUMENT: {
                        out.setPipelineConfiguration(out.getPipelineConfiguration());
                        if (copyBaseURI) {
                            out.setSystemId(source.getBaseURI());
                        }
                        source.copy(out, copyOptions);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());
                }

            } else {
                out.append(item, NodeInfo.ALL_NAMESPACES);
            }
        }
        return null;
    }

    private String computeNewBaseUri(NodeInfo source) {
        // These rules are the rules for xsl:copy-of instruction in XSLT. The same code is used to support the
        // validate{} expression in XQuery. XQuery says nothing about the base URI of a node that results
        // from a validate{} expression, so until it does, we might as well use the same logic.
        String newBaseUri;
        String xmlBase = Navigator.getAttributeValue(source, NamespaceConstant.XML, "base");
        if (xmlBase != null) {
            try {
                URI xmlBaseUri = new URI(xmlBase, true);
                if (xmlBaseUri.isAbsolute()) {
                    newBaseUri = xmlBase;
                } else if (staticBaseUri != null) {
                    URI sbu = new URI(staticBaseUri);
                    URI abs = sbu.resolve(xmlBaseUri.toString());
                    newBaseUri = abs.toString();
                } else {
                    newBaseUri = source.getBaseURI();
                }
            } catch (URI.URISyntaxException err) {
                newBaseUri = source.getBaseURI();
            }
        } else {
            newBaseUri = source.getBaseURI();
        }
        return newBaseUri;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
