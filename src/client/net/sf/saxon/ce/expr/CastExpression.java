package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.functions.StringFn;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.EmptySequenceTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;

import java.util.HashMap;

/**
* Cast Expression: implements "cast as data-type ( expression )". It also allows an internal
* cast, which has the same semantics as a user-requested cast, but maps an empty sequence to
* an empty sequence.
*/

public final class CastExpression extends UnaryExpression  {

    private AtomicType targetType;
    private AtomicType targetPrimitiveType;
    private boolean allowEmpty = false;
    private boolean upcast = false;

    static HashMap<AtomicType, AtomicType[]> castingTable =
            new HashMap<AtomicType, AtomicType[]>(25);

    static void addAllowedCasts(AtomicType source, AtomicType[] target) {
        castingTable.put(source, target);
    }

    /**
     * The following data represents all the "Y" and "M" entries in section 17.1 of the F+O spec.
     */

    static {
        final AtomicType uat = AtomicType.UNTYPED_ATOMIC;
        final AtomicType str = AtomicType.STRING;
        final AtomicType flt = AtomicType.FLOAT;
        final AtomicType dbl = AtomicType.DOUBLE;
        final AtomicType dec = AtomicType.DECIMAL;
        final AtomicType ing = AtomicType.INTEGER;
        final AtomicType dur = AtomicType.DURATION;
        final AtomicType ymd = AtomicType.YEAR_MONTH_DURATION;
        final AtomicType dtd = AtomicType.DAY_TIME_DURATION;
        final AtomicType dtm = AtomicType.DATE_TIME;
        final AtomicType tim = AtomicType.TIME;
        final AtomicType dat = AtomicType.DATE;
        final AtomicType gym = AtomicType.G_YEAR_MONTH;
        final AtomicType gyr = AtomicType.G_YEAR;
        final AtomicType gmd = AtomicType.G_MONTH_DAY;
        final AtomicType gdy = AtomicType.G_DAY;
        final AtomicType gmo = AtomicType.G_MONTH;
        final AtomicType boo = AtomicType.BOOLEAN;
        final AtomicType b64 = AtomicType.BASE64_BINARY;
        final AtomicType hxb = AtomicType.HEX_BINARY;
        final AtomicType uri = AtomicType.ANY_URI;
        final AtomicType qnm = AtomicType.QNAME;

        final AtomicType[] t01 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri};
        addAllowedCasts(uat, t01);
        final AtomicType[] t02 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri, qnm};
        addAllowedCasts(str, t02);
        final AtomicType[] t03 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(flt, t03);
        addAllowedCasts(dbl, t03);
        addAllowedCasts(dec, t03);
        addAllowedCasts(ing, t03);
        final AtomicType[] t04 = {uat, str, dur, ymd, dtd};
        addAllowedCasts(dur, t04);
        addAllowedCasts(ymd, t04);
        addAllowedCasts(dtd, t04);
        final AtomicType[] t05 = {uat, str, dtm, tim, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dtm, t05);
        final AtomicType[] t06 = {uat, str, tim};
        addAllowedCasts(tim, t06);
        final AtomicType[] t07 = {uat, str, dtm, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dat, t07);
        final AtomicType[] t08 = {uat, str, gym};
        addAllowedCasts(gym, t08);
        final AtomicType[] t09 = {uat, str, gyr};
        addAllowedCasts(gyr, t09);
        final AtomicType[] t10 = {uat, str, gmd};
        addAllowedCasts(gmd, t10);
        final AtomicType[] t11 = {uat, str, gdy};
        addAllowedCasts(gdy, t11);
        final AtomicType[] t12 = {uat, str, gmo};
        addAllowedCasts(gmo, t12);
        final AtomicType[] t13 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(boo, t13);
        final AtomicType[] t14 = {uat, str, b64, hxb};
        addAllowedCasts(b64, t14);
        addAllowedCasts(hxb, t14);
        final AtomicType[] t15 = {uat, str, uri};
        addAllowedCasts(uri, t15);
        final AtomicType[] t16 = {uat, str, qnm};
        addAllowedCasts(qnm, t16);
    }

    /**
     * Determine whether casting from a source type to a target type is possible
     * @param source a primitive type (one that has an entry in the casting table)
     * @param target another primitive type
     * @return true if the entry in the casting table is either "Y" (casting always succeeds)
     * or "M" (casting allowed but may fail for some values)
     */

    public static boolean isPossibleCast(AtomicType source, AtomicType target) {
        if (source == AtomicType.ANY_ATOMIC) {
            return true;
        }
        if (source == AtomicType.NUMERIC) {
            source = AtomicType.DOUBLE;
        }
        AtomicType[] targets = castingTable.get(source);
        if (targets == null) {
            return false;
        }
        for (AtomicType t : targets) {
            if (t == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a cast expression
     * @param source expression giving the value to be converted
     * @param target the type to which the value is to be converted
     * @param allowEmpty true if the expression allows an empty sequence as input, producing
     * an empty sequence as output. If false, an empty sequence is a type error.
     */

    public CastExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source);
        this.allowEmpty = allowEmpty;
        targetType = target;
        targetPrimitiveType = (AtomicType)target.getPrimitiveItemType();
        adoptChildExpression(source);
    }

    /**
     * Get the target type (the result type)
     * @return the target type
     */

    public AtomicType getTargetType() {
        return targetType;
    }

    /**
     * Simplify the expression
     * @return the simplified expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (Literal.isAtomic(operand)) {
            return typeCheck(visitor, Type.ITEM_TYPE);
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        SequenceType atomicType = SequenceType.makeSequenceType(AtomicType.ANY_ATOMIC, getCardinality());

        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "cast as", 0);
        //role.setSourceLocator(this);
        operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role);

        final TypeHierarchy th = TypeHierarchy.getInstance();
        ItemType sourceType = operand.getItemType();
        int r = th.relationship(sourceType, targetType);
        if (r == TypeHierarchy.SAME_TYPE) {
            return operand;
        } else if (r == TypeHierarchy.SUBSUMED_BY) {
            // It's generally true that any expression defined to return an X is allowed to return a subtype of X.
            // However, people seem to get upset if we treat the cast as a no-op.
            upcast = true;
            return this;
        }

        if (operand instanceof Literal) {
            Value literalOperand = ((Literal)operand).getValue();
            if (literalOperand instanceof AtomicValue) {
                AtomicValue av = ((AtomicValue)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()));
                if (av instanceof StringValue) {
                    return new StringLiteral((StringValue)av);
                } else {
                    return new Literal(av);
                }
            }
            if (literalOperand instanceof EmptySequence) {
                if (allowEmpty) {
                    return operand;
                } else {
                    typeError("Cast can never succeed: the operand must not be an empty sequence", "XPTY0004");
                }
            }
        }
        if (sourceType != EmptySequenceTest.getInstance()) {
            AtomicType p = sourceType.getAtomizedItemType();
            if (!isPossibleCast(p, targetType)) {
                typeError("Casting from " + sourceType + " to " + targetType +
                        " can never succeed", "XPTY0004");
            }
        }

        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = TypeHierarchy.getInstance();
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // Eliminate pointless casting between untypedAtomic and string
        if (targetType == AtomicType.UNTYPED_ATOMIC) {
            if (operand instanceof StringFn) {
                Expression e = ((StringFn)operand).getArguments()[0];
                if (e.getItemType() instanceof AtomicType && e.getCardinality() == StaticProperty.EXACTLY_ONE) {
                    operand = e;
                }
            }
        }
        // avoid converting anything to a string and then back again
        if (operand instanceof StringFn) {
            Expression e = ((StringFn)operand).getArguments()[0];
            ItemType et = e.getItemType();
            if (et instanceof AtomicType &&
                    e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                    th.isSubType(et, targetType)) {
                return e;
            }
        }
        // avoid converting anything to untypedAtomic and then back again
        if (operand instanceof CastExpression) {
            ItemType it = ((CastExpression)operand).targetType;
            if (th.isSubType(it, AtomicType.STRING) || th.isSubType(it, AtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((CastExpression)operand).getBaseExpression();
                ItemType et = e.getItemType();
                if (et instanceof AtomicType &&
                        e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                        th.isSubType(et, targetType)) {
                    return e;
                }
            }
        }
        // if the operand can't be empty, then set allowEmpty to false to provide more information for analysis
        if (!Cardinality.allowsZero(operand.getCardinality())) {
            allowEmpty = false;
            resetLocalStaticProperties();
        }
        return this;
    }

    /**
    * Get the static cardinality of the expression
    */

    public int computeCardinality() {
        return (allowEmpty && Cardinality.allowsZero(operand.getCardinality())
                ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE);
    }

    /**
     * Get the static type of the expression
     */

    public ItemType getItemType() {
        return targetType;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue)operand.evaluateItem(context);
        if (value==null) {
            if (allowEmpty) {
                return null;
            } else {
                throw new XPathException("Cast does not allow an empty sequence", "XPTY0004", getSourceLocator());
            }
        }
        if (upcast) {
            // When casting to a supertype of the original type, we can bypass validation
            return (AtomicValue)value.convert(targetPrimitiveType);
        }
        ConversionResult result = value.convert(targetType);
        if (result instanceof ValidationFailure) {
            ValidationFailure err = (ValidationFailure)result;
            StructuredQName code = err.getErrorCodeQName();
            String lcode = (code == null ? null : code.getLocalName());
            if (lcode == null) {
                lcode = "FORG0001";
            }
            dynamicError(err.getMessage(), lcode);
            return null;
        }
        return (AtomicValue)result;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((CastExpression)other).targetType &&
                allowEmpty == ((CastExpression)other).allowEmpty;
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ targetType.hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return targetType.toString() + "(" + operand.toString() + ")";
    }

    /**
     * Evaluate the "pseudo-cast" of a string literal to a QName or NOTATION value. This can only happen
     * at compile time
     *
     * @param operand the value to be converted
     * @param env the static context
     * @return the QName or NOTATION value that results from casting the string to a QName.
     * This will either be a QNameValue or a derived AtomicValue derived from QName or NOTATION
     */

    public static AtomicValue castStringToQName(
            CharSequence operand, StaticContext env) throws XPathException {
        try {
            String arg = Whitespace.trimWhitespace(operand).toString();
            StructuredQName qn = StructuredQName.fromLexicalQName(arg, "", env.getNamespaceResolver());
            return new QNameValue(qn);

        } catch (XPathException err) {
            if (err.getErrorCodeQName() == null) {
                err.setErrorCode("FONS0004");
            }
            throw err;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.