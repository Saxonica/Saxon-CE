package client.net.sf.saxon.ce.expr.instruct;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * Handler for local xsl:variable elements in stylesheet. Not used in XQuery. In fact, the class is used
 * only transiently in XSLT: local variables are compiled first to a LocalVariable object, and subsequently
 * to a LetExpression.
*/

public class LocalVariable extends GeneralVariable {

    /**
    * Process the local variable declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        throw new UnsupportedOperationException("LocalVariable");
//        context.setLocalVariable(getSlotNumber(),
//                ExpressionTool.evaluate(getSelectExpression(), evaluationMode, context, 10));
//        return null;
    }

   /**
    * Evaluate the variable
    */

   public Sequence evaluateVariable(XPathContext c) throws XPathException {
       throw new UnsupportedOperationException("LocalVariable");
//       return c.evaluateLocalVariable(getSlotNumber());
   }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
