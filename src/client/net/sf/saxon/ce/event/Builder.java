package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * The abstract Builder class is responsible for taking a stream of SAX events
 * and constructing a Document tree. There is one concrete subclass for each
 * tree implementation.
 * @author Michael H. Kay
 */

public abstract class Builder implements Receiver {
    /**
     * Constant denoting a request for the default tree model
     */
    public static final int UNSPECIFIED_TREE_MODEL = -1;
    /**
     * Constant denoting the "linked tree" in which each node is represented as an object
     */
    public static final int LINKED_TREE = 0;

    /**
     * Constant denoting the "tiny tree" in which the tree is represented internally using arrays of integers
     */
    public static final int TINY_TREE = 1;
    /**
     * Constant denoting the "tiny tree condensed", a variant of the tiny tree in which text and attribute nodes
     * sharing the same string value use shared storage for the value.
     */
    public static final int TINY_TREE_CONDENSED = 2;

    protected PipelineConfiguration pipe;
    protected Configuration config;
    protected NamePool namePool;
    protected String systemId;
    protected String baseURI;
    protected NodeInfo currentRoot;

    protected boolean started = false;
    protected boolean open = false;

    /**
     * Create a Builder and initialise variables
     */

    public Builder() {
    }

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        //System.err.println("Builder#setPipelineConfiguration pipe = " + pipe);
//        if (pipe == null) {
//            new NullPointerException("pipe not initialized").printStackTrace();
//        }
        this.pipe = pipe;
        config = pipe.getConfiguration();
        namePool = config.getNamePool();
    }

    public PipelineConfiguration getPipelineConfiguration () {
        return pipe;
    }

    /**
     * Get the Configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

   /**
     * The SystemId is equivalent to the document-uri property defined in the XDM data model.
     * It should be set only in the case of a document that is potentially retrievable via this URI.
     * This means it should not be set in the case of a temporary tree constructed in the course of
     * executing a query or transformation.
     * @param systemId the SystemId, that is, the document-uri.
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * The SystemId is equivalent to the document-uri property defined in the XDM data model.
     * It should be set only in the case of a document that is potentially retrievable via this URI.
     * This means the value will be null in the case of a temporary tree constructed in the course of
     * executing a query or transformation.
     * @return the SystemId, that is, the document-uri.
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Set the base URI of the document node of the tree being constructed by this builder
     * @param baseURI the base URI
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     * Get the base URI of the document node of the tree being constructed by this builder
     * @return the base URI
     */

    public String getBaseURI() {
        return baseURI;
    }



    /////////////////////////////////////////////////////////////////////////
    // Methods setting and getting options for building the tree
    /////////////////////////////////////////////////////////////////////////


    public void open() {
        open = true;
    }

    public void close() throws XPathException {
        open = false;
    }

    /**
     * Get the current root node. This will normally be a document node, but if the root of the tree
     * is an element node, it can be an element.
     * @return the root of the tree that is currently being built, or that has been most recently built
     * using this builder
     */

    public NodeInfo getCurrentRoot() {
        return currentRoot;
    }

    /**
     * Reset the builder to its initial state. The most important effect of calling this
     * method (implemented in subclasses) is to release any links to the constructed document
     * tree, allowing the memory occupied by the tree to released by the garbage collector even
     * if the Builder is still in memory. This can happen because the Builder is referenced from a
     * parser in the Configuration's parser pool.
     */

    public void reset() {
        pipe = null;
        config = null;
        namePool = null;
        systemId = null;
        baseURI = null;
        currentRoot = null;
        started = false;
        open = false;
    }

   
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
