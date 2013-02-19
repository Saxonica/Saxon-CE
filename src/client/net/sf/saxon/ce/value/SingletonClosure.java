package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * A SingletonClosure represents a value that has not yet been evaluated: the value is represented
 * by an expression, together with saved values of all the context variables that the
 * expression depends on. The value of a SingletonClosure is always either a single item
 * or an empty sequence.
 * 
 * <p>The expression may depend on local variables and on the context item; these values
 * are held in the saved XPathContext object that is kept as part of the Closure, and they
 * will always be read from that object. The expression may also depend on global variables;
 * these are unchanging, so they can be read from the Bindery in the normal way. Expressions
 * that depend on other contextual information, for example the values of position(), last(),
 * current(), current-group(), should not be evaluated using this mechanism: they should
 * always be evaluated eagerly. This means that the Closure does not need to keep a copy
 * of these context variables.</p>
 */

public class SingletonClosure extends Closure {

    private boolean built = false;
    private Item value = null;

    /**
     * Constructor should not be called directly, instances should be made using the make() method.
     * @param exp the expression to be lazily evaluated
     * @param context the context in which the expression should be evaluated
     */

    public SingletonClosure(Expression exp, XPathContext context) throws XPathException {
        expression = exp;
        savedXPathContext = context.newContext();
        saveContext(exp, context);
        //System.err.println("Creating SingletonClosure");
    }

    /**
     * Evaluate the expression in a given context to return an iterator over a sequence
     */

    public SequenceIterator iterate() throws XPathException {
        return SingletonIterator.makeIterator(asItem());
    }

    /**
     * Process the expression by writing the value to the current Receiver
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        Item item = asItem();
        if (item != null) {
            out.append(item, NodeInfo.ALL_NAMESPACES);
        }
    }


    /**
     * Return the value in the form of an Item
     *
     * @return the value in the form of an Item
     */

    public Item asItem() throws XPathException {
        if (!built) {
            value = expression.evaluateItem(savedXPathContext);
            built = true;
            savedXPathContext = null;   // release variables saved in the context to the garbage collector
        }
        return value;
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     */

    public Item itemAt(int n) throws XPathException {
        if (n != 0) {
            return null;
        }
        return asItem();
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return (asItem() == null ? 0 : 1);
    }

    /**
     * Return a value containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding value
     */

    public Value materialize() throws XPathException {
        return Value.asValue(asItem());
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
