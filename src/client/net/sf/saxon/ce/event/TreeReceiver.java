package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.type.Type;

/**
 * A TreeReceiver acts as a bridge between a SequenceReceiver, which can receive
 * events for constructing any kind of sequence, and an ordinary Receiver, which
 * only handles events relating to the building of trees. To do this, it has to
 * process any items added to the sequence using the append() interface; all other
 * events are passed through unchanged.
 *
 * <p>If atomic items are appended to the sequence, then adjacent atomic items are
 * turned in to a text node by converting them to strings and adding a single space
 * as a separator.</p>
 *
 * <p>If a document node is appended to the sequence, then the document node is ignored
 * and its children are appended to the sequence.</p>
 *
 * <p>If any other node is appended to the sequence, then it is pushed to the result
 * as a sequence of Receiver events, which may involve walking recursively through the
 * contents of a tree.</p>
 */

public class TreeReceiver extends SequenceReceiver {
    private Receiver nextReceiver;
    private int level = 0;
    private boolean[] isDocumentLevel = new boolean[20];
        // The sequence of events can include startElement/endElement pairs or startDocument/endDocument
        // pairs at any level. A startDocument/endDocument pair is essentially ignored except at the
        // outermost level, except that a namespace or attribute node cannot be sent when we're at a
        // document level. See for example schema90963-err.xsl
    private boolean inStartTag = false;

    /**
     * Create a TreeReceiver
     * @param nextInChain the receiver to which events will be directed, after
     * expanding append events into more primitive tree-based events
     */

    public TreeReceiver(Receiver nextInChain) {
        nextReceiver = nextInChain;
        previousAtomic = false;
        setPipelineConfiguration(nextInChain.getPipelineConfiguration());
    }

    public void setSystemId(String systemId) {
        if (systemId != null && !systemId.equals(this.systemId)) {
            this.systemId = systemId;
            if (nextReceiver != null) {
                nextReceiver.setSystemId(systemId);
            }
        }
    }

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        if (pipelineConfiguration != pipe) {
            pipelineConfiguration = pipe;
            if (nextReceiver != null) {
                nextReceiver.setPipelineConfiguration(pipe);
            }
        }
    }

    /**
     * Get the underlying Receiver (that is, the next one in the pipeline)
     * @return the underlying Receiver
     */

    public Receiver getUnderlyingReceiver() {
        return nextReceiver;
    }

    /**
     * Start of event sequence
     */

    public void open() throws XPathException {
        if (nextReceiver == null) {
            throw new IllegalStateException("TreeReceiver.open(): no underlying receiver provided");
        }
        nextReceiver.open();
        previousAtomic = false;
    }

    /**
     * End of event sequence
     */

    public void close() throws XPathException {
        if (nextReceiver != null) {
            nextReceiver.close();
        }
        previousAtomic = false;
    }

    /**
     * Start of a document node.
    */

    public void startDocument() throws XPathException {
        if (level == 0) {
            nextReceiver.startDocument();
        }
        if (isDocumentLevel.length - 1 < level) {
            boolean[] d2 = new boolean[level*2];
            System.arraycopy(isDocumentLevel, 0, d2, 0, level);
            isDocumentLevel = d2;
        }
        isDocumentLevel[level++] = true;
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        level--;
        if (level == 0) {
            nextReceiver.endDocument();
        }
    }

    /**
     * Notify the start of an element
     * @param qName integer code identifying the name of the element within the name pool.
     * @param properties bit-significant properties of the element node
     */

    public void startElement(StructuredQName qName, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        inStartTag = true;
        nextReceiver.startElement(qName, properties);
        previousAtomic = false;
        if (isDocumentLevel.length - 1 < level) {
            boolean[] d2 = new boolean[level*2];
            System.arraycopy(isDocumentLevel, 0, d2, 0, level);
            isDocumentLevel = d2;
        }
        isDocumentLevel[level++] = false;
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     * @param nsBinding an integer: the top half is a prefix code, the bottom half a URI code.
     * These may be translated into an actual prefix and URI using the name pool. A prefix code of
     * zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     * a URI of "", that is, a namespace undeclaration.
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     * start tag
     */

    public void namespace(NamespaceBinding nsBinding, int properties) throws XPathException {
        boolean documentLevel = level==0 || isDocumentLevel[level-1];
        if (documentLevel || !inStartTag) {
            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.NAMESPACE, nsBinding.getPrefix(),
                    documentLevel);
        }
        nextReceiver.namespace(nsBinding, properties);
        previousAtomic = false;
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode The name of the attribute, as held in the name pool
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     * start tag
     */

    public void attribute(StructuredQName nameCode, CharSequence value)
            throws XPathException {
        boolean documentLevel = level==0 || isDocumentLevel[level-1];
        if (documentLevel || !inStartTag) {
            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.ATTRIBUTE, nameCode.getDisplayName(),
                    documentLevel);
        }
        nextReceiver.attribute(nameCode, value);
        previousAtomic = false;
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        inStartTag = false;
        nextReceiver.startContent();
        previousAtomic = false;
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        if (inStartTag) {
            startContent();
        }
        nextReceiver.endElement();
        previousAtomic = false;
        level--;
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars) throws XPathException {
        if (chars.length() > 0) {
            if (inStartTag) {
                startContent();
            }
            nextReceiver.characters(chars);
        }
        previousAtomic = false;
    }


    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        nextReceiver.processingInstruction(target, data);
        previousAtomic = false;
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        nextReceiver.comment(chars);
        previousAtomic = false;
    }


    /**
     * Append an arbitrary item (node or atomic value) to the output
     */

    public void append(Item item, int copyNamespaces) throws XPathException {
        if (item != null) {
            if (item instanceof AtomicValue) {
                if (previousAtomic) {
                    characters(" ");
                }
                characters(item.getStringValue());
                previousAtomic = true;
            } else if (((NodeInfo)item).getNodeKind() == Type.DOCUMENT) {
                startDocument(); // needed to ensure that illegal namespaces or attributes in the content are caught
                SequenceIterator iter = ((NodeInfo)item).iterateAxis(Axis.CHILD);
                while (true) {
                    Item it = iter.next();
                    if (it == null) break;
                    append(it, copyNamespaces);
                }
                previousAtomic = false;
                endDocument();
            } else {
                int copyOptions = CopyOptions.TYPE_ANNOTATIONS;
                if (copyNamespaces == NodeInfo.LOCAL_NAMESPACES) {
                    copyOptions |= CopyOptions.LOCAL_NAMESPACES;
                } else if (copyNamespaces == NodeInfo.ALL_NAMESPACES) {
                    copyOptions |= CopyOptions.ALL_NAMESPACES;
                }
                ((NodeInfo)item).copy(this, copyOptions);
                previousAtomic = false;
            }
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.