package client.net.sf.saxon.ce.tree.linked;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
  * CommentImpl is an implementation of a Comment node
  * @author Michael H. Kay
  */


final class CommentImpl extends NodeImpl {

    String comment;

    public CommentImpl(String content) {
        this.comment = content;
    }

    public final String getStringValue() {
        return comment;
    }

    /**
     * Get the typed value of this node.
     * Returns the string value, as an instance of xs:string
     */

    public AtomicValue getTypedValue() {
        return new StringValue(getStringValue());
    }

 
    public final int getNodeKind() {
        return Type.COMMENT;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int copyOptions) throws XPathException {
        out.comment(comment);
    }


}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
