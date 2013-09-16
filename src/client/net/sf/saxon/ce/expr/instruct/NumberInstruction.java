package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.number.NumberFormatter;
import client.net.sf.saxon.ce.expr.number.Numberer_en;
import client.net.sf.saxon.ce.functions.NumberFn;
import client.net.sf.saxon.ce.lib.Numberer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.pattern.PatternSponsor;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * An xsl:number element in the stylesheet. Although this is an XSLT instruction, it is compiled
 * into an expression, evaluated using xsl:value-of to create the resulting text node.<br>
 */

public class NumberInstruction extends Expression {

    private static final int SINGLE = 0;
    private static final int MULTI = 1;
    private static final int ANY = 2;
    private static final int SIMPLE = 3;

    private int level;
    private Pattern count = null;
    private Pattern from = null;
    private Expression[] arguments = new Expression[8];
    private static final int SELECT = 0;
    private static final int VALUE = 1;
    private static final int FORMAT = 2;
    private static final int GROUP_SIZE = 3;
    private static final int GROUP_SEPARATOR = 4;
    private static final int LETTER_VALUE = 5;
    private static final int ORDINAL = 6;
    private static final int LANG = 7;
    private static final int ARGS = 8;
//    private Expression select = null;
//    private Expression value = null;
//    private Expression format = null;
//    private Expression groupSize = null;
//    private Expression groupSeparator = null;
//    private Expression letterValue = null;
//    private Expression ordinal = null;
//    private Expression lang = null;
    private NumberFormatter formatter = null;
    private Numberer numberer = null;
    private boolean hasVariablesInPatterns;
    private boolean backwardsCompatible;

    /**
     * Construct a NumberInstruction
     * @param config the Saxon configuration
     * @param select the expression supplied in the select attribute
     * @param level one of "single", "level", "multi"
     * @param count the pattern supplied in the count attribute
     * @param from the pattern supplied in the from attribute
     * @param value the expression supplied in the value attribute
     * @param format the expression supplied in the format attribute
     * @param groupSize the expression supplied in the group-size attribute
     * @param groupSeparator the expression supplied in the grouping-separator attribute
     * @param letterValue the expression supplied in the letter-value attribute
     * @param ordinal the expression supplied in the ordinal attribute
     * @param lang the expression supplied in the lang attribute
     * @param formatter A NumberFormatter to be used
     * @param numberer A Numberer to be used for localization
     * @param hasVariablesInPatterns true if one or more of the patterns contains variable references
     * @param backwardsCompatible true if running in 1.0 compatibility mode
     */

    public NumberInstruction(Configuration config,
                             Expression select,
                             int level,
                             Pattern count,
                             Pattern from,
                             Expression value,
                             Expression format,
                             Expression groupSize,
                             Expression groupSeparator,
                             Expression letterValue,
                             Expression ordinal,
                             Expression lang,
                             NumberFormatter formatter,
                             Numberer numberer,
                             boolean hasVariablesInPatterns,
                             boolean backwardsCompatible) {
        arguments[SELECT] = select;
        this.level = level;
        this.count = count;
        this.from = from;
        arguments[VALUE] = value;
        arguments[FORMAT] = format;
        arguments[GROUP_SIZE] = groupSize;
        arguments[GROUP_SEPARATOR] = groupSeparator;
        arguments[LETTER_VALUE] = letterValue;
        arguments[ORDINAL] = ordinal;
        arguments[LANG] = lang;
        this.formatter = formatter;
        this.numberer = numberer;
        this.hasVariablesInPatterns = hasVariablesInPatterns;
        this.backwardsCompatible = backwardsCompatible;

        final TypeHierarchy th = TypeHierarchy.getInstance();
        if (arguments[VALUE] != null && !arguments[VALUE].getItemType().isAtomicType()) {
            arguments[VALUE] = new Atomizer(arguments[VALUE]);
        }

        Iterator<Expression> kids = iterateSubExpressions();
        while (kids.hasNext()) {
            Expression child = kids.next();
            adoptChildExpression(child);
        }
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        for (int i=0; i<ARGS; i++) {
            arguments[i] = visitor.simplify(arguments[i]);
        }
        if (count != null) {
            count = count.simplify(visitor);
        }
        if (from != null) {
            from = from.simplify(visitor);
        }
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @throws XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (arguments[SELECT] == null && arguments[VALUE]==null) {
            // we are numbering the context node
            XPathException err = null;
            if (contextItemType == null) {
                err = new XPathException(
                        "xsl:number requires a select attribute, a value attribute, or a context item");
            } else if (contextItemType.isAtomicType()) {
                err = new XPathException(
                        "xsl:number requires the context item to be a node, but it is an atomic value");

            }
            if (err != null) {
                err.setIsTypeError(true);
                err.setErrorCode("XTTE0990");
                err.setLocator(getSourceLocator());
                throw err;
            }
        }
        for (int i=0; i<ARGS; i++) {
            arguments[i] = visitor.typeCheck(arguments[i], contextItemType);
        }
        if (count != null) {
            visitor.typeCheck(new PatternSponsor(count), contextItemType);
        }
        if (from != null) {
            visitor.typeCheck(new PatternSponsor(from), contextItemType);
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int i=0; i<ARGS; i++) {
            arguments[i] = visitor.optimize(arguments[i], contextItemType);
        }
        return this;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator<Expression> iterateSubExpressions() {
        List<Expression> sub = new ArrayList<Expression>(9);
        for (int i=0; i<ARGS; i++) {
            if (arguments[i] != null) {
                sub.add(arguments[i]);
            }
        }
        if (count != null) {
            sub.add(new PatternSponsor(count));
        }
        if (from != null) {
            sub.add(new PatternSponsor(from));
        }
        return sub.iterator();
    }


    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class client.net.sf.saxon.ce.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return ((arguments[SELECT] == null && arguments[VALUE] == null) ? StaticProperty.DEPENDS_ON_CONTEXT_ITEM : 0);
    }

