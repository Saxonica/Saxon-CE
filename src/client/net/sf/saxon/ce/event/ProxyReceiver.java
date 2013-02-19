package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * A ProxyReceiver is an Receiver that filters data before passing it to another
 * underlying Receiver.
 */

public abstract class ProxyReceiver extends SequenceReceiver {
    protected Receiver nextReceiver;

    public void setSystemId(String systemId) {
        //noinspection StringEquality
        if (systemId != this.systemId) {
            // use of == rather than equals() is deliberate, since this is only an optimization
            this.systemId = systemId;
            if (nextReceiver != null) {
                nextReceiver.setSystemId(systemId);
            }
        }
    }

     /**
      * Set the underlying receiver. This call is mandatory before using the Receiver.
      * @param receiver the underlying receiver, the one that is to receive events after processing
      * by this filter.
     */

    public void setUnderlyingReceiver(Receiver receiver) {
        if (receiver != nextReceiver) {
            nextReceiver = receiver;
            if (pipelineConfiguration != null && receiver != null) {
                nextReceiver.setPipelineConfiguration(pipelineConfiguration);
            }
        }
    }

    /**
     * Get the underlying Receiver (that is, the next one in the pipeline)
     */

    public Receiver getUnderlyingReceiver() {
        return nextReceiver;
    }


    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        if (pipelineConfiguration != pipe) {
            pipelineConfiguration = pipe;
            if (nextReceiver != null) {
                nextReceiver.setPipelineConfiguration(pipe);
            }
        }
    }

    public Configuration getConfiguration() {
        return pipelineConfiguration.getConfiguration();
    }

    /**
     * Get the namepool for this configuration
     */

    public NamePool getNamePool() {
        return pipelineConfiguration.getConfiguration().getNamePool();
    }

    /**
     * Start of event stream
     */

    public void open() throws XPathException {
        if (nextReceiver == null) {
            throw new IllegalStateException("ProxyReceiver.open(): no underlying receiver provided");
        }
        nextReceiver.open();
    }

    /**
     * End of output. Note that closing this receiver also closes the rest of the
     * pipeline.
     */

    public void close() throws XPathException {
        // Note: It's wrong to assume that because we've finished writing to this
        // receiver, then we've also finished writing to other receivers in the pipe.
        // In the case where the rest of the pipe is to stay open, the caller should
        // either avoid doing the close(), or should first set the underlying receiver
        // to null.
        nextReceiver.close();
    }

    /**
     * Start of a document node.
     */

    public void startDocument() throws XPathException {
        nextReceiver.startDocument();
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        nextReceiver.endDocument();
    }

    /**
     * Notify the start of an element
     *
     * @param nameCode   integer code identifying the name of the element within the name pool.
     * @param properties properties of the element node
     */

    public void startElement(int nameCode, int properties) throws XPathException {
        nextReceiver.startElement(nameCode, properties);
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
        nextReceiver.namespace(nsBinding, properties);
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, CharSequence value)
            throws XPathException {
        nextReceiver.attribute(nameCode, value);
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        nextReceiver.startContent();
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        nextReceiver.endElement();
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars) throws XPathException {
        nextReceiver.characters(chars);
    }


    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data) throws XPathException {
        nextReceiver.processingInstruction(target, data);
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars) throws XPathException {
        nextReceiver.comment(chars);
    }

    /**
     * Append an arbitrary item (node or atomic value) to the output
     *
     * @param item           the item to be appended
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
*                       need to be copied. Values are {@link client.net.sf.saxon.ce.om.NodeInfo#ALL_NAMESPACES},
*                       {@link client.net.sf.saxon.ce.om.NodeInfo#LOCAL_NAMESPACES}, {@link client.net.sf.saxon.ce.om.NodeInfo#NO_NAMESPACES}
     */

    public void append(Item item, int copyNamespaces) throws XPathException {
        if (nextReceiver instanceof SequenceReceiver) {
            ((SequenceReceiver)nextReceiver).append(item, copyNamespaces);
        } else {
            throw new UnsupportedOperationException("append() method is not supported in this class");
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.