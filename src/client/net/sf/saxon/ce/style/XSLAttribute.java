package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.instruct.*;
import client.net.sf.saxon.ce.om.InscopeNamespaceResolver;
import client.net.sf.saxon.ce.om.NamespaceResolver;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.logging.client.LogConfiguration;

/**
* xsl:attribute element in stylesheet. <br>
*/

public class XSLAttribute extends XSLLeafNodeConstructor {

    private Expression attributeName;
    private Expression separator;
    private Expression namespace;

    public void prepareAttributes() throws XPathException {

        attributeName = (Expression)checkAttribute("name", "a1");
        namespace = (Expression)checkAttribute("namespace", "a");
        select = (Expression)checkAttribute("select", "e");
        separator = (Expression)checkAttribute("separator", "a");
        checkAttribute("validation", "v");
        checkAttribute("type", "t");
        checkForUnknownAttributes();


        if (separator == null) {
            if (select == null) {
                separator = new StringLiteral(StringValue.EMPTY_STRING);
            } else {
                separator = new StringLiteral(StringValue.SINGLE_SPACE);
            }
        }

    }

    public void validate(Declaration decl) throws XPathException {
        attributeName = typeCheck(attributeName);
        namespace = typeCheck(namespace);
        select = typeCheck(select);
        separator = typeCheck(separator);
        super.validate(decl);
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0840";
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        // deal specially with the simple case of an unprefixed attribute whose name is known statically

        SimpleNodeConstructor inst;
        if (attributeName instanceof StringLiteral && namespace == null &&
                ((StringLiteral)attributeName).getStringValue().indexOf(':') < 0) {
            String localName = Whitespace.trim(((StringLiteral)attributeName).getStringValue());
            inst = new FixedAttribute(new StructuredQName("", "", localName));
        } else {
            NamespaceResolver resolver = new InscopeNamespaceResolver(this);
            inst = new ComputedAttribute(attributeName, namespace, resolver);
        }
        inst.setContainer(this);     // temporarily
        compileContent(exec, decl, inst, separator);
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
        	inst.AddTraceProperty("name", attributeName);
        }
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
