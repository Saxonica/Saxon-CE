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

    private BuiltInAtomicType targetType;
    private BuiltInAtomicType targetPrimitiveType;
    private boolean allowEmpty = false;
    private boolean upcast = false;

    static HashMap<BuiltInAtomicType, BuiltInAtomicType[]> castingTable =
            new HashMap<BuiltInAtomicType, BuiltInAtomicType[]>(25);

    static void addAllowedCasts(BuiltInAtomicType source, BuiltInAtomicType[] target) {
        castingTable.put(source, target);
    }

    /**
     * The following data represents all the "Y" and "M" entries in section 17.1 of the F+O spec.
     */

    static {
        final BuiltInAtomicType uat = BuiltInAtomicType.UNTYPED_ATOMIC;
        final BuiltInAtomicType str = BuiltInAtomicType.STRING;
        final BuiltInAtomicType flt = BuiltInAtomicType.FLOAT;
        final BuiltInAtomicType dbl = BuiltInAtomicType.DOUBLE;
        final BuiltInAtomicType dec = BuiltInAtomicType.DECIMAL;
        final BuiltInAtomicType ing = BuiltInAtomicType.INTEGER;
        final BuiltInAtomicType dur = BuiltInAtomicType.DURATION;
        final BuiltInAtomicType ymd = BuiltInAtomicType.YEAR_MONTH_DURATION;
        final BuiltInAtomicType dtd = BuiltInAtomicType.DAY_TIME_DURATION;
        final BuiltInAtomicType dtm = BuiltInAtomicType.DATE_TIME;
        final BuiltInAtomicType tim = BuiltInAtomicType.TIME;
        final BuiltInAtomicType dat = BuiltInAtomicType.DATE;
        final BuiltInAtomicType gym = BuiltInAtomicType.G_YEAR_MONTH;
        final BuiltInAtomicType gyr = BuiltInAtomicType.G_YEAR;
        final BuiltInAtomicType gmd = BuiltInAtomicType.G_MONTH_DAY;
        final BuiltInAtomicType gdy = BuiltInAtomicType.G_DAY;
        final BuiltInAtomicType gmo = BuiltInAtomicType.G_MONTH;
        final BuiltInAtomicType boo = BuiltInAtomicType.BOOLEAN;
        final BuiltInAtomicType b64 = BuiltInAtomicType.BASE64_BINARY;
        final BuiltInAtomicType hxb = BuiltInAtomicType.HEX_BINARY;
        final BuiltInAtomicType uri = BuiltInAtomicType.ANY_URI;
        final BuiltInAtomicType qnm = BuiltInAtomicType.QNAME;

        final BuiltInAtomicType[] t01 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri};
        addAllowedCasts(uat, t01);
        final BuiltInAtomicType[] t02 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri, qnm};
        addAllowedCasts(str, t02);
        final BuiltInAtomicType[] t03 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(flt, t03);
        addAllowedCasts(dbl, t03);
        addAllowedCasts(dec, t03);
        addAllowedCasts(ing, t03);
        final BuiltInAtomicType[] t04 = {uat, str, dur, ymd, dtd};
        addAllowedCasts(dur, t04);
        addAllowedCasts(ymd, t04);
        addAllowedCasts(dtd, t04);
        final BuiltInAtomicType[] t05 = {uat, str, dtm, tim, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dtm, t05);
        final BuiltInAtomicType[] t06 = {uat, str, tim};
        addAllowedCasts(tim, t06);
        final BuiltInAtomicType[] t07 = {uat, str, dtm, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dat, t07);
        final BuiltInAtomicType[] t08 = {uat, str, gym};
        addAllowedCasts(gym, t08);
        final BuiltInAtomicType[] t09 = {uat, str, gyr};
        addAllowedCasts(gyr, t09);
        final BuiltInAtomicType[] t10 = {uat, str, gmd};
        addAllowedCasts(gmd, t10);
        final BuiltInAtomicType[] t11 = {uat, str, gdy};
        addAllowedCasts(gdy, t11);
        final BuiltInAtomicType[] t12 = {uat, str, gmo};
        addAllowedCasts(gmo, t12);
        final BuiltInAtomicType[] t13 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(boo, t13);
        final BuiltInAtomicType[] t14 = {uat, str, b64, hxb};
        addAllowedCasts(b64, t14);
        addAllowedCasts(hxb, t14);
        final BuiltInAtomicType[] t15 = {uat, str, uri};
        addAllowedCasts(uri, t15);
        final BuiltInAtomicType[] t16 = {uat, str, qnm};
        addAllowedCasts(qnm, t16);
    }

    /**
     * Determine whether casting from a source type to a target type is possible
     * @param source a primitive type (one that has an entry in the casting table)
     * @param target another primitive type
     * @return true if the entry in the casting table is either "Y" (casting always succeeds)
     * or "M" (casting allowed but may fail for some values)
     */

    public static boolean isPossibleCast(BuiltInAtomicType source, BuiltInAtomicType target) {
        if (source == BuiltInAtomicType.ANY_ATOMIC) {
            return true;
        }
        if (source == BuiltInAtomicType.NUMERIC) {
            source = BuiltInAtomicType.DOUBLE;
        }
        BuiltInAtomicType[] targets = castingTable.get(source);
        if (targets == null) {
            return false;
        }
        for (BuiltInAtomicType t : targets) {
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

    public CastExpression(Expression source, BuiltInAtomicType target, boolean allowEmpty) {
        super(source);
        this.allowEmpty = allowEmpty;
        targetType = target;
        targetPrimitiveType = (BuiltInAtomicType)target.getPrimitiveItemType();
        adoptChildExpression(source);
    }

    /**
     * Get the target type (the result type)
     * @return the target type
     */

    public BuiltInAtomicType getTargetType() {
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
        SequenceType atomicType = SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, getCardinality());

        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "cast as", 0);
        //role.setSourceLocator(this);
        operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, visitor);

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        ItemType sourceType = operand.getItemType(th);
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
                    typeError("Cast can never succeed: the operand must not be an empty sequence", "XPTY0004", null);
                }
            }
        }
        if (sourceType != EmptySequenceTest.getInstance()) {
            BuiltInAtomicType p = sourceType.getAtomizedItemType();
            if (!isPossibleCast(p, targetType)) {
                typeError("Casting from " + sourceType + " to " + targetType +
                        " can never succeed", "XPTY0004", null);
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
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // Eliminate pointless casting between untypedAtomic and string
        if (targetType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            if (operand instanceof StringFn) {
                Expression e = ((StringFn)operand).getArguments()[0];
                if (e.getItemType(th) instanceof BuiltInAtomicType && e.getCardinality() == StaticProperty.EXACTLY_ONE) {
                    operand = e;
                }
            }
        }
        // avoid converting anything to a string and then back again
        if (operand instanceof StringFn) {
            Expression e = ((StringFn)operand).getArguments()[0];
            ItemType et = e.getItemType(th);
            if (et instanceof BuiltInAtomicType &&
                    e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                    th.isSubType(et, targetType)) {
                return e;
            }
        }
        // avoid converting anything to untypedAtomic and then back again
        if (operand instanceof CastExpression) {
            ItemType it = ((CastExpression)operand).targetType;
            if (th.isSubType(it, BuiltInAtomicType.STRING) || th.isSubType(it, BuiltInAtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((CastExpression)operand).getBaseExpression();
                ItemType et = e.getItemType(th);
                if (et instanceof BuiltInAtomicType &&
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
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
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
                XPathException e = new XPathException("Cast does not allow an empty sequence");
                e.setXPathContext(context);
                e.setLocator(getSourceLocator());
                e.setErrorCode("XPTY0004");
                throw e;
            }
        }
        if (upcast) {
            // When casting to a supertype of the original type, we can bypass validation
            return (AtomicValue)value.convert(targetPrimitiveType, false);
        }
        ConversionResult result = value.convert(targetType, true);
        if (result instanceof ValidationFailure) {
            ValidationFailure err = (ValidationFailure)result;
            String code = err.getErrorCode();
            if (code == null) {
                code = "FORG0001";
            }
            dynamicError(err.getMessage(), code, context);
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
        try {
            NamePool pool = getExecutable().getConfiguration().getNamePool();
            return targetType.toString(pool) + "(" + operand.toString() + ")";
        } catch (Exception err) {
            return targetType.toString() + "(" + operand.toString() + ")";
        }
    }    

    /**
     * Evaluate the "pseudo-cast" of a string literal to a QName or NOTATION value. This can only happen
     * at compile time
     * @param operand the value to be converted
     * @param targetType the type to which it is to be converted
     * @param env the static context
     * @return the QName or NOTATION value that results from casting the string to a QName.
     * This will either be a QNameValue or a derived AtomicValue derived from QName or NOTATION
     */

    public static AtomicValue castStringToQName(
            CharSequence operand, BuiltInAtomicType targetType, StaticContext env) throws XPathException {
        try {
            CharSequence arg = Whitespace.trimWhitespace(operand);
            String parts[] = NameChecker.getQNameParts(arg);
            String uri;
            if (parts[0].length() == 0) {
                uri = env.getDefaultElementNamespace();
            } else {
                try {
                    uri = env.getURIForPrefix(parts[0]);
                } catch (XPathException e) {
                    uri = null;
                }
                if (uri == null) {
                    XPathException e = new XPathException("Prefix '" + parts[0] + "' has not been declared");
                    e.setErrorCode("FONS0004");
                    throw e;
                }
            }
            return new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, true);

        } catch (XPathException err) {
            if (err.getErrorCodeQName() == null) {
                err.setErrorCode("FONS0004");
            }
            throw err;
        } catch (QNameException err) {
            XPathException e = new XPathException(err);
            e.setErrorCode("FORG0001");
            throw e;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.