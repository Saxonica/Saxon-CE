package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.sort.AtomicComparer;

/**
 * Interface implemented by expressions that perform a comparison
 */
public interface ComparisonExpression {

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used
     */

    public AtomicComparer getAtomicComparer();

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator();

    /**
     * Get the two operands of the comparison
     */

    public Expression[] getOperands();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.