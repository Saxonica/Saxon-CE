package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.NameChecker;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An xsl:processing-instruction element in the stylesheet, or a processing-instruction
 * constructor in a query
 */

public class ProcessingInstruction extends SimpleNodeConstructor {

    private Expression name;

    /**
     * Create an xsl:processing-instruction instruction
     * @param name the expression used to compute the name of the generated
     * processing-instruction
     */

    public ProcessingInstruction(Expression name) {
        this.name = name;
        adoptChildExpression(name);
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     * @return the string "xsl:processing-instruction"
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_PROCESSING_INSTRUCTION;
    }

    /**
     * Get the expression that defines the processing instruction name
     * @return the expression that defines the processing instruction name
     */

    public Expression getNameExpression() {
        return name;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.PROCESSING_INSTRUCTION;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        name = visitor.simplify(name);
        return super.simplify(visitor);
    }

    public void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        name = visitor.typeCheck(name, contextItemType);
        adoptChildExpression(name);

        RoleLocator role = new RoleLocator(RoleLocator.INSTRUCTION, "processing-instruction/name", 0);
        //role.setSourceLocator(this);
        name = TypeChecker.staticTypeCheck(name, SequenceType.SINGLE_STRING, false, role, visitor);
        adoptChildExpression(name);

    }

    public int getDependencies() {
        return name.getDependencies() | super.getDependencies();
    }

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        if (select != null) {
            list.add(select);
        }
        list.add(name);
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
        if (name == original) {
            name = replacement;
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
        name = doPromotion(name, offer);
        super.promoteInst(offer);
    }


    /**
     * Process the value of the node, to create the new node.
     * @param value the string value of the new node
     * @param context the dynamic evaluation context
     * @throws XPathException
     */

    public void processValue(CharSequence value, XPathContext context) throws XPathException {
        String expandedName = evaluateName(context);
        if (expandedName != null) {
            String data = checkContent(value.toString(), context);
            SequenceReceiver out = context.getReceiver();
            out.processingInstruction(expandedName, data);
        }
    }

    /**
     * Check the content of the node, and adjust it if necessary
     *
     * @param data the supplied content
     * @return the original content, unless adjustments are needed
     * @throws XPathException if the content is invalid
     */

    protected String checkContent(String data, XPathContext context) throws XPathException {
        int hh;
        while ((hh = data.indexOf("?>")) >= 0) {
            data = data.substring(0, hh + 1) + ' ' + data.substring(hh + 1);
        }
        data = Whitespace.removeLeadingWhitespace(data).toString();
        return data;
    }

    public int evaluateNameCode(XPathContext context) throws XPathException {
        String expandedName = evaluateName(context);
        return context.getNamePool().allocate("", "", expandedName);
    }

    /**
     * Evaluate the name of the processing instruction.
     * @param context
     * @return the name of the processing instruction (an NCName), or null, incicating an invalid name
     * @throws XPathException if evaluation fails, or if the recoverable error is treated as fatal
     */
    private String evaluateName(XPathContext context) throws XPathException {
        String expandedName = null;
        try {
            expandedName = Whitespace.trim(name.evaluateAsString(context));
        } catch (ClassCastException err) {
            dynamicError("Processing instruction name is not a string", "XQDY0041", context);
        }
        checkName(expandedName, context);
        return expandedName;
    }

    private void checkName(String expandedName, XPathContext context) throws XPathException {
        if (!(NameChecker.isValidNCName(expandedName))) {
            dynamicError("Processing instruction name " + Err.wrap(expandedName) + " is not a valid NCName", "XTDE0890", context);
        }
        if (expandedName.equalsIgnoreCase("xml")) {
            dynamicError("Processing instructions cannot be named 'xml' in any combination of upper/lower case", "XTDE0890", context);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
