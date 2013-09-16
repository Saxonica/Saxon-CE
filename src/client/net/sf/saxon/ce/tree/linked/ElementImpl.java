package client.net.sf.saxon.ce.tree.linked;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.UnfailingIterator;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.NamespaceIterator;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;

/**
  * ElementImpl implements an element with no attributes or namespace declarations.<P>
  * This class is an implementation of NodeInfo.
  * @author Michael H. Kay
  */


public class ElementImpl extends ParentNodeImpl {

    private StructuredQName elementName;
    private AttributeCollection attributeList;      // this excludes namespace attributes
    private NamespaceBinding[] namespaceList = null;             // list of namespace codes

    /**
    * Construct an empty ElementImpl
    */

    public ElementImpl() {}

    /**
     * Set the name code. Used when creating a dummy element in the Stripper
     * @param name the new element name
    */

    public void setNodeName(StructuredQName name) {
    	this.elementName = name;
    }

    /**
     * Set the attribute list
     * @param atts the list of attributes of this element (not including namespace attributes)
     */

    public void setAttributeList(AttributeCollection atts) {
        this.attributeList = atts;
    }

    /**
     * Set the namespace list
     * @param namespaces an integer array of namespace codes
     */

    public void setNamespaceList(NamespaceBinding[] namespaces) {
        this.namespaceList = namespaces;
    }

    /**
     * Initialise a new ElementImpl with an element name
     * @param qName the element name, with namespaces resolved
     * @param atts The attribute list: always null
     * @param parent  The parent node
     * @param sequenceNumber  Integer identifying this element within the document
     */

    public void initialise(StructuredQName qName, AttributeCollection atts, NodeInfo parent,
                           int sequenceNumber) {
        setNodeName(qName);
        setRawParent((ParentNodeImpl)parent);
        setRawSequenceNumber(sequenceNumber);
        attributeList = atts;
    }

    /**
     * Set location information for this node
     * @param systemId the base URI
     */

    public void setLocation(String systemId) {
        DocumentImpl root = getRawParent().getPhysicalRoot();
        root.setSystemId(getRawSequenceNumber(), systemId);
    }

    /**
    * Set the system ID of this node. This method is provided so that a NodeInfo
    * implements the javax.xml.transform.Source interface, allowing a node to be
    * used directly as the Source of a transformation
    */

    public void setSystemId(String uri) {
        getPhysicalRoot().setSystemId(getRawSequenceNumber(), uri);
    }

	/**
	* Get the root node
	*/

	public NodeInfo getRoot() {
        ParentNodeImpl up = getRawParent();
        if (up == null || (up instanceof DocumentImpl && ((DocumentImpl)up).isImaginary())) {
            return this;
        } else {
            return up.getRoot();
        }
    }

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *     node is part of a tree that does not have a document node as its
     *     root, returns null.
     * @since 8.4
     */

	public DocumentInfo getDocumentRoot() {
		NodeInfo root = getRoot();
        if (root instanceof DocumentInfo) {
            return (DocumentInfo)root;
        } else {
            return null;
        }
    }

    /**
    * Get the system ID of the entity containing this element node.
    */

    public final String getSystemId() {
        DocumentImpl root = getPhysicalRoot();
        return (root == null ? null : root.getSystemId(getRawSequenceNumber()));
    }

    /**
    * Get the base URI of this element node. This will be the same as the System ID unless
    * xml:base has been used.
    */

    public String getBaseURI() {
        return Navigator.getBaseURI(this);
    }

    public StructuredQName getNodeName() {
        return elementName;
    }

    /**
     * Get a character string that uniquely identifies this node
     * @param buffer to contain the generated ID
     */

    public void generateId(FastStringBuffer buffer) {
        int sequence = getRawSequenceNumber();
        if (sequence >= 0) {
            getPhysicalRoot().generateId(buffer);
            buffer.append("e");
            buffer.append(Integer.toString(sequence));
        } else {
            getRawParent().generateId(buffer);
            buffer.append("f");
            buffer.append(Integer.toString(getSiblingPosition()));
        }
    }

    /**
    * Return the kind of node.
    * @return Type.ELEMENT
    */

    public final int getNodeKind() {
        return Type.ELEMENT;
    }

    /**
    * Copy this node to a given outputter (supporting xsl:copy-of)
    * @param out The outputter
     * @param copyOptions
     */

