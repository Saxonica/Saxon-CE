package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.ExpressionTool;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.Optimizer;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * This abstract class provides functionality common to the sum(), avg(), count(),
 * exists(), and empty() functions. These all take a sequence as input and produce a singleton
 * as output; and they are all independent of the ordering of the items in the input.
*/

public abstract class Aggregate extends SystemFunction  {

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], true);
        // we don't care about the order of the results, but we do care about how many nodes there are
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
