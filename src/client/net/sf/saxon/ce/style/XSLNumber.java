package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.RoleLocator;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.expr.TypeChecker;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.NumberInstruction;
import client.net.sf.saxon.ce.expr.instruct.ValueOf;
import client.net.sf.saxon.ce.expr.number.NumberFormatter;
import client.net.sf.saxon.ce.expr.number.Numberer_en;
import client.net.sf.saxon.ce.lib.Numberer;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.StringValue;

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

		String levelAtt;

        select = (Expression)checkAttribute("select", "e");
        value = (Expression)checkAttribute("value", "e");
        count = (Pattern)checkAttribute("count", "p");
        from = (Pattern)checkAttribute("from", "p");
        levelAtt = (String)checkAttribute("level", "w");
        format = (Expression)checkAttribute("format", "a");
        lang = (Expression)checkAttribute("lang", "a");
        letterValue = (Expression)checkAttribute("letter-value", "a");
        groupSize = (Expression)checkAttribute("grouping-size", "a");
        groupSeparator = (Expression)checkAttribute("grouping-separator", "a");
        ordinal = (Expression)checkAttribute("ordinal", "a");
        checkForUnknownAttributes();


        if (value!=null) {
            if (select != null || count != null || from != null || levelAtt != null) {
                compileError("If the value attribute is present then select, count, from, and level must be absent", "XTSE0975");
            }
        }

        // the following test is a very crude way of testing if the pattern might
        // contain variables, but it's good enough...
        if ((count != null && count.toString().indexOf('$')>=0) || (count != null && count.toString().indexOf('$')>=0)) {
            hasVariablesInPatterns = true;
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

        if (format != null) {
            if (format instanceof StringLiteral) {
                formatter = new NumberFormatter();
                formatter.prepare(((StringLiteral)format).getStringValue());
            }
            // else we'll need to allocate the formatter at run-time
        } else {
            formatter = new NumberFormatter();
            formatter.prepare("1");
        }

        if ((groupSize == null) != (groupSeparator == null)) {
            // the spec says that if only one is specified, it is ignored
            groupSize = null;
            groupSeparator = null;
        }

        if (lang==null) {
            numberer = new Numberer_en();
        } else {
            if (lang instanceof StringLiteral) {
                String language = ((StringLiteral)lang).getStringValue();
                if (language.length() != 0) {
                    if (!StringValue.isValidLanguageCode(language)) {
                        compileError("The lang attribute must be a valid language code", "XTDE0030");
                        lang = new StringLiteral(StringValue.EMPTY_STRING);
                    }
                }
                numberer = new Numberer_en();
            }   // no localisation support currently
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
                                            false, role);
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
