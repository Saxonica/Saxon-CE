package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.event.ComplexContentOutputter;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.Cardinality;
import client.net.sf.saxon.ce.value.StringValue;

/**
 * xf:string-join(string* $sequence, string $separator)
 */

public class StringJoin extends SystemFunction {

    public StringJoin newInstance() {
        return new StringJoin();
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp instanceof StringJoin) {
            return ((StringJoin) exp).simplifySingleton();
        } else {
            return exp;
        }
    }

    private Expression simplifySingleton() {
        int card = argument[0].getCardinality();
        if (!Cardinality.allowsMany(card)) {
            if (Cardinality.allowsZero(card)) {
                return SystemFunction.makeSystemFunction("string", new Expression[]{argument[0]});
            } else {
                return argument[0];
            }
        }
        return this;
    }

    public Item evaluateItem(XPathContext c) throws XPathException {

        // This rather tortuous code is designed to ensure that we don't evaluate the
        // separator argument unless there are at least two items in the sequence.

        SequenceIterator iter = argument[0].iterate(c);
        Item it = iter.next();
        if (it == null) {
            return StringValue.EMPTY_STRING;
        }

        CharSequence first = it.getStringValueCS();

        it = iter.next();
        if (it == null) {
            return StringValue.makeStringValue(first);
        }

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        sb.append(first);

        // Type checking ensures that the separator is not an empty sequence
        if (argument.length == 1) {
            sb.append(it.getStringValueCS());
            while (true) {
                it = iter.next();
                if (it == null) {
                    return StringValue.makeStringValue(sb.condense());
                }
                sb.append(it.getStringValueCS());
            }

        } else {
            CharSequence sep = argument[1].evaluateItem(c).getStringValueCS();
            sb.append(sep);
            sb.append(it.getStringValueCS());

            while (true) {
                it = iter.next();
                if (it == null) {
                    return StringValue.makeStringValue(sb.condense());
                }
                sb.append(sep);
                sb.append(it.getStringValueCS());
            }
        }
    }

    /**
     * Process the instruction in push mode. This avoids constructing the concatenated string
     * in memory, instead its parts can be sent straight to the serializer.
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        // This rather tortuous code is designed to ensure that we don't evaluate the
        // separator argument unless there are at least two items in the sequence.

        SequenceReceiver out = context.getReceiver();
        if (out instanceof ComplexContentOutputter) {
            // Optimization is only safe if evaluated as part of a complex content constructor
            // Start and end with an empty string to force space separation from any previous or following outputs
            out.append(StringValue.EMPTY_STRING, 0);

            SequenceIterator iter = argument[0].iterate(context);
            Item it = iter.next();
            if (it == null) {
                return;
            }

            CharSequence first = it.getStringValueCS();
            out.characters(first);

            it = iter.next();
            if (it == null) {
                out.append(StringValue.EMPTY_STRING, 0);
                return;
            }

            // Type checking ensures that the separator is not an empty sequence
            if (argument.length == 1) {
                out.characters(it.getStringValueCS());

                while (true) {
                    it = iter.next();
                    if (it == null) {
                        break;
                    }
                    out.characters(it.getStringValueCS());
                }
            } else {
                CharSequence sep = argument[1].evaluateItem(context).getStringValueCS();
                out.characters(sep);
                out.characters(it.getStringValueCS());

                while (true) {
                    it = iter.next();
                    if (it == null) {
                        break;
                    }
                    out.characters(sep);
                    out.characters(it.getStringValueCS());
                }

            }


            out.append(StringValue.EMPTY_STRING, 0);
        } else {
            out.append(evaluateItem(context), 0);
        }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.