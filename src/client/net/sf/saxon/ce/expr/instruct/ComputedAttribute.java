package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StandardURIChecker;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

import java.util.ArrayList;
import java.util.Iterator;

/**
* An instruction derived from an xsl:attribute element in stylesheet, or from
 * an attribute constructor in XQuery, in cases where the attribute name is not known
 * statically
*/

public final class ComputedAttribute extends AttributeCreator {

    private Expression attributeName;
    private Expression namespace = null;
    private NamespaceResolver nsContext;

    /**
     * Construct an Attribute instruction
     * @param attributeName An expression to calculate the attribute name
     * @param namespace An expression to calculate the attribute namespace
     * @param nsContext a NamespaceContext object containing the static namespace context of the
* stylesheet instruction
     */

    public ComputedAttribute(Expression attributeName,
                             Expression namespace,
                             NamespaceResolver nsContext) {
        this.attributeName = attributeName;
        this.namespace = namespace;
        this.nsContext = nsContext;
        adoptChildExpression(attributeName);
        adoptChildExpression(namespace);
    }

    /**
     * Get the namespace resolver used to resolve any prefix in the name of the attribute
     * @return the namespace resolver if one has been saved; or null otherwise
     */

    public NamespaceResolver getNamespaceResolver() {
        return nsContext;
    }

    /**
     * Get the static type of this expression
     * @param th the type hierarchy cache
     * @return the static type of the item returned by this expression
     */

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.ATTRIBUTE;
    }

    /**
     * Get the static cardinality of this expression
     * @return the static cardinality (exactly one)
     */

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
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


     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        attributeName = visitor.simplify(attributeName);
        namespace = visitor.simplify(namespace);
        return super.simplify(visitor);
    }

    public void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        StaticContext env = visitor.getStaticContext();
        attributeName = visitor.typeCheck(attributeName, contextItemType);
        adoptChildExpression(attributeName);

        RoleLocator role;
        //role.setSourceLocator(this);

        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (!th.isSubType(attributeName.getItemType(th), BuiltInAtomicType.STRING)) {
            attributeName = SystemFunction.makeSystemFunction("string", new Expression[]{attributeName});
        }

        if (namespace != null) {
            visitor.typeCheck(namespace, contextItemType);
            adoptChildExpression(namespace);

            role = new RoleLocator(RoleLocator.INSTRUCTION, "attribute/namespace", 0);
            //role.setSourceLocator(this);
            namespace = TypeChecker.staticTypeCheck(
                    namespace, SequenceType.SINGLE_STRING, false, role, visitor);
        }
    }


    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        attributeName = visitor.optimize(attributeName, contextItemType);
        if (namespace != null) {
            namespace = visitor.optimize(namespace, contextItemType);
        }
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp != this) {
            return exp;
        }
        // If the name is known statically, use a FixedAttribute instead
        if (attributeName instanceof Literal && (namespace == null || namespace instanceof Literal)) {
            XPathContext context = visitor.getStaticContext().makeEarlyEvaluationContext();
            StructuredQName nc = evaluateNameCode(context);
            FixedAttribute fa = new FixedAttribute(nc);
            fa.setSelect(getContentExpression(), visitor.getConfiguration());
            return fa;
        }
        return this;
    }

    /**
     * Get the subexpressions of this expression
     * @return an iterator over the subexpressions
     */

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList list = new ArrayList(3);
        if (select != null) {
            list.add(select);
        }
        list.add(attributeName);
        if (namespace != null) {
            list.add(namespace);
        }
        return list.iterator();
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
        if (attributeName == original) {
            attributeName = replacement;
            found = true;
        }
        if (namespace == original) {
            namespace = replacement;
            found = true;
        }
                return found;
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
        attributeName = doPromotion(attributeName, offer);
        if (namespace != null) {
            namespace = doPromotion(namespace, offer);
        }
        super.promoteInst(offer);
    }


    /**
     * Determine the name to be used for the attribute, as a StructuredQName
     *
     * @param context Dynamic evaluation context
     * @return the StructuredQName for the attribute name
     * @throws XPathException
     */

    public StructuredQName evaluateNameCode(XPathContext context) throws XPathException {

        Item nameValue = attributeName.evaluateItem(context);

        String prefix = null;
        String localName = null;
        String uri = null;

        if (nameValue instanceof StringValue) {
            // this will always be the case in XSLT
            CharSequence rawName = nameValue.getStringValue();
            rawName = Whitespace.trimWhitespace(rawName); // required in XSLT; possibly wrong in XQuery
            try {
                String[] parts = NameChecker.getQNameParts(rawName);
                prefix = parts[0];
                localName = parts[1];
            } catch (QNameException err) {
                dynamicError("Invalid attribute name: " + rawName, "XTDE0850", context);
            }
            if (rawName.toString().equals("xmlns")) {
                if (namespace==null) {
                    dynamicError("Invalid attribute name: " + rawName, "XTDE0855", context);
                }
            }
            if (prefix.equals("xmlns")) {
                if (namespace==null) {
                    dynamicError("Invalid attribute name: " + rawName, "XTDE0860", context);
                } else {
                    // ignore the prefix "xmlns"
                    prefix = "";
                }
            }

        } else {
            typeError("Attribute name must be either a string or a QName", "XPTY0004", context);
        }

        if (namespace == null && uri == null) {
        	if (prefix.length() == 0) {
        		uri = "";
        	} else {
                uri = nsContext.getURIForPrefix(prefix, false);
                if (uri==null) {
                    dynamicError("Undeclared prefix in attribute name: " + prefix, "XTDE0860", context);
                }
        	}

        } else {
            if (uri == null) {
                // generate a name using the supplied namespace URI
                if (namespace instanceof StringLiteral) {
                    uri = ((StringLiteral)namespace).getStringValue();
                } else {
                    uri = namespace.evaluateAsString(context).toString();
                    if (!StandardURIChecker.getInstance().isValidURI(uri)) {
                        dynamicError("The value of the namespace attribute must be a valid URI", "XTDE0865", context);
                    }
                }
            }
            if (uri.length() == 0) {
                // there is a special rule for this case in the XSLT specification;
                // we force the attribute to go in the null namespace
                prefix = "";

            } else {
                // if a suggested prefix is given, use it; otherwise try to find a prefix
                // associated with this URI; if all else fails, invent one.
                if (prefix.length() == 0) {
                    prefix = "ns0"; // this will be replaced later if it is already in use
                    Iterator<String> prefixes = nsContext.iteratePrefixes();
                    while (prefixes.hasNext()) {
                        String p = prefixes.next();
                        if (nsContext.getURIForPrefix(p, false).equals(uri)) {
                            prefix = p;
                            break;
                        }
                    }
                }
            }
        }

        if (uri.equals(NamespaceConstant.XMLNS)) {
            dynamicError("Cannot create attribute in namespace " + uri, "XTDE0835", context);
        }

        return new StructuredQName(prefix, uri, localName);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