    public ItemType getItemType() {
        return AtomicType.STRING;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp!=null) {
            return exp;
        } else {
            for (int i=0; i<ARGS; i++) {
                if (arguments[i] != null) {
                    arguments[i] = doPromotion(arguments[i], offer);
                }
            }
            if (count != null) {
                count.promote(offer, this);
            }
            if (from != null) {
                from.promote(offer, this);
            }
            return this;
        }
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        long value = -1;
        List vec = null;    // a list whose items may be of type either Long or
                            // BigDecimal or the string to be output (e.g. "NaN")
        if (arguments[VALUE] != null) {

            SequenceIterator iter = arguments[VALUE].iterate(context);
            vec = new ArrayList(4);
            while (true) {
                AtomicValue val = (AtomicValue) iter.next();
                if (val == null) {
                    break;
                }
                if (backwardsCompatible && !vec.isEmpty()) {
                    break;
                }
                try {
                    NumericValue num;
                    if (val instanceof NumericValue) {
                        num = (NumericValue) val;
                    } else {
                        num = NumberFn.convert(val);
                    }
                    if (num.isNaN()) {
                        throw new XPathException("NaN");  // thrown to be caught
                    }
                    num = num.round();
                    if (num.signum() >= 0) {
                        long i = ((NumericValue)num.convert(AtomicType.INTEGER).asAtomic()).intValue();
                        vec.add(Long.valueOf(i));
                    } else {
                        if (num.compareTo(IntegerValue.ZERO) < 0) {
                            throw new XPathException("The numbers to be formatted must not be negative");
                            // thrown to be caught
                        }
                        long i = ((NumericValue)num.convert(AtomicType.INTEGER).asAtomic()).intValue();
                        vec.add(Long.valueOf(i));
                    }
                } catch (XPathException err) {
                    if (backwardsCompatible) {
                        vec.add("NaN");
                    } else {
                        vec.add(val.getStringValue());
                        throw new XPathException("Cannot convert supplied value to an integer. " + err.getMessage(), "XTDE0980");
                    }
                }
            }
            if (backwardsCompatible && vec.isEmpty()) {
                vec.add("NaN");
            }
        } else {
            NodeInfo source;
            if (arguments[SELECT] != null) {
                source = (NodeInfo) arguments[SELECT].evaluateItem(context);
            } else {
                Item item = context.getContextItem();
                if (!(item instanceof NodeInfo)) {
                    XPathException err = new XPathException("context item for xsl:number must be a node", "XTTE0990");
                    err.setIsTypeError(true);
                    throw err;
                }
                source = (NodeInfo) item;
            }

            if (level == SIMPLE) {
                value = Navigator.getNumberSimple(source);
            } else if (level == SINGLE) {
                value = Navigator.getNumberSingle(source, count, from, context);
                if (value == 0) {
                    vec = Collections.EMPTY_LIST; 	// an empty list
                }
            } else if (level == ANY) {
                value = Navigator.getNumberAny(this, source, count, from, context, hasVariablesInPatterns);
                if (value == 0) {
                    vec = Collections.EMPTY_LIST; 	// an empty list
                }
            } else if (level == MULTI) {
                vec = Navigator.getNumberMulti(source, count, from, context);
            }
        }

        int gpsize = 0;
        String gpseparator = "";
        String letterVal;
        String ordinalVal = null;

        if (arguments[GROUP_SIZE] != null) {
            String g = arguments[GROUP_SIZE].evaluateAsString(context).toString();
            try {
                gpsize = Integer.parseInt(g);
            } catch (NumberFormatException err) {
                throw new XPathException("grouping-size must be numeric", "XTDE0030");
            }
        }

        if (arguments[GROUP_SEPARATOR] != null) {
            gpseparator = arguments[GROUP_SEPARATOR].evaluateAsString(context).toString();
        }

        if (arguments[ORDINAL] != null) {
            ordinalVal = arguments[ORDINAL].evaluateAsString(context).toString();
        }

        // fast path for the simple case

        if (vec == null && arguments[FORMAT] == null && gpsize == 0 && arguments[LANG] == null) {
            return new StringValue("" + value);
        }

        // Use the numberer decided at compile time if possible; otherwise try to get it from
        // a table of numberers indexed by language; if not there, load the relevant class and
        // add it to the table.
        Numberer numb = numberer;
        if (numb == null) {
            String language = arguments[LANG].evaluateAsString(context).toString();
            if (!StringValue.isValidLanguageCode(language)) {
                 throw new XPathException("The lang attribute of xsl:number must be a valid language code", "XTDE0030");
            }   
            numb = new Numberer_en();
        }

        if (arguments[LETTER_VALUE] == null) {
            letterVal = "";
        } else {
            letterVal = arguments[LETTER_VALUE].evaluateAsString(context).toString();
            if (!("alphabetic".equals(letterVal) || "traditional".equals(letterVal))) {
                throw new XPathException("letter-value must be \"traditional\" or \"alphabetic\"", "XTDE0030");
            }
        }

        if (vec == null) {
            vec = new ArrayList(1);
            vec.add(value);
        }

        NumberFormatter nf;
        if (formatter == null) {              // format not known until run-time
            nf = new NumberFormatter();
            nf.prepare(arguments[FORMAT].evaluateAsString(context).toString());
        } else {
            nf = formatter;
        }

        CharSequence s = nf.format(vec, gpsize, gpseparator, letterVal, ordinalVal, numb);
        return new StringValue(s);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
