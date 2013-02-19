package client.net.sf.saxon.ce.event;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;

/**
  * TeeOutputter: a SequenceReceiver that duplicates received events to two different destinations
  */

public class TeeOutputter extends SequenceReceiver {

    SequenceReceiver seq1;
    SequenceReceiver seq2;

    public TeeOutputter(Receiver seq1, Receiver seq2) {
        if (seq1 instanceof SequenceReceiver) {
            this.seq1 = (SequenceReceiver)seq1;
        } else {
            this.seq1 = new TreeReceiver(seq1);
        }
        if (seq2 instanceof SequenceReceiver) {
            this.seq2 = (SequenceReceiver)seq2;
        } else {
            this.seq2 = new TreeReceiver(seq2);
        }
    }

    /**
     * Output an item (atomic value or node) to the sequence
     */

    public void append(Item item, int copyNamespaces) throws XPathException {
        seq1.append(item, NodeInfo.ALL_NAMESPACES);
        seq2.append(item, NodeInfo.ALL_NAMESPACES);
    }

    /**
     * Notify the start of a document node
     */

    public void startDocument() throws XPathException {
        seq1.startDocument();
        seq2.startDocument();
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        seq1.endDocument();
        seq2.endDocument();
    }

    /**
     * Notify the start of an element
     *
     * @param nameCode    integer code identifying the name of the element within the name pool.
     * @param properties  bit-significant properties of the element node. If there are no revelant
     */

    public void startElement(int nameCode, int properties) throws XPathException {
        seq1.startElement(nameCode, properties);
        seq2.startElement(nameCode, properties);
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param nsBinding an integer: the top half is a prefix code, the bottom half a URI code.
     *                      These may be translated into an actual prefix and URI using the name pool. A prefix code of
     *                      zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     *                      a URI of "", that is, a namespace undeclaration.
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */

    public void namespace(NamespaceBinding nsBinding, int properties) throws XPathException {
        seq1.namespace(nsBinding, properties);
        seq2.namespace(nsBinding, properties);
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, CharSequence value) throws XPathException {
        seq1.attribute(nameCode, value);
        seq2.attribute(nameCode, value);
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */

    public void startContent() throws XPathException {
        seq1.startContent();
        seq2.startContent();
    }

    /**
     * Notify the end of an element. The receiver must maintain a stack if it needs to know which
     * element is ending.
     */

    public void endElement() throws XPathException {
        seq1.endElement();
        seq2.endElement();
    }

    /**
     * Notify character data. Note that some receivers may require the character data to be
     * sent in a single event, but in general this is not a requirement.
     *
     * @param chars      The characters
     */

    public void characters(CharSequence chars) throws XPathException {
        seq1.characters(chars);
        seq2.characters(chars);
    }

    /**
     * Output a processing instruction
     *
     * @param name       The PI name. This must be a legal name (it will not be checked).
     * @param data       The data portion of the processing instruction
     * @throws IllegalArgumentException: the content is invalid for an XML processing instruction
     */

    public void processingInstruction(String name, CharSequence data) throws XPathException {
        seq1.processingInstruction(name, data);
        seq2.processingInstruction(name, data);
    }

    /**
     * Notify a comment. Comments are only notified if they are outside the DTD.
     *
     * @param content    The content of the comment
     * @throws IllegalArgumentException: the content is invalid for an XML comment
     */

    public void comment(CharSequence content) throws XPathException {
        seq1.comment(content);
        seq2.comment(content);
    }

    /**
     * Notify the end of the event stream
     */

    public void close() throws XPathException {
        seq1.close();
        seq2.close();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.