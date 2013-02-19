package client.net.sf.saxon.ce.dom;

import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;
import com.google.gwt.xml.client.*;


/**
  * DOMWriter is a Receiver that attaches the result tree to a specified Node in the DOM Document
  */

public class DOMWriter implements Receiver {

    private PipelineConfiguration pipe;
    private NamePool namePool;
    private Node currentNode;
    private Document document;
    private Node nextSibling;
    private int level = 0;
    private boolean canNormalize = true;
    private String systemId;

    /**
    * Set the pipelineConfiguration
    */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        namePool = pipe.getConfiguration().getNamePool();
    }

    /**
    * Get the pipeline configuration used for this document
    */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Set the System ID of the destination tree
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system identifier that was set with setSystemId.
     *
     * @return The system identifier that was set with setSystemId,
     *         or null if setSystemId was not called.
     */
    public String getSystemId() {
        return systemId;
    }

    /**
    * Start of the document.
    */

    public void open () {}

    /**
    * End of the document.
    */

    public void close () {}

    /**
     * Start of a document node.
    */

    public void startDocument() throws XPathException {}

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {}

    /**
    * Start of an element.
    */

    public void startElement(int nameCode, int properties) throws XPathException {
        String qname = namePool.getDisplayName(nameCode);
        String prefix = namePool.getPrefix(nameCode);
        String uri = namePool.getURI(nameCode);
        try {
            Element element = document.createElement(qname);
            addNamespace(element, prefix, uri);
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(element, nextSibling);
            } else {
                currentNode.appendChild(element);
            }
            currentNode = element;
        } catch (DOMException err) {
            throw new XPathException(err);
        }
        level++;
    }

    private void addNamespace(Element element, String prefix, String uri) {
        String attName = (prefix.isEmpty() ? "xmlns" : "xmlns:" + prefix);
        element.setAttribute(attName, uri);
    }

    public void namespace (NamespaceBinding nsBinding, int properties) throws XPathException {
        //try {
        	String prefix = nsBinding.getPrefix();
    		String uri = nsBinding.getURI();
    		Element element = (Element)currentNode;
            if (!(uri.equals(NamespaceConstant.XML))) {
                addNamespace(element, prefix, uri);
            }
//        } catch (DOMException err) {
//            throw new XPathException(err);
//        }
    }

    public void attribute(int nameCode, CharSequence value)
    throws XPathException {
        String qname = namePool.getDisplayName(nameCode);
        try {
    		Element element = (Element)currentNode;
            element.setAttribute(qname, value.toString());
            if (qname.indexOf(':') >= 0) {
                String prefix = namePool.getPrefix(nameCode);
                String uri = namePool.getURI(nameCode);
                addNamespace(element, prefix, uri);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }

    public void startContent() throws XPathException {}

    /**
    * End of an element.
    */

    public void endElement () throws XPathException {
		if (canNormalize) {
	        try {
	            currentNode.normalize();
	        } catch (Throwable err) {
	        	canNormalize = false;
	        }      // in case it's a Level 1 DOM
	    }

        currentNode = currentNode.getParentNode();
        level--;
    }


    /**
    * Character data.
    */

    public void characters(CharSequence chars) throws XPathException
    {
        if (level == 0 && nextSibling == null && Whitespace.isWhite(chars)) {
            return; // no action for top-level whitespace
        }
        try {
            Text text = document.createTextNode(chars.toString());
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(text, nextSibling);
            } else {
                currentNode.appendChild(text);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction(String target, CharSequence data)
        throws XPathException
    {
        try {
            ProcessingInstruction pi =
                document.createProcessingInstruction(target, data.toString());
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(pi, nextSibling);
            } else {
                currentNode.appendChild(pi);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }

    /**
    * Handle a comment.
    */

    public void comment(CharSequence chars) throws XPathException
    {
        try {
            Comment comment = document.createComment(chars.toString());
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(comment, nextSibling);
            } else {
                currentNode.appendChild(comment);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }

    /**
     * Set the attachment point for the new subtree
     * @param node the node to which the new subtree will be attached
    */

    public void setNode (Node node) {
        if (node == null) {
            return;
        }
        currentNode = node;
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            document = (Document)node;
        } else {
            document = currentNode.getOwnerDocument();
            if (document == null) {
                // which might be because currentNode() is a parentless ElementOverNodeInfo.
                // we create a DocumentOverNodeInfo, which is immutable, and will cause the DOMWriter to fail
                //document = new DocumentOverNodeInfo();
                // TODO:CLAXON - check this
            }
        }
    }

    /**
     * Set next sibling
     * @param nextSibling the node, which must be a child of the attachment point, before which the new subtree
     * will be created. If this is null the new subtree will be added after any existing children of the
     * attachment point.
     */

    public void setNextSibling(Node nextSibling) {
        this.nextSibling = nextSibling;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
