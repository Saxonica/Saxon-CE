package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.logging.client.LogConfiguration;

import java.util.Iterator;


/**
 * An instruction representing an xsl:element element in an XSLT stylesheet,
 * or a computed element constructor in XQuery. (In both cases, if the element name
 * is expressed as a compile-time expression, then a FixedElement instruction
 * is used instead.)
 * @see FixedElement
 */

public class ComputedElement extends ElementCreator {

    private Expression elementName;
    private Expression namespace = null;
    private NamespaceResolver nsContext;

    /**
     * Create an instruction that creates a new element node
     *
     * @param elementName      Expression that evaluates to produce the name of the
     *                         element node as a lexical QName
     * @param namespace        Expression that evaluates to produce the namespace URI of
     *                         the element node. Set to null if the namespace is to be deduced from the prefix
     *                         of the elementName.
     * @param nsContext        Saved copy of the static namespace context for the instruction.
     *                         Can be set to null if namespace is supplied. This namespace context
     *                         must resolve the null prefix correctly, based on the different rules for
     *                         XSLT and XQuery.
     * @param inheritNamespaces true if child elements automatically inherit the namespaces of their parent

     */
    public ComputedElement(Expression elementName,
                           Expression namespace,
                           NamespaceResolver nsContext,
                           boolean inheritNamespaces) {
        this.elementName = elementName;
        this.namespace = namespace;
        this.nsContext = nsContext;
        this.inheritNamespaces = inheritNamespaces;
        adoptChildExpression(elementName);
        adoptChildExpression(namespace);
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	this.AddTraceProperty("name", elementName);
        }
    }

    /**
     * Get the namespace resolver that provides the namespace bindings defined in the static context
     * @return the namespace resolver
     */

    public NamespaceResolver getNamespaceResolver() {
        return nsContext;
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        elementName = visitor.simplify(elementName);
        namespace = visitor.simplify(namespace);
        return super.simplify(visitor);
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        elementName = visitor.typeCheck(elementName, contextItemType);
        RoleLocator role;
        TypeHierarchy th = TypeHierarchy.getInstance();
        if (!th.isSubType(elementName.getItemType(), AtomicType.STRING)) {
            elementName = SystemFunction.makeSystemFunction("string", new Expression[]{elementName});
        }
        if (namespace != null) {
            namespace = visitor.typeCheck(namespace, contextItemType);
            role = new RoleLocator(RoleLocator.INSTRUCTION, "element/namespace", 0);
            namespace = TypeChecker.staticTypeCheck(
                    namespace, SequenceType.SINGLE_STRING, false, role);
        }
        if (Literal.isAtomic(elementName)) {
            // Check we have a valid lexical QName, whose prefix is in scope where necessary
            try {
                AtomicValue val = (AtomicValue)((Literal)elementName).getValue();
                if (val instanceof StringValue) {
                    String[] parts = NameChecker.checkQNameParts(val.getStringValue());
                    if (namespace == null) {
                        String prefix = parts[0];
                        String uri = getNamespaceResolver().getURIForPrefix(prefix, true);
                        if (uri == null) {
                            XPathException se = new XPathException("Prefix " + prefix + " has not been declared", "XPST0081");
                            se.setIsStaticError(true);
                            throw se;
                        }
                        namespace = new StringLiteral(uri);
                    }
                }
            } catch (XPathException e) {
                String code = e.getErrorCodeLocalPart();
                if (code == null || code.equals("FORG0001")) {
                    e.setErrorCode("XTDE0820");
                } else if (code.equals("XPST0081")) {
                    e.setErrorCode("XTDE0830");
                }
                e.maybeSetLocation(getSourceLocator());
                e.setIsStaticError(true);
                throw e;
            }
        }
        return super.typeCheck(visitor, contextItemType);
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        elementName = visitor.optimize(elementName, contextItemType);
        return super.optimize(visitor, contextItemType);
    }

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(content, elementName, namespace);
    }


    /**
     * Offer promotion for subexpressions. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws client.net.sf.saxon.ce.trans.XPathException if any error is detected
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        elementName = doPromotion(elementName, offer);
        if (namespace != null) {
            namespace = doPromotion(namespace, offer);
        }
        super.promoteInst(offer);
    }


    /**
     * Callback from the superclass ElementCreator to get the nameCode
     * for the element name
     *
     *
     * @param context The evaluation context (not used)
     * @param copiedNode
     * @return the name code for the element name
     */

    public StructuredQName getNameCode(XPathContext context, NodeInfo copiedNode)
            throws XPathException {

        String prefix = null;
        String localName = null;
        String uri = null;

        // name needs to be evaluated at run-time
        AtomicValue nameValue = (AtomicValue)elementName.evaluateItem(context);
        if (nameValue == null) {
            dynamicError("Invalid element name (empty sequence)", "XTDE0820");
        }
        //nameValue = nameValue.getPrimitiveValue();
        if (nameValue instanceof StringValue) {  // which includes UntypedAtomic
            // this will always be the case in XSLT
            CharSequence rawName = nameValue.getStringValue();
            rawName = Whitespace.trimWhitespace(rawName); // required in XSLT; possibly wrong in XQuery
            try {
                String[] parts = NameChecker.getQNameParts(rawName);
                prefix = parts[0];
                localName = parts[1];
            } catch (QNameException err) {
                String message = "Invalid element name. " + err.getMessage();
                if (rawName.length() == 0) {
                    message = "Supplied element name is a zero-length string";
                }
                dynamicError(message, "XTDE0820");
            }
        } else {
            dynamicError("Computed element name has incorrect type", "XTDE0820");
        }

        if (namespace == null && uri == null) {
//            if (prefix.length() == 0) {
//                uri = defaultNamespace;
//            } else {
                uri = nsContext.getURIForPrefix(prefix, true);
                if (uri == null) {
                    dynamicError("Undeclared prefix in element name: " + prefix, "XTDE0830");
                }
//            }
        } else {
            if (uri == null) {
                uri = namespace.evaluateAsString(context).toString();
            }
            if (uri.length() == 0) {
                // there is a special rule for this case in the specification;
                // we force the element to go in the null namespace
                prefix = "";
            }
            if (prefix.equals("xmlns")) {
                // this isn't a legal prefix so we mustn't use it
                prefix = "x-xmlns";
            }
        }
        if (uri.equals(NamespaceConstant.XMLNS)) {
            dynamicError("Cannot create element in namespace " + uri, "XTDE0835");
        }
        if (uri.equals(NamespaceConstant.XML) != prefix.equals("xml")) {
            String message;
            if (prefix.equals("xml")) {
                message = "When the prefix is 'xml', the namespace URI must be " + NamespaceConstant.XML;
            } else {
                message = "When the namespace URI is " + NamespaceConstant.XML + ", the prefix must be 'xml'";
            }
            dynamicError(message, "XTDE0835");
        }

        return new StructuredQName(prefix, uri, localName);
    }

    public String getNewBaseURI(XPathContext context, NodeInfo copiedNode) {
        return getBaseURI();
    }

    /**
     * Callback to output namespace nodes for the new element.
     *
     *
     * @param context The execution context
     * @param out     the Receiver where the namespace nodes are to be written
     * @param nameCode
     * @param copiedNode
     * @throws XPathException
     */
    protected void outputNamespaceNodes(XPathContext context, Receiver out, StructuredQName nameCode, NodeInfo copiedNode)
            throws XPathException {
        // no action
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
