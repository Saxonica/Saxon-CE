package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.EmptySequence;
import com.google.gwt.logging.client.LogConfiguration;


/**
 * An xsl:element element in the stylesheet. <br>
 */

public class XSLElement extends StyleElement {

    private Expression elementName;
    private Expression namespace = null;
    private String use;
    private AttributeSet[] attributeSets = null;
    private boolean inheritNamespaces = true;

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

    public void prepareAttributes() throws XPathException {
        elementName = (Expression)checkAttribute("name", "a1");
        namespace = (Expression)checkAttribute("namespace", "a");
        checkAttribute("validation", "v");
        checkAttribute("type", "t");
        Boolean b = (Boolean)checkAttribute("inherit-namespaces", "b");
        if (b != null) {
            inheritNamespaces = b;
        }
        use = (String)checkAttribute("use-attribute-sets", "s");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        if (use != null) {
            // find any referenced attribute sets
            attributeSets = getAttributeSets(use, null);
        }
        elementName = typeCheck(elementName);
        namespace = typeCheck(namespace);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        NamespaceResolver resolver = new InscopeNamespaceResolver(this);

        // deal specially with the case where the element name is known statically

        if (elementName instanceof StringLiteral) {
            CharSequence qName = ((StringLiteral)elementName).getStringValue();

            String[] parts;
            try {
                parts = NameChecker.getQNameParts(qName);
            } catch (QNameException e) {
                compileError("Invalid element name: " + qName, "XTDE0820");
                return null;
            }

            String nsuri = null;
            if (namespace instanceof StringLiteral) {
                nsuri = ((StringLiteral)namespace).getStringValue();
                if (nsuri.length() == 0) {
                    parts[0] = "";
                }
            } else if (namespace == null) {
                nsuri = resolver.getURIForPrefix(parts[0], true);
                if (nsuri == null) {
                    undeclaredNamespaceError(parts[0], "XTDE0830");
                }
            }
            if (nsuri != null) {
                // Local name and namespace are both known statically: generate a FixedElement instruction
                StructuredQName nameCode = new StructuredQName(parts[0], nsuri, parts[1]);
                FixedElement inst = new FixedElement(nameCode, null, inheritNamespaces);
                inst.setBaseURI(getBaseURI());
                if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
                	inst.AddTraceProperty("name", elementName);
                }
                return compileContentExpression(exec, decl, inst);
            }
        }

        ComputedElement inst = new ComputedElement(elementName, namespace, resolver, inheritNamespaces);
        return compileContentExpression(exec, decl, inst);
    }

    private Expression compileContentExpression(Executable exec, Declaration decl, ElementCreator inst) throws XPathException {
        Expression content = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));

        if (attributeSets != null) {
            UseAttributeSets use = new UseAttributeSets(attributeSets);
            if (content == null) {
                content = use;
            } else {
                content = Block.makeBlock(use, content);
                content.setSourceLocator(this);
            }
        }
        if (content == null) {
            content = new Literal(EmptySequence.getInstance());
        }
        inst.setContentExpression(content);
        return inst;
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
