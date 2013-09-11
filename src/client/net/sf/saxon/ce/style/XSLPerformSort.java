package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.expr.sort.SortExpression;
import client.net.sf.saxon.ce.expr.sort.SortKeyDefinition;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.Whitespace;


/**
* Handler for xsl:perform-sort elements in stylesheet (XSLT 2.0). <br>
*/

public class XSLPerformSort extends StyleElement {

    Expression select = null;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (select==null) {
            return getCommonChildItemType();
        } else {
            return select.getItemType();
        }
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Specify that xsl:sort is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLSort);
    }

    public void prepareAttributes() throws XPathException {
        select = (Expression)checkAttribute("select", "e1");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        checkSortComesFirst(true);

        if (select != null) {
            // if there is a select attribute, check that there are no children other than xsl:sort and xsl:fallback
            UnfailingIterator kids = iterateAxis(Axis.CHILD);
            while (true) {
                NodeInfo child = (NodeInfo)kids.next();
                if (child == null) {
                    break;
                }
                if (child instanceof XSLSort || child instanceof XSLFallback) {
                    // no action
                } else if (child.getNodeKind() == Type.TEXT && !Whitespace.isWhite(child.getStringValue())) {
                        // with xml:space=preserve, white space nodes may still be there
                    compileError("Within xsl:perform-sort, significant text must not appear if there is a select attribute",
                            "XTSE1040");
                } else {
                    ((StyleElement)child).compileError(
                            "Within xsl:perform-sort, child instructions are not allowed if there is a select attribute",
                            "XTSE1040");
                }
            }
        }
        select = typeCheck(select);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        SortKeyDefinition[] sortKeys = makeSortKeys(decl);
        if (select != null) {
            return new SortExpression(select, sortKeys);
        } else {
            Expression body = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
            if (body == null) {
                body = Literal.makeEmptySequence();
            }
            try {
                return new SortExpression(makeExpressionVisitor().simplify(body), sortKeys);
            } catch (XPathException e) {
                compileError(e);
                return null;
            }
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