    public void copy(Receiver out, int copyOptions) throws XPathException {

        out.startElement(getNodeName(), 0);

        // output the namespaces

        int childCopyOptions = copyOptions & ~CopyOptions.ALL_NAMESPACES;
        if ((copyOptions & CopyOptions.LOCAL_NAMESPACES) != 0) {
            NamespaceBinding[] localNamespaces = getDeclaredNamespaces(null);
            for (int i=0; i<localNamespaces.length; i++) {
                NamespaceBinding ns = localNamespaces[i];
                if (ns == null) {
                    break;
                }
                out.namespace(ns, 0);
            }
        } else if ((copyOptions & CopyOptions.ALL_NAMESPACES) != 0) {
            NamespaceIterator.sendNamespaces(this, out);
            childCopyOptions |= CopyOptions.LOCAL_NAMESPACES;
        }

        // output the attributes

        if (attributeList != null) {
            for (int i=0; i<attributeList.getLength(); i++) {
                StructuredQName nc = attributeList.getStructuredQName(i);
                if (nc != null) {
                    // if attribute hasn't been deleted
                    out.attribute(nc, attributeList.getValue(i));
                }
            }
        }

        out.startContent();

        // output the children

        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            next.copy(out, childCopyOptions);
            next = (NodeImpl)next.getNextSibling();
        }

        out.endElement();
    }

    /**
     * Set the namespace declarations for the element
     * @param namespaces the list of namespace codes
     * @param namespacesUsed the number of entries in the list that are used
    */

    public void setNamespaceDeclarations(NamespaceBinding[] namespaces, int namespacesUsed) {
        namespaceList = new NamespaceBinding[namespacesUsed];
        System.arraycopy(namespaces, 0, namespaceList, 0, namespacesUsed);
    }


    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

//    public Iterator iteratePrefixes() {
//        return new Iterator() {
//            private boolean atStart = true;
//            private Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(ElementImpl.this);
//            public boolean hasNext() {
//                return (atStart || iter.hasNext());
//            }
//            public Object next() {
//                if (atStart) {
//                    atStart = false;
//                    return "xml";
//                } else {
//                    return iter.next().getPrefix();
//                }
//            }
//            public void remove() {
//                throw new UnsupportedOperationException("remove");
//            }
//        };
//    }

    /**
     * Get the URI bound to a given prefix in the in-scope namespaces of this element
     * @param prefix the prefix
     * @return the uri , or null if there is no in-scope binding for this prefix
     */

//    public String getURIForPrefix(String prefix, boolean useDefault) {
//        if (prefix.equals("xml")) {
//            return NamespaceConstant.XML;
//        }
//        if (prefix.isEmpty() && !useDefault) {
//            return NamespaceConstant.NULL;
//        }
//
//        if (namespaceList!=null) {
//            for (int i=0; i<namespaceList.length; i++) {
//                if ((namespaceList[i].getPrefix().equals(prefix))) {
//                    String uri = namespaceList[i].getURI();
//                    return (uri.isEmpty() && !prefix.isEmpty() ? null : uri);
//                }
//            }
//        }
//        NodeInfo next = getRawParent();
//        if (next.getNodeKind()==Type.DOCUMENT) {
//            // prefixCode==0 represents the empty namespace prefix ""
//            return (prefix.isEmpty() ? NamespaceConstant.NULL : null);
//        } else {
//            return ((ElementImpl)next).getURIForPrefix(prefix, useDefault);
//        }
//    }
    /**
    * Search the NamespaceList for a given URI, returning the corresponding prefix.
    * @param uri The URI to be matched.
    * @return The prefix corresponding to this URI. If not found, return null. If there is
    * more than one prefix matching the URI, the first one found is returned. If the URI matches
    * the default namespace, return an empty string.
    */

    public String getPrefixForURI(String uri) {
        if (uri.equals(NamespaceConstant.XML)) {
            return "xml";
        }
        UnfailingIterator iter = iterateAxis(Axis.NAMESPACE, AnyNodeTest.getInstance());
        while (true) {
            NodeInfo ns = (NodeInfo)iter.next();
            if (ns == null) {
                return null;
            }
            if (ns.getStringValue().equals(uri)) {
                return ns.getLocalPart(); // the prefix
            }
        }
	}

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
        return (namespaceList == null ? NamespaceBinding.EMPTY_ARRAY : namespaceList);
    }

    /**
    * Get the attribute list for this element.
    * @return The attribute list. This will not include any
    * namespace attributes. The attribute names will be in expanded form, with prefixes
    * replaced by URIs
    */

    public AttributeCollection getAttributeList() {
        return (attributeList == null ? AttributeCollection.EMPTY_ATTRIBUTE_COLLECTION : attributeList);
    }

    /**
    * Get the namespace list for this element.
    * @return The raw namespace list, as an array of name codes
    */

    public NamespaceBinding[] getNamespaceList() {
        return namespaceList;
    }

    /**
     * Get the value of a given attribute of this node
     * @param uri the namespace URI of the attribute name, or "" if the attribute is not in a namepsace
     * @param localName the local part of the attribute name
    *  @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(String uri, String localName) {
    	return (attributeList == null ? null : attributeList.getValue(uri, localName));
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
