package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.RoleLocator;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.TypeChecker;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.NumberInstruction;
import client.net.sf.saxon.ce.expr.instruct.ValueOf;
import client.net.sf.saxon.ce.expr.number.NumberFormatter;
import client.net.sf.saxon.ce.lib.Numberer;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.Whitespace;

/**
* An xsl:number element in the stylesheet. <br>
*/

public class XSLNumber extends StyleElement {

    private static final int SINGLE = 0;
    private static final int MULTI = 1;
    private static final int ANY = 2;
    private static final int SIMPLE = 3;

    private int level;
    private Pattern count = null;
    private Pattern from = null;
    private Expression select = null;
    private Expression value = null;
    private Expression format = null;
    private Expression groupSize = null;
    private Expression groupSeparator = null;
    private Expression letterValue = null;
    private Expression lang = null;
    private Expression ordinal = null;
    private NumberFormatter formatter = null;
    private Numberer numberer = null;
    private boolean hasVariablesInPatterns = false;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return NodeKindTest.TEXT;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;
        String valueAtt = null;
		String countAtt = null;
		String fromAtt = null;
		String levelAtt = null;
		String formatAtt = null;
		String gsizeAtt = null;
		String gsepAtt = null;
		String langAtt = null;
		String letterValueAtt = null;
        String ordinalAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.VALUE)) {
        		valueAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.COUNT)) {
        		countAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.FROM)) {
        		fromAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.LEVEL)) {
        		levelAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.FORMAT)) {
        		formatAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.LANG)) {
        		langAtt = atts.getValue(a);
        	} else if (f.equals(StandardNames.LETTER_VALUE)) {
        		letterValueAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.GROUPING_SIZE)) {
        		gsizeAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.GROUPING_SEPARATOR)) {
        		gsepAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.ORDINAL)) {
                ordinalAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt != null) {
            select = makeExpression(selectAtt);
        }

        if (valueAtt!=null) {
            value = makeExpression(valueAtt);
            if (selectAtt != null) {
                compileError("The select attribute and value attribute must not both be present", "XTSE0975");
            }
            if (countAtt != null) {
                compileError("The count attribute and value attribute must not both be present", "XTSE0975");
            }
            if (fromAtt != null) {
                compileError("The from attribute and value attribute must not both be present", "XTSE0975");
            }
            if (levelAtt != null) {
                compileError("The level attribute and value attribute must not both be present", "XTSE0975");
            }
        }

        if (countAtt!=null) {
            count = makePattern(countAtt);
            // the following test is a very crude way of testing if the pattern might
            // contain variables, but it's good enough...
            if (countAtt.indexOf('$')>=0) {
                hasVariablesInPatterns = true;
            }
        }

        if (fromAtt!=null) {
            from = makePattern(fromAtt);
            if (fromAtt.indexOf('$')>=0) {
                hasVariablesInPatterns = true;
            }
        }

        if (levelAtt==null) {
            level = SINGLE;
        } else if (levelAtt.equals("single")) {
            level = SINGLE;
        } else if (levelAtt.equals("multiple")) {
            level = MULTI;
        } else if (levelAtt.equals("any")) {
            level = ANY;
        } else {
            compileError("Invalid value for level attribute", "XTSE0020");
        }

        if (level==SINGLE && from==null && count==null) {
            level=SIMPLE;
        }

        if (formatAtt != null) {
            format = makeAttributeValueTemplate(formatAtt);
            if (format instanceof StringLiteral) {
                formatter = new NumberFormatter();
                formatter.prepare(((StringLiteral)format).getStringValue());
            }
            // else we'll need to allocate the formatter at run-time
        } else {
            formatter = new NumberFormatter();
            formatter.prepare("1");
        }

        if (gsepAtt!=null && gsizeAtt!=null) {
            // the spec says that if only one is specified, it is ignored
            groupSize = makeAttributeValueTemplate(gsizeAtt);
            groupSeparator = makeAttributeValueTemplate(gsepAtt);
        }

        if (langAtt==null) {
            numberer = getConfiguration().makeNumberer(null, null);
        } else {
            lang = makeAttributeValueTemplate(langAtt);
            if (lang instanceof StringLiteral) {
                String language = ((StringLiteral)lang).getStringValue();
                if (language.length() != 0) {
                    if (!StringValue.isValidLanguageCode(language)) {
                        compileError("The lang attribute must be a valid language code", "XTDE0030");
                        lang = new StringLiteral(StringValue.EMPTY_STRING);
                    }
                }
                numberer = getConfiguration().makeNumberer(language, null);
            }   // else we allocate a numberer at run-time
        }

        if (letterValueAtt != null) {
            letterValue = makeAttributeValueTemplate(letterValueAtt);
        }

        if (ordinalAtt != null) {
            ordinal = makeAttributeValueTemplate(ordinalAtt);
        }

    }

    public void validate(Declaration decl) throws XPathException {
        checkEmpty();

        select = typeCheck(select);
        value = typeCheck(value);
        format = typeCheck(format);
        groupSize = typeCheck(groupSize);
        groupSeparator = typeCheck(groupSeparator);
        letterValue = typeCheck(letterValue);
        ordinal = typeCheck(ordinal);
        lang = typeCheck(lang);
        from = typeCheck("from", from);
        count = typeCheck("count", count);

        if (select != null) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:number/select", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                role.setErrorCode("XTTE1000");
                select = TypeChecker.staticTypeCheck(select,
                                            SequenceType.SINGLE_NODE,
                                            false, role, makeExpressionVisitor());
            } catch (XPathException err) {
                compileError(err);
            }
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        NumberInstruction expr = new NumberInstruction (exec.getConfiguration(),
                                        select,
                                        level,
                                        count,
                                        from,
                                        value,
                                        format,
                                        groupSize,
                                        groupSeparator,
                                        letterValue,
                                        ordinal,
                                        lang,
                                        formatter,
                                        numberer,
                                        hasVariablesInPatterns,
                                        xPath10ModeIsEnabled());
        expr.setSourceLocator(this);
        ValueOf inst = new ValueOf(expr, false);
        inst.setSourceLocator(this);
        inst.setIsNumberingInstruction();
        return inst;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
