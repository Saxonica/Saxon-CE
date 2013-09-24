package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.ValueOf;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.StringValue;
import com.google.gwt.logging.client.LogConfiguration;


/**
* An xsl:value-of element in the stylesheet. <br>
* The xsl:value-of element takes attributes:<ul>
* <li>a mandatory attribute select="expression".
* This must be a valid String expression</li>
* <li>an optional disable-output-escaping attribute, value "yes" or "no"</li>
* <li>an optional separator attribute</li>
* </ul>
*/

public final class XSLValueOf extends XSLLeafNodeConstructor {

    private Expression separator;
    private String selectAttTrace = "";

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return NodeKindTest.TEXT;
    }

    public void prepareAttributes() throws XPathException {
        select = (Expression)checkAttribute("select", "e");
        separator = (Expression)checkAttribute("separator", "a");
        checkAttribute("disable-output-escaping", "b");
        checkForUnknownAttributes();
        
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	selectAttTrace = getAttributeValue("", "select");
        }
    }

    public void validate(Declaration decl) throws XPathException {
        super.validate(decl);
        select = typeCheck(select);
        separator = typeCheck(separator);
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0870";
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        final TypeHierarchy th = TypeHierarchy.getInstance();
        if (separator == null && select != null && xPath10ModeIsEnabled()) {
            if (!(select.getItemType() instanceof AtomicType)) {
                select = new Atomizer(select);
                select = makeExpressionVisitor().simplify(select);
            }
            if (Cardinality.allowsMany(select.getCardinality())) {
                select = new FirstItemExpression(select);
            }
            if (!th.isSubType(select.getItemType(), AtomicType.STRING)) {
                select = new AtomicSequenceConverter(select, AtomicType.STRING);
            }
        } else {
            if (separator == null) {
                if (select == null) {
                    separator = new StringLiteral(StringValue.EMPTY_STRING);
                } else {
                    separator = new StringLiteral(StringValue.SINGLE_SPACE);
                }
            }
        }
        ValueOf inst = new ValueOf(select, false);
        compileContent(exec, decl, inst, separator);
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	inst.AddTraceProperty("select", selectAttTrace);
        }
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
