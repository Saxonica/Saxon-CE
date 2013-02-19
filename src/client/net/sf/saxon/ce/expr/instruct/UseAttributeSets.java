package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;

/**
 * This instruction corresponds to a use-attribute-sets attribute on a literal result element, xsl:element,
 * or xsl:copy.
 */
public class UseAttributeSets extends Instruction {

    private AttributeSet[] attributeSets;

    /**
     * Create a use-attribute-sets expression
     * @param sets the set of attribute sets to be expanded
     */

    public UseAttributeSets(AttributeSet[] sets) {
        attributeSets = sets;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *          if an error is discovered during expression
     *          rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
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
        return this;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    private Expression copy() {
        throw new UnsupportedOperationException("UseAttributeSets.copy()");
    }

    /**
     * Perform type checking of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link client.net.sf.saxon.ce.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.ATTRIBUTE;
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
        int d = 0;
        for (int i=0; i<attributeSets.length; i++) {
            AttributeSet as = attributeSets[i];
            d |= as.getFocusDependencies();
        }
        return d;
    }

    /**
     * ProcessLeavingTail: called to do the real work of this instruction. This method
     * must be implemented in each subclass. The results of the instruction are written
     * to the current Receiver, which can be obtained via the Controller.
     *
     * @param context The dynamic context of the transformation, giving access to the current node,
     *                the current variables, etc.
     * @return null if the instruction has completed execution; or a TailCall indicating
     *         a function call or template call that is delegated to the caller, to be made after the stack has
     *         been unwound so as to save stack space.
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        AttributeSet.expand(attributeSets, context);
        return null;
    }

    /**
     * Test whether this UseAttributeSets expression is equal to another
     * @param obj the other expression
     */

    public boolean equals(Object obj) {
        if (!(obj instanceof UseAttributeSets)) {
            return false;
        }
        if (attributeSets.length != ((UseAttributeSets)obj).attributeSets.length) {
            return false;
        }
        for (int i=0; i<attributeSets.length; i++) {
            AttributeSet as0 = attributeSets[i];
            AttributeSet as1 = ((UseAttributeSets)obj).attributeSets[i];
            if (!as0.getObjectName().equals(as1.getObjectName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compute a hashcode
     */

    public int hashCode() {
        int h = 0x86423719;
        for (int i=0; i<attributeSets.length; i++) {
            h ^= attributeSets[i].getObjectName().hashCode();
        }
        return h;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

