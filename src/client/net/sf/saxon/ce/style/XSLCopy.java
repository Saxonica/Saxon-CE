package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.lib.Validation;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.Whitespace;

/**
* Handler for xsl:copy elements in stylesheet. <br>
*/

public class XSLCopy extends StyleElement {

    private String use;                     // value of use-attribute-sets attribute
    private AttributeSet[] attributeSets = null;
    private boolean copyNamespaces = true;
    private boolean inheritNamespaces = true;
    private Expression select = null;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
		String copyNamespacesAtt = null;
		String validationAtt = null;
		String typeAtt = null;
        String inheritAtt = null;

        for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.USE_ATTRIBUTE_SETS)) {
        		use = atts.getValue(a);
            } else if (f.equals(StandardNames.COPY_NAMESPACES)) {
                copyNamespacesAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.TYPE)) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.VALIDATION)) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.INHERIT_NAMESPACES)) {
                inheritAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (copyNamespacesAtt == null) {
            copyNamespaces = true;
        } else {
            if (copyNamespacesAtt.equals("yes")) {
                copyNamespaces = true;
            } else if (copyNamespacesAtt.equals("no")) {
                copyNamespaces = false;
            } else {
                compileError("Value of copy-namespaces must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (validationAtt != null && Validation.getCode(validationAtt) != Validation.STRIP) {
            compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
        }
        if (typeAtt!=null) {
            compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
        }

        if (inheritAtt != null) {
            if (inheritAtt.equals("yes")) {
                inheritNamespaces = true;
            } else if (inheritAtt.equals("no")) {
                inheritNamespaces = false;
            } else {
                compileError("The @inherit-namespaces attribute has permitted values (yes, no)", "XTSE0020");
            }
        }

    }

    public void validate(Declaration decl) throws XPathException {
        if (use!=null) {
            // find any referenced attribute sets
            attributeSets = getAttributeSets(use, null);
        }

        if (select == null) {
            select = new ContextItemExpression();
            select.setSourceLocator(this);
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        select = typeCheck(select);
        try {
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "xsl:copy/select", 0);
            role.setErrorCode("XTTE2170");
            select = TypeChecker.staticTypeCheck(select,
                                        SequenceType.OPTIONAL_ITEM,
                                        false, role, makeExpressionVisitor());
        } catch (XPathException err) {
            compileError(err);
        }

        Copy inst = new Copy(select,
                             copyNamespaces,
                             inheritNamespaces);
        Expression content = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));

        if (attributeSets != null) {
            UseAttributeSets use = new UseAttributeSets(attributeSets);
            // The use-attribute-sets is ignored unless the context item is an element node. So we
            // wrap the UseAttributeSets instruction in a conditional to perform a run-time test
            Expression condition = new InstanceOfExpression(
                    new ContextItemExpression(),
                    SequenceType.makeSequenceType(NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE));
            Expression choice = Choose.makeConditional(condition, use);
            if (content == null) {
                content = choice;
            } else {
                content = Block.makeBlock(choice, content);
                content.setSourceLocator(this);
            }
        }

        if (content == null) {
            content = Literal.makeEmptySequence();
        }
        inst.setContentExpression(content);
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
