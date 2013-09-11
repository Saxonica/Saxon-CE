package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.ResultDocument;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.InscopeNamespaceResolver;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.EmptySequence;

import java.util.ArrayList;
import java.util.List;

/**
 * An xsl:result-document element in the stylesheet. <BR>
 * The xsl:result-document element takes an attribute href="filename". The filename will
 * often contain parameters, e.g. {position()} to ensure that a different file is produced
 * for each element instance. <BR>
 * There is a further attribute "name" which determines the format of the
 * output file, it identifies the name of an xsl:output element containing the output
 * format details.
 */

public class XSLResultDocument extends StyleElement {

    //    When serialization is not being performed, either because the implementation does not support the
    //    serialization option, or because the user is executing the transformation in a way that does not
    //    invoke serialization, then the content of the xsl:output and xsl:character-map declarations has no effect.
    //    Under these circumstances the processor may report any errors in an xsl:output or xsl:character-map
    //    declaration, or in the serialization attributes of xsl:result-document, but is not required to do so.

    private static final List<String> fans = new ArrayList<String>(25);    // formatting attribute names

    static {
        fans.add("method");
        fans.add("output-version");
        fans.add("byte-order-mark");
        fans.add("indent");
        fans.add("encoding");
        fans.add("media-type");
        fans.add("doctype-system");
        fans.add("doctype-public");
        fans.add("omit-xml-declaration");
        fans.add("standalone");
        fans.add("cdata-section-elements");
        fans.add("include-content-type");
        fans.add("escape-uri-attributes");
        fans.add("undeclare-prefixes");
        fans.add("normalization-form");
        fans.add("use-character-maps");
    }

    private Expression href;
    private StructuredQName formatQName;     // used when format is a literal string
    private Expression methodExpression;     // used when format is an AVT
    private Expression method;


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

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return null;
    }

    public void prepareAttributes() throws XPathException {
        for (String att : fans) {
            checkAttribute(att, "s");
        }
        methodExpression = (Expression)checkAttribute("method", "a");
        checkAttribute("validation", "v");
        checkAttribute("type", "t");
        checkAttribute("format", "q");
        href = (Expression)checkAttribute("href", "a");
        checkForUnknownAttributes();
    }

    public void validate(Declaration decl) throws XPathException {
        if (href != null && !getConfiguration().isAllowExternalFunctions()) {
            compileError("xsl:result-document is disabled when extension functions are disabled");
        }
        href = typeCheck(href);
        methodExpression = typeCheck(methodExpression);

        getExecutable().setCreatesSecondaryResult(true);

    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        ResultDocument inst = new ResultDocument(href, methodExpression, getBaseURI(), new InscopeNamespaceResolver(this));

        Expression b = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD));
        if (b == null) {
            b = new Literal(EmptySequence.getInstance());
        }
        inst.setContentExpression(b);
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.//