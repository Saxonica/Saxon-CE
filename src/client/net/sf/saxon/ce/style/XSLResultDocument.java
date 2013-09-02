package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.Literal;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.ResultDocument;
import client.net.sf.saxon.ce.lib.Validation;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.Axis;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.HashSet;

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

    private static final HashSet fans = new HashSet(25);    // formatting attribute names

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
    }

    private Expression href;
    private StructuredQName formatQName;     // used when format is a literal string
    private Expression methodExpression;     // used when format is an AVT
    private Expression method;


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

    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();

        String methodAtt = null;
        String hrefAttribute = null;
        String validationAtt = null;
        String typeAtt = null;
        String useCharacterMapsAtt = null;


		for (int a=0; a<atts.getLength(); a++) {
			StructuredQName qn = atts.getStructuredQName(a);
            String f = qn.getClarkName();
			if (f.equals("method")) {
        		methodAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals("href")) {
        		hrefAttribute = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("validation")) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("type")) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("use-character-maps")) {
                useCharacterMapsAtt = Whitespace.trim(atts.getValue(a));
            } else if (fans.contains(f) || f.startsWith("{")) {
                // this is a serialization attribute
                String val = Whitespace.trim(atts.getValue(a));
                Expression exp = makeAttributeValueTemplate(val);
                //serializationAttributes.put(nc&0xfffff, exp);
        	} else {
        		checkUnknownAttribute(qn);
        	}
        }

        if (hrefAttribute==null) {
            //href = StringValue.EMPTY_STRING;
        } else {
            href = makeAttributeValueTemplate(hrefAttribute);
        }

        if (methodAtt!=null) {
            methodExpression = makeAttributeValueTemplate(methodAtt);
        }

        if (validationAtt!=null && Validation.getCode(validationAtt) != Validation.STRIP) {
            compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
        }
        if (typeAtt!=null) {
            compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
        }

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

        ResultDocument inst = new ResultDocument(href, methodExpression, getBaseURI(), this);

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