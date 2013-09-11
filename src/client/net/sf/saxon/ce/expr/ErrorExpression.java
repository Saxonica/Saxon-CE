package client.net.sf.saxon.ce.expr;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;


/**
* Error expression: this expression is generated when the supplied expression cannot be
* parsed, and the containing element enables forwards-compatible processing. It defers
* the generation of an error message until an attempt is made to evaluate the expression
*/

public class ErrorExpression extends Expression {

    private XPathException exception;     // the error found when parsing this expression

    /**
    * Constructor
    * @param exception the error found when parsing this expression
    */

    public ErrorExpression(XPathException exception) {
        this.exception = exception;
        exception.setLocator(getSourceLocator()); // to remove any links to the compile-time stylesheet objects
    }

    /**
     * Get the wrapped exception
     */

    public XPathException getException() {
        return exception;
    }

    /**
    * Type-check the expression.
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
    * Evaluate the expression. This always throws the exception registered when the expression
    * was first parsed.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        // copy the exception for thread-safety, because we want to add context information
        XPathException err = new XPathException(exception.getMessage());
        err.setLocator(getSourceLocator());
        err.setErrorCodeQName(exception.getErrorCodeQName());
        throw err;
    }

    /**
    * Iterate over the expression. This always throws the exception registered when the expression
    * was first parsed.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        evaluateItem(context);
        return null;    // to fool the compiler
    }

    /**
    * Determine the data type of the expression, if possible
    * @return Type.ITEM (meaning not known in advance)
     */

    public ItemType getItemType() {
        return AnyItemType.getInstance();
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
        // we return a liberal value, so that we never get a type error reported
        // statically
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    private Expression copy() {
        return new ErrorExpression(exception);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
