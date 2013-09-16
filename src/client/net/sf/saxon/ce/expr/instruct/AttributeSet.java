package client.net.sf.saxon.ce.expr.instruct;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.XPathContextMajor;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;

/**
* The compiled form of an xsl:attribute-set element in the stylesheet.
*/

// Note, there is no run-time check for circularity. This is checked at compile time.

public class AttributeSet extends Procedure {

    StructuredQName attributeSetName;

    private AttributeSet[] useAttributeSets;

    /**
     * Create an empty attribute set
     */

    public AttributeSet() {
    }

    /**
     * Set the name of the attribute-set
     * @param attributeSetName the name of the attribute-set
     */

    public void setName(StructuredQName attributeSetName) {
        this.attributeSetName = attributeSetName;
    }

    /**
     * Set the attribute sets used by this attribute set
     * @param useAttributeSets the set of attribute sets used by this attribute set
     */

    public void setUseAttributeSets(AttributeSet[] useAttributeSets) {
        this.useAttributeSets = useAttributeSets;
    }

    /**
     * Determine whether the attribute set has any dependencies on the focus
     * @return the dependencies
     */

    public int getFocusDependencies() {
        int d = 0;
        if (body != null) {
            d |= body.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS;
        }
        if (useAttributeSets != null) {
            for (AttributeSet useAttributeSet : useAttributeSets) {
                d |= useAttributeSet.getFocusDependencies();
            }
        }
        return d;
    }

    /**
     * Evaluate an attribute set
     * @param context the dynamic context
     * @throws XPathException if any failure occurs
     */

    public void expand(XPathContext context) throws XPathException {
        // apply the content of any attribute sets mentioned in use-attribute-sets

        if (useAttributeSets != null) {
            AttributeSet.expand(useAttributeSets, context);
        }

        if (getNumberOfSlots() != 0) {
            XPathContextMajor c2 = context.newContext();
            c2.openStackFrame(getNumberOfSlots());
            getBody().process(c2);
        } else {
            getBody().process(context);
        }
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     *
     */

    public StructuredQName getObjectName() {
        return attributeSetName;
    }

    /**
     * Expand an array of attribute sets
     * @param asets the attribute sets to be expanded
     * @param context the run-time context to use
     * @throws XPathException if a dynamic error occurs
     */

    protected static void expand(AttributeSet[] asets, XPathContext context) throws XPathException {
        for (AttributeSet aset : asets) {
            aset.expand(context);
        }
    }

    public StructuredQName getConstructType() {
        return new StructuredQName("xsl", NamespaceConstant.XSLT, "attribute-set");
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
