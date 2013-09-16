package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.SortKeyDefinition;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.StringValue;


/**
* An xsl:sort element in the stylesheet. <br>
*/

public class XSLSort extends StyleElement {

    private SortKeyDefinition sortKeyDefinition;
    private Expression select;
    private Expression order;
    private Expression dataType = null;
    private Expression caseOrder;
    private Expression lang;
    private Expression collationName;
    private Expression stable;
    private boolean useDefaultCollation = true;

    /**
      * Determine whether this type of element is allowed to contain a sequence constructor
      * @return true: yes, it may contain a sequence constructor
      */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
        select = (Expression)checkAttribute("select", "e");
        order = (Expression)checkAttribute("order", "a");
        dataType = (Expression)checkAttribute("data-type", "a");
        caseOrder = (Expression)checkAttribute("case-order", "a");
        lang = (Expression)checkAttribute("lang", "a");
        collationName = (Expression)checkAttribute("collation", "a");
        stable = (Expression)checkAttribute("stable", "a");

        if (order == null) {
            order = new StringLiteral("ascending");
        }

        if (caseOrder == null) {
            caseOrder = new StringLiteral("#default");
        } else {
            useDefaultCollation = false;
        }

        if (lang == null) {
            lang = new StringLiteral(StringValue.EMPTY_STRING);
        } else {
            useDefaultCollation = false;
        }

        if (collationName != null) {
            useDefaultCollation = false;
        }

    }

    public void validate(Declaration decl) throws XPathException {
        if (select != null && hasChildNodes()) {
            compileError("An xsl:sort element with a select attribute must be empty", "XTSE1015");
        }
        if (select == null && !hasChildNodes()) {
            select = new ContextItemExpression();
        }

        // Get the named or default collation

        if (useDefaultCollation) {
            collationName = new StringLiteral(getDefaultCollationName());
        }

        StringCollator stringCollator = null;
        if (collationName instanceof StringLiteral) {
            String collationString = ((StringLiteral)collationName).getStringValue();
            try {
                URI collationURI = new URI(collationString, true);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(getBaseURI());
                    collationURI = base.resolve(collationURI.toString());
                    collationString = collationURI.toString();
                }
            } catch (URI.URISyntaxException err) {
                compileError("Collation name '" + collationString + "' is not a valid URI");
                collationString = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
            stringCollator = getConfiguration().getNamedCollation(collationString);
            if (stringCollator==null) {
                compileError("Collation " + collationString + " has not been defined", "XTDE1035");
                stringCollator = CodepointCollator.getInstance();     // for recovery paths
            }
        }

        select      = typeCheck(select);
        order       = typeCheck(order);
        caseOrder   = typeCheck(caseOrder);
        lang        = typeCheck(lang);
        dataType    = typeCheck(dataType);
        collationName = typeCheck(collationName);
        stable      = typeCheck(stable);

        if (select != null) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                select = TypeChecker.staticTypeCheck(select,
                                SequenceType.ATOMIC_SEQUENCE,
                                false, role);
            } catch (XPathException err) {
                compileError(err);
            }
        }

        sortKeyDefinition = new SortKeyDefinition();
        sortKeyDefinition.setSortProperty(SortKeyDefinition.ORDER, order);
        sortKeyDefinition.setSortProperty(SortKeyDefinition.CASE_ORDER, caseOrder);
        sortKeyDefinition.setSortProperty(SortKeyDefinition.LANG, lang);
        sortKeyDefinition.setSortKey(select);
        sortKeyDefinition.setSortProperty(SortKeyDefinition.DATA_TYPE, dataType);
        sortKeyDefinition.setSortProperty(SortKeyDefinition.COLLATION, collationName);
        sortKeyDefinition.setCollation(stringCollator);
        sortKeyDefinition.setBaseURI(getBaseURI());
        sortKeyDefinition.setSortProperty(SortKeyDefinition.STABLE, stable);
        sortKeyDefinition.setBackwardsCompatible(xPath10ModeIsEnabled());
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return null;
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        if (select == null) {
            Expression b = compileSequenceConstructor(exec, decl);
            if (b == null) {
                b = new Literal(EmptySequence.getInstance());
            }
            b.setContainer(this);
            try {
                ExpressionVisitor visitor = makeExpressionVisitor();
                Expression atomizedSortKey = new Atomizer(b);
                atomizedSortKey = visitor.simplify(atomizedSortKey);
                ExpressionTool.copyLocationInfo(b, atomizedSortKey);
                sortKeyDefinition.setSortKey(atomizedSortKey);
            } catch (XPathException e) {
                compileError(e);
            }
        }
        // Simplify the sort key definition - this is especially important in the case where
        // all aspects of the sort key are known statically.
        sortKeyDefinition = sortKeyDefinition.simplify(makeExpressionVisitor());
        // not an executable instruction
        return null;
    }

    public SortKeyDefinition getSortKeyDefinition() {
        return sortKeyDefinition;
    }

    public Expression getStable() {
        return stable;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
