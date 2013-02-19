package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.trans.XPathException;

/**
 * SequenceReceiver: this extension of the Receiver interface is used when processing
 * a sequence constructor. It differs from the Receiver in allowing items (atomic values or
 * nodes) to be added to the sequence, not just tree-building events.
 */

public abstract class SequenceReceiver implements Receiver {

    protected boolean previousAtomic = false;
    protected PipelineConfiguration pipelineConfiguration;
    protected String systemId = null;

    /**
     * Create a SequenceReceiver
     */

    public SequenceReceiver() {
    }

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public void setPipelineConfiguration(PipelineConfiguration pipelineConfiguration) {
        this.pipelineConfiguration = pipelineConfiguration;
    }

    /**
     * Get the Saxon Configuration
     * @return the Configuration
     */

    public Configuration getConfiguration() {
        return pipelineConfiguration.getConfiguration();
    }

    /**
     * Set the system ID
     * @param systemId the URI used to identify the tree being passed across this interface
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system ID
     * @return the system ID that was supplied using the setSystemId() method
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Start the output process
     */

    public void open() throws XPathException {
        previousAtomic = false;
    }

    /**
     * Append an arbitrary item (node or atomic value) to the output
     * @param item           the item to be appended
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
 *                       need to be copied. Values are {@link client.net.sf.saxon.ce.om.NodeInfo#ALL_NAMESPACES},
 *                       {@link client.net.sf.saxon.ce.om.NodeInfo#LOCAL_NAMESPACES}, {@link client.net.sf.saxon.ce.om.NodeInfo#NO_NAMESPACES}
     */

    public abstract void append(Item item, int copyNamespaces) throws XPathException;

    /**
     * Append an item (node or atomic value) to the output
     * @param item the item to be appended
     */

    public void append(Item item) throws XPathException {
        append(item, NodeInfo.ALL_NAMESPACES);
    }

    /**
     * Get the name pool
     * @return the Name Pool that was supplied using the setConfiguration() method
     */

    public NamePool getNamePool() {
        return pipelineConfiguration.getConfiguration().getNamePool();
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.