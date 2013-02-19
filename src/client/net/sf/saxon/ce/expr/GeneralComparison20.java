package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.trans.XPathException;

/**
 *  The class GeneralComparison20 specializes GeneralComparison for the case where
 *  the comparison is done with 2.0 semantics (i.e. with backwards compatibility off).
 *  It differs from the superclass in that it will never turn the expression into
 *  a GeneralComparison10, which could lead to non-terminating optimizations
 */
public class GeneralComparison20 extends GeneralComparison {

    /**
     * Create a relational expression identifying the two operands and the operator
     *
     * @param p0 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p1 the right-hand operand
     */
    public GeneralComparison20(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
    }

    @Override
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    protected GeneralComparison getInverseComparison() {
        return new GeneralComparison20(operand1, Token.inverse(operator), operand0);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


