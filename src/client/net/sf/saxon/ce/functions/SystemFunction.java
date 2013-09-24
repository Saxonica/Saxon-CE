package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceType;


/**
* Abstract superclass for system-defined and user-defined functions
*/

public abstract class SystemFunction extends FunctionCall {

    /**
     * Make a system function call (one in the standard function namespace).
     * @param name The local name of the function.
     * @param arguments the arguments to the function call
     * @return a FunctionCall that implements this function, if it
     * exists, or null if the function is unknown.
     */

    public static FunctionCall makeSystemFunction(String name, Expression[] arguments) {
        StandardFunction.Entry entry = StandardFunction.getFunction(name, arguments.length);
        if (entry==null) {
            return null;
        }
        SystemFunction f = entry.skeleton.newInstance();
        f.setDetails(entry);
        f.setFunctionName(new StructuredQName("", NamespaceConstant.FN, name));
        f.setArguments(arguments);
        return f;
    }


    private StandardFunction.Entry details;
    protected int operation;

    /**
     * Set the details of this type of function
     * @param entry information giving details of the function signature
    */

    public void setDetails(StandardFunction.Entry entry) {
        details = entry;
    }

    /**
     * Get the details of the function signature
     * @return information about the function signature
    */

    public StandardFunction.Entry getDetails() {
        return details;
    }

    /**
    * Method called during static type checking
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        checkArgumentCount(details.minArguments, details.maxArguments, visitor);
        for (int i=0; i<argument.length; i++) {
            checkArgument(i, visitor);
        }
    }

    /**
     * Perform static type checking on an argument to a function call, and add
     * type conversion logic where necessary.
     * @param arg argument number, zero-based
     * @param visitor an expression visitor
     * @throws XPathException if type checking fails
    */

    private void checkArgument(int arg, ExpressionVisitor visitor) throws XPathException {
        RoleLocator role = new RoleLocator(RoleLocator.FUNCTION,
                getFunctionName(), arg);
        //role.setSourceLocator(this);
        role.setErrorCode(getErrorCodeForTypeErrors());
        argument[arg] = TypeChecker.staticTypeCheck(
                                argument[arg],
                                getRequiredType(arg),
                                visitor.getStaticContext().isInBackwardsCompatibleMode(),
                                role);
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public abstract SystemFunction newInstance();

    /**
     * Return the error code to be used for type errors. This is overridden for functions
     * such as exactly-one(), one-or-more(), ...
     * @return the error code to be used for type errors in the function call. Normally XPTY0004,
     * but different codes are used for functions such as exactly-one()
     */

    public String getErrorCodeForTypeErrors() {
        return "XPTY0004";
    }

    /**
     * Get the required type of the nth argument
     * @param arg the number of the argument whose type is requested, zero-based
     * @return the required type of the argument as defined in the function signature
    */

    protected SequenceType getRequiredType(int arg) {
        if (details == null) {
            return SequenceType.ANY_SEQUENCE;
        }
        return details.argumentTypes[arg];
        // this is overridden for concat()
    }

    /**
    * Determine the item type of the value returned by the function
     */

    public ItemType getItemType() {
        ItemType type = details.resultType.getPrimaryType();
        if (details.sameItemTypeAsFirstArgument) {
            if (argument.length > 0) {
                return argument[0].getItemType();
            } else {
                return AnyItemType.getInstance();
                // if there is no first argument, an error will be reported
            }
        } else {
            return type;
        }
    }

    /**
    * Determine the cardinality of the function.
    */

    public int computeCardinality() {
        return details.resultType.getCardinality();
    }

    /**
     * Determine the special properties of this expression. The general rule
     * is that a system function call is non-creative if its return type is
     * atomic, or if all its arguments are non-creative. This is overridden
     * for the generate-id() function, which is considered creative if
     * its operand is creative (because the result depends on the
     * identity of the operand)
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (details == null) {
            return p;
        }
        if (details.resultType.getPrimaryType() instanceof AtomicType) {
            return p | StaticProperty.NON_CREATIVE;
        }
        for (Expression arg : argument) {
            if ((arg.getSpecialProperties() & StaticProperty.NON_CREATIVE) == 0) {
                // the argument is creative
                return p;
            }
        }
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Set "." as the default value for the first and only argument. Called from subclasses.
    */

    protected final void useContextItemAsDefault() {
        if (argument.length==0) {
            argument = new Expression[1];
            argument[0] = new ContextItemExpression();
            ExpressionTool.copyLocationInfo(this, argument[0]);
            resetLocalStaticProperties();
        }
        // Note that the extra argument is added before type-checking takes place. The
        // type-checking will add any necessary checks to ensure that the context item
        // is a node, in cases where this is required.
    }

    /**
    * Add an implicit argument referring to the context document. Called by functions such as
    * id() and key() that take the context document as an implicit argument
     * @param pos the position of the argument whose default value is ".", zero-based
     * @param augmentedName the name to be used for the function call with its extra argument.
     * There are some cases where user function calls cannot supply the argument directly (notably
     * unparsed-entity-uri() and unparsed-entity-public-id()) and in these cases a synthesized
     * function name is used for the new function call.
     * @throws XPathException if an error occurs
    */

    protected final void addContextDocumentArgument(int pos, String augmentedName)
    throws XPathException {
        if (argument.length > pos) {
            return;
            // this can happen during optimization, if the extra argument is already present
        }
        if (argument.length != pos) {
            throw new XPathException("Too few arguments in call to " + augmentedName + "() function");
        }
        Expression[] newArgs = new Expression[pos+1];
        System.arraycopy(argument, 0, newArgs, 0, argument.length);
        RootExpression rootExpression = new RootExpression();
        ExpressionTool.copyLocationInfo(this, rootExpression);
        newArgs[pos] = rootExpression;
        argument = newArgs;
        setDetails(StandardFunction.getFunction(augmentedName, newArgs.length));
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
