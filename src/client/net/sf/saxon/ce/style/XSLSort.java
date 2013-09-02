package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.expr.sort.SortKeyDefinition;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;


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

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;
        String orderAtt = null;
        String dataTypeAtt = null;
        String caseOrderAtt = null;
        String langAtt = null;
        String collationAtt = null;
        String stableAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals("select")) {
        		selectAtt = atts.getValue(a);
        	} else if (f.equals("order")) {
        		orderAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("data-type")) {
        		dataTypeAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("case-order")) {
        		caseOrderAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("lang")) {
        		langAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("collation")) {
        		collationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("stable")) {
        		stableAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (selectAtt==null) {
            //select = new ContextItemExpression();
        } else {
            select = makeExpression(selectAtt);
        }

        if (orderAtt == null) {
            order = new StringLiteral("ascending");
        } else {
            order = makeAttributeValueTemplate(orderAtt);
        }

        if (dataTypeAtt == null) {
            dataType = null;
        } else {
            dataType = makeAttributeValueTemplate(dataTypeAtt);
        }

        if (caseOrderAtt == null) {
            caseOrder = new StringLiteral("#default");
        } else {
            caseOrder = makeAttributeValueTemplate(caseOrderAtt);
            useDefaultCollation = false;
        }

        if (langAtt == null || langAtt.equals("")) {
            lang = new StringLiteral(StringValue.EMPTY_STRING);
        } else {
            lang = makeAttributeValueTemplate(langAtt);
            useDefaultCollation = false;
            if (lang instanceof StringLiteral) {
                String s = ((StringLiteral)lang).getStringValue();
                if (s.length() != 0) {
                    if (!StringValue.isValidLanguageCode(s)) {
                        compileError("The lang attribute must be a valid language code", "XTDE0030");
                        lang = new StringLiteral(StringValue.EMPTY_STRING);
                    }
                }
            }
        }

        if (stableAtt == null) {
            stable = null;
        } else {
            stable = makeAttributeValueTemplate(stableAtt);
        }

        if (collationAtt != null) {
            collationName = makeAttributeValueTemplate(collationAtt);
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
                                false, role, makeExpressionVisitor());
            } catch (XPathException err) {
                compileError(err);
            }
        }

        sortKeyDefinition = new SortKeyDefinition();
        sortKeyDefinition.setOrder(order);
        sortKeyDefinition.setCaseOrder(caseOrder);
        sortKeyDefinition.setLanguage(lang);
        sortKeyDefinition.setSortKey(select);
        sortKeyDefinition.setDataTypeExpression(dataType);
        sortKeyDefinition.setCollationNameExpression(collationName);
        sortKeyDefinition.setCollation(stringCollator);
        sortKeyDefinition.setBaseURI(getBaseURI());
        sortKeyDefinition.setStable(stable);
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
            Expression b = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
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
