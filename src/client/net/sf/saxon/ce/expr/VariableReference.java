package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.GlobalParam;
import client.net.sf.saxon.ce.expr.instruct.UserFunctionParameter;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.SequenceType;
import client.net.sf.saxon.ce.value.SingletonItem;
import client.net.sf.saxon.ce.value.Value;

import java.util.logging.Logger;

/**
 * Variable reference: a reference to a variable. This may be an XSLT-defined variable, a range
 * variable defined within the XPath expression, or a variable defined in some other static context.
 */

public class VariableReference extends Expression {

    protected Binding binding = null;     // This will be null until fixup() is called; it will also be null
                                // if the variable reference has been inlined
    protected SequenceType staticType = null;
    protected Value constantValue = null;
    transient String displayName = null;
    private boolean flattened = false;
    private boolean inLoop = true;

    /**
     * Create a Variable Reference
     */

    public VariableReference() {
        //System.err.println("Creating varRef");
    }

    /**
     * Create a Variable Reference
     * @param binding the variable binding to which this variable refers
     */

    public VariableReference(Binding binding) {
        //System.err.println("Creating varRef1");
        displayName = binding.getVariableQName().getDisplayName();
        fixup(binding);
    }

    /**
     * Set static type. This is a callback from the variable declaration object. As well
     * as supplying the static type, it may also supply a compile-time value for the variable.
     * As well as the type information, other static properties of the value are supplied:
     * for example, whether the value is an ordered node-set.
     * @param type the static type of the variable
     * @param value the value of the variable if this is a compile-time constant
     * @param properties static properties of the expression to which the variable is bound
     */

    public void setStaticType(SequenceType type, Value value, int properties) {
        // System.err.println(this + " Set static type = " + type);
        staticType = type;
        constantValue = value;
        // Although the variable may be a context document node-set at the point it is defined,
        // the context at the point of use may be different, so this property cannot be transferred.
        int dependencies = getDependencies();
        staticProperties = (properties & ~StaticProperty.CONTEXT_DOCUMENT_NODESET) |
                StaticProperty.NON_CREATIVE |
                type.getCardinality() |
                dependencies;
    }

    /**
     * Mark an expression as being "flattened". This is a collective term that includes extracting the
     * string value or typed value, or operations such as simple value construction that concatenate text
     * nodes before atomizing. The implication of all of these is that although the expression might
     * return nodes, the identity of the nodes has no significance. This is called during type checking
     * of the parent expression. At present, only variable references take any notice of this notification.
     */

    public void setFlattened(boolean flattened) {
        super.setFlattened(flattened);
        this.flattened = flattened;
    }

    /**
     * Test whether this variable reference is flattened - that is, whether it is atomized etc
     * @return true if the value of the variable is atomized, or converted to a string or number
     */

    public boolean isFlattened() {
        return flattened;
    }

    /**
     * Type-check the expression. At this stage details of the static type must be known.
     * If the variable has a compile-time value, this is substituted for the variable reference
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (constantValue != null) {
            binding = null;
            return Literal.makeLiteral(constantValue);
        }
//        if (staticType == null) {
//            throw new IllegalStateException("Variable $" + getDisplayName() + " has not been fixed up");
//        }
        if (binding instanceof Expression) {
            inLoop = visitor.isLoopingSubexpression((Expression)binding);
// following code removed because it causes error181 to blow the stack - need to check for circularities well            
//            if (binding instanceof GlobalVariable) {
//                ((GlobalVariable)binding).typeCheck(visitor, AnyItemType.getInstance());
//            }
        } else if (binding instanceof UserFunctionParameter) {
            inLoop = visitor.isLoopingSubexpression(null);
        }
        return this;
    }

    /**
     * Type-check the expression. At this stage details of the static type must be known.
     * If the variable has a compile-time value, this is substituted for the variable reference
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (constantValue != null) {
            binding = null;
            return Literal.makeLiteral(constantValue);
        }

        return this;
    }



    /**
     * Fix up this variable reference to a Binding object, which enables the value of the variable
     * to be located at run-time.
     */

    public void fixup(Binding binding) {
        this.binding = binding;
        resetLocalStaticProperties();
    }

    /**
     * Provide additional information about the type of the variable, typically derived by analyzing
     * the initializer of the variable binding
     * @param type the item type of the variable
     * @param cardinality the cardinality of the variable
     * @param constantValue the actual value of the variable, if this is known statically, otherwise null
     * @param properties additional static properties of the variable's initializer
     * @param visitor an ExpressionVisitor
     */

    public void refineVariableType(
            ItemType type, int cardinality, Value constantValue, int properties, ExpressionVisitor visitor) {
        TypeHierarchy th = TypeHierarchy.getInstance();
        ItemType oldItemType = getItemType();
        ItemType newItemType = oldItemType;
        if (th.isSubType(type, oldItemType)) {
            newItemType = type;
        }
        int newcard = cardinality & getCardinality();
        if (newcard==0) {
            // this will probably lead to a type error later
            newcard = getCardinality();
        }
        SequenceType seqType = SequenceType.makeSequenceType(newItemType, newcard);
        setStaticType(seqType, constantValue, properties);
    }

