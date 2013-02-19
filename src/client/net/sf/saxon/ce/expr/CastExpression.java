package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.functions.StringFn;
import client.net.sf.saxon.ce.om.*;
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
    private boolean derived = false;
    private boolean upcast = false;

    static HashMap<Integer, int[]> castingTable = new HashMap(25);

    static void addAllowedCasts(int source, int[] target) {
        castingTable.put(source, target);
    }

    /**
     * The following data represents all the "Y" and "M" entries in section 17.1 of the F+O spec.
     */

    static {
        final int uat = StandardNames.XS_UNTYPED_ATOMIC;
        final int str = StandardNames.XS_STRING;
        final int flt = StandardNames.XS_FLOAT;
        final int dbl = StandardNames.XS_DOUBLE;
        final int dec = StandardNames.XS_DECIMAL;
        final int ing = StandardNames.XS_INTEGER;
        final int dur = StandardNames.XS_DURATION;
        final int ymd = StandardNames.XS_YEAR_MONTH_DURATION;
        final int dtd = StandardNames.XS_DAY_TIME_DURATION;
        final int dtm = StandardNames.XS_DATE_TIME;
        final int tim = StandardNames.XS_TIME;
        final int dat = StandardNames.XS_DATE;
        final int gym = StandardNames.XS_G_YEAR_MONTH;
        final int gyr = StandardNames.XS_G_YEAR;
        final int gmd = StandardNames.XS_G_MONTH_DAY;
        final int gdy = StandardNames.XS_G_DAY;
        final int gmo = StandardNames.XS_G_MONTH;
        final int boo = StandardNames.XS_BOOLEAN;
        final int b64 = StandardNames.XS_BASE64_BINARY;
        final int hxb = StandardNames.XS_HEX_BINARY;
        final int uri = StandardNames.XS_ANY_URI;
        final int qnm = StandardNames.XS_QNAME;
        //final int not = StandardNames.XS_NOTATION;

        final int[] t01 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri};
        addAllowedCasts(uat, t01);
        final int[] t02 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri, qnm};
        addAllowedCasts(str, t02);
        final int[] t03 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(flt, t03);
        addAllowedCasts(dbl, t03);
        addAllowedCasts(dec, t03);
        addAllowedCasts(ing, t03);
        final int[] t04 = {uat, str, dur, ymd, dtd};
        addAllowedCasts(dur, t04);
        addAllowedCasts(ymd, t04);
        addAllowedCasts(dtd, t04);
        final int[] t05 = {uat, str, dtm, tim, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dtm, t05);
        final int[] t06 = {uat, str, tim};
        addAllowedCasts(tim, t06);
        final int[] t07 = {uat, str, dtm, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dat, t07);
        final int[] t08 = {uat, str, gym};
        addAllowedCasts(gym, t08);
        final int[] t09 = {uat, str, gyr};
        addAllowedCasts(gyr, t09);
        final int[] t10 = {uat, str, gmd};
        addAllowedCasts(gmd, t10);
        final int[] t11 = {uat, str, gdy};
        addAllowedCasts(gdy, t11);
        final int[] t12 = {uat, str, gmo};
        addAllowedCasts(gmo, t12);
        final int[] t13 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(boo, t13);
        final int[] t14 = {uat, str, b64, hxb};
        addAllowedCasts(b64, t14);
        addAllowedCasts(hxb, t14);
        final int[] t15 = {uat, str, uri};
        addAllowedCasts(uri, t15);
        final int[] t16 = {uat, str, qnm};
        addAllowedCasts(qnm, t16);
    }

    /**
     * Determine whether casting from a source type to a target type is possible
     * @param source a primitive type (one that has an entry in the casting table)
     * @param target another primitive type
     * @return true if the entry in the casting table is either "Y" (casting always succeeds)
     * or "M" (casting allowed but may fail for some values)
     */

    public static boolean isPossibleCast(int source, int target) {
        if (source == StandardNames.XS_ANY_ATOMIC_TYPE || source == Type.EMPTY) {
            return true;
        }
        if (source == StandardNames.XS_NUMERIC) {
            source = StandardNames.XS_DOUBLE;
        }
        int[] targets = castingTable.get(source);
        if (targets == null) {
            return false;
        }
        for (int i=0; i<targets.length; i++) {
            if (targets[i] == target) {
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
        derived = (targetType.getFingerprint() != targetPrimitiveType.getFingerprint());
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
        int p = sourceType.getPrimitiveType();
        if (!isPossibleCast(p, targetType.getPrimitiveType())) {
            typeError("Casting from " + sourceType + " to " + targetType +
                    " can never succeed", "XPTY0004", null);
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
                if (e.getItemType(th) instanceof AtomicType && e.getCardinality() == StaticProperty.EXACTLY_ONE) {
                    operand = e;
                }
            }
        }
        // avoid converting anything to a string and then back again
        if (operand instanceof StringFn) {
            Expression e = ((StringFn)operand).getArguments()[0];
            ItemType et = e.getItemType(th);
            if (et instanceof AtomicType &&
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
            AtomicValue result = (AtomicValue)value.convert(targetPrimitiveType, false);
            if (derived) {
                result = (AtomicValue)result.convert(targetType, false);
            }
            return result;
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
            CharSequence operand, AtomicType targetType, StaticContext env) throws XPathException {
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