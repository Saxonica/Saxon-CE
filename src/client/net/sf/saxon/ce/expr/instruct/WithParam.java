package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;

import java.util.List;
import java.util.Arrays;

/**
 * An instruction derived from a xsl:with-param element in the stylesheet. <br>
 */

public class WithParam extends GeneralVariable {

    int parameterId;
    boolean typeChecked = false;

    public WithParam() {
    }

    /**
     * Allocate a number which is essentially an alias for the parameter name,
     * unique within a stylesheet
     *
     * @param id the parameter id
     */

    public void setParameterId(int id) {
        parameterId = id;
    }

    /**
     * Say whether this parameter will have been typechecked by the caller to ensure it satisfies
     * the required type, in which case the callee need not do a dynamic type check
     *
     * @param checked true if the caller has done static type checking against the required type
     */

    public void setTypeChecked(boolean checked) {
        typeChecked = checked;
    }

    /**
     * Get the parameter id, which is essentially an alias for the parameter name,
     * unique within a stylesheet
     *
     * @return the parameter id
     */

    public int getParameterId() {
        return parameterId;
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        // not used
        return null;
    }

    public static void simplify(WithParam[] params, ExpressionVisitor visitor) throws XPathException {
        for (int i = 0; i < params.length; i++) {
            Expression select = params[i].getSelectExpression();
            if (select != null) {
                params[i].setSelectExpression(visitor.simplify(select));
            }
        }
    }


    public static void typeCheck(WithParam[] params, ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int i = 0; i < params.length; i++) {
            Expression select = params[i].getSelectExpression();
            if (select != null) {
                params[i].setSelectExpression(visitor.typeCheck(select, contextItemType));
            }
        }
    }

    public static void optimize(ExpressionVisitor visitor, WithParam[] params, ItemType contextItemType) throws XPathException {
        for (int i = 0; i < params.length; i++) {
            visitor.optimize(params[i], contextItemType);
        }
    }

    /**
     * Promote the expressions in a set of with-param elements. This is a convenience
     * method for use by containing instructions.
     */

    public static void promoteParams(Expression parent, WithParam[] params, PromotionOffer offer) throws XPathException {
        for (int i = 0; i < params.length; i++) {
            Expression select = params[i].getSelectExpression();
            if (select != null) {
                params[i].setSelectExpression(select.promote(offer, parent));
            }
        }
    }

    /**
     * Get the XPath expressions used in an array of WithParam parameters (add them to the supplied list)
     */

    public static void getXPathExpressions(WithParam[] params, List list) {
        if (params != null) {
            list.addAll(Arrays.asList(params));
        }
    }

    /**
     * Evaluate the variable (method exists only to satisfy the interface)
     */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        throw new UnsupportedOperationException();
    }

    /**
     * Ask whether static type checking has been done
     *
     * @return true if the caller has done static type checking against the type required by the callee
     */

    public boolean isTypeChecked() {
        return typeChecked;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