    /**
     * Determine the data type of the expression, if possible
     *
     * @return the type of the variable, if this can be determined statically;
     *         otherwise Type.ITEM (meaning not known in advance)
     */

    public ItemType getItemType() {
        if (staticType == null || staticType.getPrimaryType() == AnyItemType.getInstance()) {
            if (binding != null) {
                SequenceType st = binding.getRequiredType();
                if (st != null) {
                    return st.getPrimaryType();
                }
            }
            return AnyItemType.getInstance();
        } else {
            return staticType.getPrimaryType();
        }
    }

    /**
     * Get the static cardinality
     */

    public int computeCardinality() {
        if (staticType == null) {
            if (binding == null) {
                return StaticProperty.ALLOWS_ZERO_OR_MORE;
            } else if (binding instanceof LetExpression) {
                return binding.getRequiredType().getCardinality();
            } else if (binding instanceof Assignation) {
                return StaticProperty.EXACTLY_ONE;
            } else {
                return binding.getRequiredType().getCardinality();
            }
        } else {
            return staticType.getCardinality();
        }
    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        p |= StaticProperty.NON_CREATIVE;
        if (binding instanceof Assignation) {
            Expression exp = ((Assignation)binding).getSequence();
            if (exp != null) {
                p |= (exp.getSpecialProperties() & StaticProperty.NOT_UNTYPED);
            }
        }
        if (staticType != null &&
                !Cardinality.allowsMany(staticType.getCardinality()) &&
                staticType.getPrimaryType() instanceof NodeTest) {
            p |= StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        return p;
    }

    /**
     * Test if this expression is the same as another expression.
     * (Note, we only compare expressions that
     * have the same static and dynamic context).
     */

    public boolean equals(Object other) {
        return (other instanceof VariableReference &&
                binding == ((VariableReference) other).binding &&
                binding != null);
    }

    /**
     * get HashCode for comparing two expressions
     */

    public int hashCode() {
        return binding == null ? 73619830 : binding.hashCode();
    }


    public int getIntrinsicDependencies() {
        int d = 0;
        if (binding == null) {
            // assume the worst
            d |= (StaticProperty.DEPENDS_ON_LOCAL_VARIABLES |
                    StaticProperty.DEPENDS_ON_RUNTIME_ENVIRONMENT);
        } else if (binding.isGlobal()) {
            if (binding instanceof GlobalParam) {
                d |= StaticProperty.DEPENDS_ON_RUNTIME_ENVIRONMENT;
            }
        } else {
            d |= StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
        }
        return d;
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both all three methods
     * natively.
     */

    public int getImplementationMethod() {
        return (Cardinality.allowsMany(getCardinality()) ? 0 : EVALUATE_METHOD)
                | ITERATE_METHOD | PROCESS_METHOD;
    }


    /**
     * Get the value of this variable in a given context.
     *
     * @param c the XPathContext which contains the relevant variable bindings
     * @return the value of the variable, if it is defined
     * @throws XPathException if the variable is undefined
     */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        try {
            Sequence actual = evaluateVariable(c);
            return Value.asIterator(actual);
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        } catch (AssertionError err) {
            //err.printStackTrace();
            String msg = err.getMessage() + ". Variable reference $" + getDisplayName() +
                     (getSystemId() == null ? "" : " of " + getSystemId());
            // log here in case this is not handled properly
            Logger logger = Logger.getLogger("VariableReference");
            logger.severe("internal null reference error: " + msg);
            throw new XPathException(msg);
        }
    }

    public Item evaluateItem(XPathContext c) throws XPathException {
        try {
            Sequence actual = evaluateVariable(c);
            if (actual instanceof Item) {
                return (Item) actual;
            }
            return Value.asItem(actual);
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
    }

    public void process(XPathContext c) throws XPathException {
        try {
            Sequence actual = evaluateVariable(c);
            if (actual instanceof NodeInfo) {
                actual = new SingletonItem((NodeInfo) actual);
            }
            Value.process(Value.asIterator(actual), c);
        } catch (XPathException err) {
            err.maybeSetLocation(getSourceLocator());
            throw err;
        }
    }

    /**
     * Evaluate this variable
     * @param c the XPath dynamic context
     * @return the value of the variable
     * @throws XPathException if any error occurs
     */

    public Sequence evaluateVariable(XPathContext c) throws XPathException {
        try {
            return binding.evaluateVariable(c);
        } catch (NullPointerException err) {
            if (binding == null) {
                throw new IllegalStateException("Variable $" + displayName + " has not been fixed up");
            } else {
                throw err;
            }
        }
    }

    /**
     * Get the object bound to the variable
     * @return the Binding which declares this variable and associates it with a value
     */

    public Binding getBinding() {
        return binding;
    }

    /**
     * Get the display name of the variable. This is taken from the variable binding if possible
     * @return the display name (a lexical QName
     */

    public String getDisplayName() {
        if (binding != null) {
            return binding.getVariableQName().getDisplayName();
        } else {
            return displayName;
        }
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        String d = getDisplayName();
        return "$" + (d == null ? "$" : d);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
