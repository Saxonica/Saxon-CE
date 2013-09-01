package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.ArrayList;
import java.util.List;

/**
 * StartTagBuffer is a ProxyReceiver that buffers attributes and namespace events within a start tag.
 * It maintains details of the namespace context, and a full set of attribute information, on behalf
 * of other filters that need access to namespace information or need to process attributes in arbitrary
 * order.
 */

public class StartTagBuffer extends ProxyReceiver {

    // Details of the pending element event

    protected StructuredQName elementNameCode;
    protected int elementProperties;

    // Details of pending attribute events

    protected AttributeCollection bufferedAttributes;
    protected List<NamespaceBinding> bufferedNamespaces = new ArrayList<NamespaceBinding>();
    int attCount = 0;

    /**
     * Set the pipeline configuration
     * @param pipe the pipeline configuration
     */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        super.setPipelineConfiguration(pipe);
        bufferedAttributes = new AttributeCollection(pipe.getConfiguration());
    }

    /**
    * startElement
    */

    public void startElement(StructuredQName nameCode, int properties) throws XPathException {

        elementNameCode = nameCode;
        elementProperties = properties;

        bufferedAttributes.clear();
        bufferedNamespaces.clear();
        attCount = 0;
    }

    @Override
    public void namespace(NamespaceBinding nsBinding, int properties) throws XPathException {
        bufferedNamespaces.add(nsBinding);
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(StructuredQName nameCode, CharSequence value)
            throws XPathException {

        bufferedAttributes.addAttribute(nameCode, value.toString());

        // Note: we're relying on the fact that AttributeCollection can hold two attributes of the same name
        // and maintain their order, because the check for duplicate attributes is not done until later in the
        // pipeline. We validate both the attributes (see Bugzilla #4600 which legitimizes this.)

    }

    /**
     * startContent: Add any namespace undeclarations needed to stop
     * namespaces being inherited from parent elements
     */

    public void startContent() throws XPathException {
        nextReceiver.startElement(elementNameCode, elementProperties);

        final int length = bufferedAttributes.getLength();
        for (int i=0; i<length; i++) {
            nextReceiver.attribute(bufferedAttributes.getStructuredQName(i),
                    bufferedAttributes.getValue(i)
            );
        }
        for (NamespaceBinding nb : bufferedNamespaces) {
            nextReceiver.namespace(nb, 0);
        }

        nextReceiver.startContent();
    }

    /**
     * Get the value of the current attribute with a given nameCode
     * @param uri the namespace of the required attribute
     * @param local the local name of the required attribute
     * @return the attribute value, or null if the attribute is not present
     */

    public String getAttribute(String uri, String local) {
        return bufferedAttributes.getValue(uri, local);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


