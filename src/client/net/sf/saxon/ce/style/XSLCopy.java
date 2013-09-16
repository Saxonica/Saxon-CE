package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.SequenceType;

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

        Boolean b = (Boolean)checkAttribute("copy-namespaces", "b");
        if (b != null) {
            copyNamespaces = b;
        }
        b = (Boolean)checkAttribute("inherit-namespaces", "b");
        if (b != null) {
            inheritNamespaces = b;
        }
        checkAttribute("type", "t");
        checkAttribute("validation", "v");
        use = (String)checkAttribute("use-attribute-sets", "w");
        checkForUnknownAttributes();

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
                                        false, role);
        } catch (XPathException err) {
            compileError(err);
        }

        Copy inst = new Copy(select,
                             copyNamespaces,
                             inheritNamespaces);
        Expression content = compileSequenceConstructor(exec, decl);

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
