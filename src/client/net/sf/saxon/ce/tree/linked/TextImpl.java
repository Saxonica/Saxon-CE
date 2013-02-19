package client.net.sf.saxon.ce.tree.linked;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;

/**
  * A node in the XML parse tree representing character content<P>
  * @author Michael H. Kay
  */

final class TextImpl extends NodeImpl {

    private String content;

    public TextImpl(String content) {
    	this.content = content;
    }

    /**
     * Append to the content of the text node
     * @param content the new content to be appended
     */

    public void appendStringValue(String content) {
        this.content = this.content + content;
    }

    /**
    * Return the character value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
		return content;
    }

    /**
    * Return the type of node.
    * @return Type.TEXT
    */

    public final int getNodeKind() {
        return Type.TEXT;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int copyOptions) throws XPathException {
        out.characters(content);
    }



}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.