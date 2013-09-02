package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
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

        CharSequence first = it.getStringValue();

        it = iter.next();
        if (it == null) {
            return StringValue.makeStringValue(first);
        }

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        sb.append(first);

        // Type checking ensures that the separator is not an empty sequence
        if (argument.length == 1) {
            sb.append(it.getStringValue());
            while (true) {
                it = iter.next();
                if (it == null) {
                    return StringValue.makeStringValue(sb.condense());
                }
                sb.append(it.getStringValue());
            }

        } else {
            CharSequence sep = argument[1].evaluateItem(c).getStringValue();
            sb.append(sep);
            sb.append(it.getStringValue());

            while (true) {
                it = iter.next();
                if (it == null) {
                    return StringValue.makeStringValue(sb.condense());
                }
                sb.append(sep);
                sb.append(it.getStringValue());
            }
        }
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.