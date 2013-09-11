package client.net.sf.saxon.ce.expr.instruct;
import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;

/**
 * An instruction derived from an xsl:attribute element in stylesheet, or from
 * an attribute constructor in XQuery. This version deals only with attributes
 * whose name is known at compile time. It is also used for attributes of
 * literal result elements. The value of the attribute is in general computed
 * at run-time.
*/

public final class FixedAttribute extends AttributeCreator {

    private StructuredQName nameCode;

    /**
     * Construct an Attribute instruction
     * @param nameCode Represents the attribute name
     * of the instruction - zero if the attribute was not present
    */

    public FixedAttribute (StructuredQName nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Set the expression defining the value of the attribute. If this is a constant, and if
     * validation against a schema type was requested, the validation is done immediately.
     * @param select The expression defining the content of the attribute
     * @param config The Saxon configuration
     * @throws XPathException if the expression is a constant, and validation is requested, and
     * the constant doesn't match the required type.
     */
    public void setSelect(Expression select, Configuration config) throws XPathException {
        super.setSelect(select, config);
        // If attribute name is xml:id, add whitespace normalization
        if (nameCode.equals(StructuredQName.XML_ID)) {
            select = SystemFunction.makeSystemFunction("normalize-space", new Expression[]{select});
            super.setSelect(select, config);
        }
    }

    public void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        //
    }

    public ItemType getItemType() {
        return NodeKindTest.ATTRIBUTE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public StructuredQName evaluateNameCode(XPathContext context)  {
        return nameCode;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
