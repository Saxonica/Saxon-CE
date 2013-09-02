package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.functions.SystemFunction;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.SimpleNodeConstructor;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.tree.iter.AxisIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

/**
 * Common superclass for XSLT elements whose content template produces a text
 * value: xsl:text, xsl:value-of, xsl:attribute, xsl:comment, and xsl:processing-instruction
 */

public abstract class XSLLeafNodeConstructor extends StyleElement {

    //protected String stringValue = null;
    protected Expression select = null;

    /**
     * Method for use by subclasses (processing-instruction and namespace) that take
     * a name and a select attribute
     * @return the expression defining the name attribute
     * @throws XPathException
     */

    protected Expression prepareAttributesNameAndSelect() throws XPathException {

        Expression name = null;
        String nameAtt = null;
        String selectAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals("name")) {
        		nameAtt = Whitespace.trim(atts.getValue(a));
       	    } else if (f.equals("select")) {
        		selectAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
        } else {
            name = makeAttributeValueTemplate(nameAtt);
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

        return name;
    }
    

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void validate(Declaration decl) throws XPathException {
        if (select != null && hasChildNodes()) {
            String errorCode = getErrorCodeForSelectPlusContent();
            compileError("An " + getDisplayName() + " element with a select attribute must be empty", errorCode);
        }
        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo first = (NodeInfo)kids.next();
        if (select == null) {
            if (first == null) {
                // there are no child nodes and no select attribute
                //stringValue = "";
                select = new StringLiteral(StringValue.EMPTY_STRING);
            } else {
                if (kids.next() == null) {
                    // there is exactly one child node
                    if (first.getNodeKind() == Type.TEXT) {
                        // it is a text node: optimize for this case
                        select = new StringLiteral(first.getStringValue());
                    }
                }
            }
        }
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     * @return the error code defined for this condition, for this particular instruction
     */

    protected abstract String getErrorCodeForSelectPlusContent();

    protected void compileContent(Executable exec, Declaration decl, SimpleNodeConstructor inst, Expression separator) throws XPathException {
        if (separator == null) {
            separator = new StringLiteral(StringValue.SINGLE_SPACE);
        }
        try {
            if (select == null) {
                select = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
            }
            select = makeSimpleContentConstructor(select, separator);
            inst.setSelect(select, exec.getConfiguration());

        } catch (XPathException err) {
            compileError(err);
        }
    }

    /**
     * Construct an expression that implements the rules of "constructing simple content":
     * given an expression to select the base sequence, and an expression to compute the separator,
     * build an (unoptimized) expression to produce the value of the node as a string.
     * @param select the expression that selects the base sequence
     * @param separator the expression that computes the separator
     * @return an expression that returns a string containing the string value of the constructed node
     */

    public static Expression makeSimpleContentConstructor(Expression select, Expression separator) {
        // Merge adjacent text nodes
        select = new AdjacentTextNodeMerger(select);
        // Atomize the result
        select = new Atomizer(select);
        // Convert each atomic value to a string
        select = new AtomicSequenceConverter(select, BuiltInAtomicType.STRING);
        // Join the resulting strings with a separator
        select = SystemFunction.makeSystemFunction("string-join", new Expression[]{select, separator});
        // All that's left for the instruction to do is to construct the right kind of node
        return select;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
