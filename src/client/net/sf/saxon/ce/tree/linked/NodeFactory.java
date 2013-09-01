package client.net.sf.saxon.ce.tree.linked;

import client.net.sf.saxon.ce.event.PipelineConfiguration;
import client.net.sf.saxon.ce.om.AttributeCollection;
import client.net.sf.saxon.ce.om.NamespaceBinding;
import client.net.sf.saxon.ce.om.NodeInfo;
import client.net.sf.saxon.ce.om.StructuredQName;


/**
  * Interface NodeFactory. <br>
  * A Factory for nodes used to build a tree. <br>
  * Currently only allows Element nodes to be user-constructed.
  * @author Michael H. Kay
  */

public interface NodeFactory {

    /**
    * Create an Element node
     * @param parent The parent element
      * @param nameCode The element name
     * @param attlist The attribute collection, excluding any namespace attributes
     * @param namespaces List of new namespace declarations for this element, as a sequence
* of namespace codes representing pairs of strings: (prefix1, uri1), (prefix2, uri2)...
     * @param namespacesUsed the number of elemnts of the namespaces array actually used
     * @param pipe The pipeline configuration (provides access to the error listener and the
* location provider)
     * @param baseURI Indicates the source document base URI
     * @param sequenceNumber Sequence number to be assigned to represent document order.
     */

    public ElementImpl makeElementNode(
            NodeInfo parent,
            StructuredQName nameCode,
            AttributeCollection attlist,
            NamespaceBinding[] namespaces,
            int namespacesUsed,
            PipelineConfiguration pipe,
            String baseURI,
            int sequenceNumber);

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
