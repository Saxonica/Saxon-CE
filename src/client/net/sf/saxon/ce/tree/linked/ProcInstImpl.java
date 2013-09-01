package client.net.sf.saxon.ce.tree.linked;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;

/**
  * ProcInstImpl is an implementation of ProcInstInfo used by the Propagator to construct
  * its trees.
  * @author Michael H. Kay
  */


class ProcInstImpl extends NodeImpl {

    String content;
    String localName;

    public ProcInstImpl(String localName, String content) {
        this.localName = localName;
        this.content = content;
    }


    public String getStringValue() {
        return content;
    }
    /**
     * Get the typed value of this node.
     * Returns the string value, as an instance of xs:string
     */

    public AtomicValue getTypedValue() {
        return new StringValue(getStringValue());
    }

    
    public final int getNodeKind() {
        return Type.PROCESSING_INSTRUCTION;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int copyOptions) throws XPathException {
        out.processingInstruction(getLocalPart(), content);
    }

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
