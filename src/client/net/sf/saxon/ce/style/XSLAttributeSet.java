package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.AttributeSet;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* An xsl:attribute-set element in the stylesheet. <br>
*/

public class XSLAttributeSet extends StyleElement implements StylesheetProcedure {

    private String useAtt;
                // the value of the use-attribute-sets attribute, as supplied

    private List<Declaration> attributeSetElements = null;
                // list of Declarations of XSLAttributeSet objects referenced by this one

    private AttributeSet[] useAttributeSets = null;
                // compiled instructions for the attribute sets used by this one

    private AttributeSet procedure = new AttributeSet();
                // the compiled form of this attribute set

    private boolean validated = false;

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
     * Get the name of this attribute set
     * @return the name of the attribute set, as a QName
     */

    public StructuredQName getAttributeSetName() {
        return getObjectName();
    }

    /**
     * Get the compiled code produced for this XSLT element
     * @return the compiled AttributeSet
     */

    public AttributeSet getInstruction() {
        return procedure;
    }

    public void prepareAttributes() throws XPathException {
        setObjectName((StructuredQName)checkAttribute("name", "q1"));
        useAtt = (String)checkAttribute("use-attribute-sets", "w");
        checkForUnknownAttributes();
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be null.
     * @return the name of the object declared in this element, if any
     */

    public StructuredQName getObjectName() {
        StructuredQName o = super.getObjectName();
        if (o == null) {
            try {
                prepareAttributes();
                o = getObjectName();
            } catch (XPathException err) {
                o = new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-attribute-set");
                setObjectName(o);
            }
        }
        return o;
    }

    public void validate(Declaration decl) throws XPathException {

        if (validated) return;

        checkTopLevel(null);

        onlyAllow("attribute");

        if (useAtt!=null) {
            // identify any attribute sets that this one refers to

            attributeSetElements = new ArrayList<Declaration>(5);
            useAttributeSets = getAttributeSets(useAtt, attributeSetElements);

            // check for circularity

            for (Iterator<Declaration> it=attributeSetElements.iterator(); it.hasNext();) {
                ((XSLAttributeSet)it.next().getSourceElement()).checkCircularity(this);
            }
        }

        validated = true;
    }

    /**
     * Check for circularity: specifically, check that this attribute set does not contain
     * a direct or indirect reference to the one supplied as a parameter
     * @param origin the place from which the search started
    */

    public void checkCircularity(XSLAttributeSet origin) throws XPathException {
        if (this==origin) {
            compileError("The definition of the attribute set is circular", "XTSE0720");
            useAttributeSets = null;
        } else {
            if (!validated) {
                // if this attribute set isn't validated yet, we don't check it.
                // The circularity will be detected when the last attribute set in the cycle
                // gets validated
                return;
            }
            if (attributeSetElements != null) {
                for (Iterator<Declaration> it=attributeSetElements.iterator(); it.hasNext();) {
                    ((XSLAttributeSet)it.next().getSourceElement()).checkCircularity(origin);
                }
            }
        }
    }

    /**
     * Compile the attribute set
     * @param exec the Executable
     * @param decl
     * @return a Procedure object representing the compiled attribute set
     * @throws XPathException if a failure is detected
     */
    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        Expression body = compileSequenceConstructor(exec, decl);
        if (body == null) {
            body = Literal.makeEmptySequence();
        }

        try {

            ExpressionVisitor visitor = makeExpressionVisitor();
            body = visitor.simplify(body);

            procedure.setUseAttributeSets(useAttributeSets);
            procedure.setName(getObjectName());
            procedure.setBody(body);
            procedure.setSourceLocator(this);
            procedure.setExecutable(exec);

            Expression exp2 = body.optimize(visitor, AnyItemType.getInstance());
            if (body != exp2) {
                procedure.setBody(exp2);
                body = exp2;
            }

            procedure.allocateSlots(0);
        } catch (XPathException e) {
            compileError(e);
        }
        return null;
    }

    /**
     * Optimize the stylesheet construct
     * @param declaration
     */

    public void optimize(Declaration declaration) throws XPathException {
        // Already done earlier
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
