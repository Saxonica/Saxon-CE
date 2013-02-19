package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;

/**
  * The CommentStripper class is a filter that removes all comments and processing instructions.
  * It also concatenates text nodes that are split by comments and PIs. This follows the rules for
  * processing stylesheets.
  * @author Michael H. Kay
  */


public class CommentStripper extends ProxyReceiver {

    private FastStringBuffer buffer = new FastStringBuffer(FastStringBuffer.MEDIUM);

    /**
    * Default constructor for use in subclasses
    */

    public CommentStripper() {}

    public void startElement(int nameCode, int properties)
    throws XPathException {
        flush();
        nextReceiver.startElement(nameCode, properties);
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endElement () throws XPathException {
        flush();
        nextReceiver.endElement();
    }

    /**
     * Handle a text node. Because we're often handling stylesheets on this path, whitespace text
     * nodes will often be stripped but we can't strip them immediately because of the case
     * [element]   [!-- comment --]text[/element], where the space before the comment is considered
     * significant. But it's worth going to some effort to avoid uncompressing the whitespace in the
     * more common case, so that it can easily be detected and stripped downstream.
    */

    public void characters(CharSequence chars) throws XPathException {
        buffer.append(chars);
    }

    /**
    * Remove comments
    */

    public void comment(CharSequence chars) {}

    /**
    * Remove processing instructions
    */

    public void processingInstruction(String name, CharSequence data) {}

    /**
    * Flush the character buffer
    */

    private void flush() throws XPathException {
        if (buffer.length() > 0) {
            nextReceiver.characters(buffer);
        } 
        buffer.setLength(0);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
