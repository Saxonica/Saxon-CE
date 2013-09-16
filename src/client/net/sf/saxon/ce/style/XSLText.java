package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.ValueOf;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.NodeImpl;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.StringValue;

/**
* Handler for xsl:text elements in stylesheet. <BR>
*/

public class XSLText extends XSLLeafNodeConstructor {

    private StringValue value;

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return NodeKindTest.TEXT;
    }

    public void prepareAttributes() throws XPathException {
        checkAttribute("disable-output-escaping", "b");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {

        value = StringValue.EMPTY_STRING;
        for (NodeImpl child: allChildren()) {
            if (child instanceof StyleElement) {
                ((StyleElement)child).compileError("xsl:text must not contain child elements", "XTSE0010");
                return;
            } else {
                value = StringValue.makeStringValue(child.getStringValue());
                //continue;
            }
        }
        super.validate(decl);
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return null;     // not applicable
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return new ValueOf(Literal.makeLiteral(value), false);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
