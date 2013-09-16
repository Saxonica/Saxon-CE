package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.Builder;
import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.StringJoin;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.om.DocumentInfo;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.TextFragmentValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;


/**
 * An instruction to create a document node. This corresponds to the xsl:document-node
 * instruction in XSLT. It is also used to support the document node constructor
 * expression in XQuery, and is generated implicitly within an xsl:variable
 * that constructs a temporary tree.
 *
 * <p>Conceptually it represents an XSLT instruction xsl:document-node,
 * with no attributes, whose content is a complex content constructor for the
 * children of the document node.</p>
 */

public class DocumentInstr extends ParentNodeConstructor {

    //private static final int[] treeSizeParameters = {50, 10, 5, 200};
    // estimated size of a temporary tree: {nodes, attributes, namespaces, characters}

    private boolean textOnly;
    private String constantText;

    /**
     * Create a document constructor instruction
     * @param textOnly true if the content contains text nodes only
     * @param constantText if the content contains text nodes only and the text is known at compile time,
     *        supplies the textual content
     * @param baseURI the base URI of the instruction
     */

    public DocumentInstr(boolean textOnly,
                         String constantText,
                         String baseURI) {
        this.textOnly = textOnly;
        this.constantText = constantText;
        setBaseURI(baseURI);
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.EVALUATE_METHOD;
    }

    /**
     * Determine whether this is a "text only" document: essentially, an XSLT xsl:variable that contains
     * a single text node or xsl:value-of instruction.
     * @return true if this is a text-only document
     */

    public boolean isTextOnly() {
        return textOnly;
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
        return super.simplify(visitor);
    }


    /**
     * In the case of a text-only instruction (xsl:variable containing a text node or one or more xsl:value-of
     * instructions), return an expression that evaluates to the textual content as an instance of xs:untypedAtomic
     * @param env the static evaluation context
     * @return an expression that evaluates to the textual content
     */

    public Expression getStringValueExpression(StaticContext env) {
        if (textOnly) {
            if (constantText != null) {
                return new StringLiteral(new UntypedAtomicValue(constantText));
            } else if (content instanceof ValueOf) {
                return ((ValueOf)content).convertToCastAsString();
            } else {
                StringJoin fn = (StringJoin)SystemFunction.makeSystemFunction(
                        "string-join", new Expression[]{content, new StringLiteral(StringValue.EMPTY_STRING)});
                CastExpression cast = new CastExpression(fn, AtomicType.UNTYPED_ATOMIC, false);
                ExpressionTool.copyLocationInfo(this, cast);
                return cast;
            }
        } else {
            throw new AssertionError("getStringValueExpression() called on non-text-only document instruction");
        }
    }


    /**
     * Get the item type
     * @return the in
     */
    public ItemType getItemType() {
        return NodeKindTest.DOCUMENT;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        out.startDocument();
        content.process(context);
        out.endDocument();
        return null;
    }

    /**
     * Evaluate as an expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        DocumentInfo root;
        if (textOnly) {
            CharSequence textValue;
            if (constantText != null) {
                textValue = constantText;
            } else {
                FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
                SequenceIterator iter = content.iterate(context);
                while (true) {
                    Item item = iter.next();
                    if (item==null) break;
                    sb.append(item.getStringValue());
                }
                textValue = sb.condense();
            }
            root = new TextFragmentValue(textValue, getBaseURI());
            ((TextFragmentValue)root).setConfiguration(controller.getConfiguration());
        } else {
            try {
                XPathContext c2 = context.newMinorContext();

                Builder builder = controller.makeBuilder();

                //receiver.setSystemId(getBaseURI());
                builder.setBaseURI(getBaseURI());

                PipelineConfiguration pipe = controller.makePipelineConfiguration();
                builder.setPipelineConfiguration(pipe);

                c2.changeOutputDestination(builder, false);
                Receiver out = c2.getReceiver();
                out.open();
                out.startDocument();

                content.process(c2);

                out.endDocument();
                out.close();

                root = (DocumentInfo)builder.getCurrentRoot();
            } catch (XPathException e) {
                e.maybeSetLocation(getSourceLocator());
                throw e;
            }
        }
        return root;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
