package client.net.sf.saxon.ce.dom;

import java.util.ArrayList;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper.DocType;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.expr.Container;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.StaticContext;
import client.net.sf.saxon.ce.expr.StringLiteral;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.NameTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.NamespaceNode;
import client.net.sf.saxon.ce.tree.iter.*;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.tree.wrapper.SiblingCountingNode;
import client.net.sf.saxon.ce.tree.wrapper.VirtualNode;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.StringValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
//import com.google.gwt.xml.client.Attr;

/**
  * A node in the XML parse tree representing an HTML element, character content, or attribute.<P>
  * This is the implementation of the NodeInfo interface used as a wrapper for nodes in the DOM.
  * The DOM itself makes little distinction between XML and HTML, so the parent DocumentNodeWrapper
  * it used to identify the document type for cases where there's a difference - such as namespaces
  * and the capitalisation of element names 
  */

public class HTMLNodeWrapper implements NodeInfo, VirtualNode, SiblingCountingNode {

    protected Node node;
    private int namecode = -1;
    protected short nodeKind;
    private HTMLNodeWrapper parent;     // null means unknown
    protected HTMLDocumentWrapper docWrapper;
    protected int index;            // -1 means unknown
    protected int span = 1;         // the number of adjacent text nodes wrapped by this XMLNodeWrapper.
                                    // If span>1, node will always be the first of a sequence of adjacent text nodes
    /**
     * This constructor is protected: nodes should be created using the makeWrapper
     * factory method
     * @param node    The DOM node to be wrapped
     * @param parent  The XMLNodeWrapper that wraps the parent of this node
     * @param index   Position of this node among its siblings
     */
    protected HTMLNodeWrapper(Node node, HTMLNodeWrapper parent, int index) {
        this.node = node;
        this.parent = parent;
        this.index = index;
    }

    /**
     * Factory method to wrap a DOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The DOM node
     * @param docWrapper  The wrapper for the containing Document node
     * @return            The new wrapper for the supplied node
     * @throws NullPointerException if the node or the document wrapper are null
     */
    protected HTMLNodeWrapper makeWrapper(Node node, HTMLDocumentWrapper docWrapper) {
        if (node == null) {
            throw new NullPointerException("HTMLNodeWrapper#makeWrapper: Node must not be null");
        }
        if (docWrapper == null) {
            throw new NullPointerException("HTMLNodeWrapper#makeWrapper: XMLDocumentWrapper must not be null");
        }
        return makeWrapper(node, docWrapper, null, -1);
    }

    /**
     * Factory method to wrap a DOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The DOM node
     * @param docWrapper  The wrapper for the containing Document node     *
     * @param parent      The wrapper for the parent of the JDOM node
     * @param index       The position of this node relative to its siblings
     * @return            The new wrapper for the supplied node
     */

    protected HTMLNodeWrapper makeWrapper(Node node, HTMLDocumentWrapper docWrapper,
                                   HTMLNodeWrapper parent, int index) {
        HTMLNodeWrapper wrapper;
        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE:
        case DOCUMENT_FRAGMENT_NODE:
            return docWrapper;
        case Node.ELEMENT_NODE:
            wrapper = new HTMLNodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ELEMENT;
            break;
        case Type.ATTRIBUTE:
            wrapper = new HTMLNodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ATTRIBUTE;
            break;
        case Node.TEXT_NODE:
            wrapper = new HTMLNodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.TEXT;
            break;
        case CDATA_SECTION_NODE:
            wrapper = new HTMLNodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.TEXT;
            break;
        case Type.COMMENT:
            wrapper = new HTMLNodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.COMMENT;
            break;
        case Type.PROCESSING_INSTRUCTION:
        	wrapper = new HTMLNodeWrapper(node, parent, index);
        	wrapper.nodeKind = Type.PROCESSING_INSTRUCTION;
        	break;
        default:
            short nodeType = node.getNodeType();
            throw new IllegalArgumentException("Unsupported node type in DOM! " + node.getNodeType() + " instance " + node.toString());
        }
        wrapper.docWrapper = docWrapper;
        return wrapper;
    }
    
    private final static short DOCUMENT_FRAGMENT_NODE = 11;
    private final static short CDATA_SECTION_NODE = 4;
    

    /**
    * Get the underlying DOM node, to implement the VirtualNode interface
    */

    public Object getUnderlyingNode() {
        return node;
    }

    /**
     * Get the node underlying this virtual node. If this is a VirtualNode the method
     * will automatically drill down through several layers of wrapping.
     * @return The underlying node.
     */

    public Object getRealNode() {
        return getUnderlyingNode();
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return docWrapper.getConfiguration();
    }

    /**
     * Get the name pool for this node
     * @return the NamePool
     */

    public NamePool getNamePool() {
        return docWrapper.getNamePool();
    }

    /**
    * Return the type of node.
    * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
    */

    public int getNodeKind() {
        return nodeKind;
    }

    /**
    * Get the typed value of the item
    */

    public AtomicValue getTypedValue() {
        switch (getNodeKind()) {
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return new StringValue(getStringValueCS());
            default:
                return new UntypedAtomicValue(getStringValueCS());
        }
    }

    /**
    * Get the type annotation
    */

    public int getTypeAnnotation() {
        if (getNodeKind() == Type.ATTRIBUTE) {
            return StandardNames.XS_UNTYPED_ATOMIC;
        }
        return StandardNames.XS_UNTYPED;
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof HTMLNodeWrapper)) {
            return false;
        }
        HTMLNodeWrapper ow = (HTMLNodeWrapper)other;
        return getNodeKind()==ow.getNodeKind() &&
            getNameCode()==ow.getNameCode() &&  // redundant, but gives a quick exit
            getSiblingPosition()==ow.getSiblingPosition() &&
            getParent().isSameNodeInfo(ow.getParent());
    }

    /**
      * The equals() method compares nodes for identity. It is defined to give the same result
      * as isSameNodeInfo().
      * @param other the node to be compared with this node
      * @return true if this NodeInfo object and the supplied NodeInfo object represent
      *      the same node in the tree.
      * @since 8.7 Previously, the effect of the equals() method was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics. It is safer to use isSameNodeInfo() for this reason.
      * The equals() method has been defined because it is useful in contexts such as a Java Set or HashMap.
      */

     public boolean equals(Object other) {
        return other instanceof NodeInfo && isSameNodeInfo((NodeInfo)other);
    }

     /**
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

     public int hashCode() {
         FastStringBuffer buffer = new FastStringBuffer(FastStringBuffer.SMALL);
         generateId(buffer);
         return buffer.toString().hashCode();
     }

    /**
    * Get the System ID for the node.
    * @return the System Identifier of the entity in the source document containing the node,
    * or null if not known. Note this is not the same as the base URI: the base URI can be
    * modified by xml:base, but the system ID cannot.
    */

    public String getSystemId() {
        return docWrapper.baseURI;
    }

    public void setSystemId(String uri) {
        docWrapper.baseURI = uri;
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. In the DOM model, base URIs are held only an the document level.
    */

    public String getBaseURI() {
        NodeInfo n = this;
        if (getNodeKind() != Type.ELEMENT) {
            n = getParent();
        }
        // Look for an xml:base attribute
        while (n != null) {
            String xmlbase = Navigator.getAttributeValue(n, NamespaceConstant.XML, "base");
            if (xmlbase != null) {
                return xmlbase;
            }
            n = n.getParent();
        }
        // if not found, return the base URI of the document node
        return docWrapper.baseURI;
    }

    /**
    * Get line number
    * @return the line number of the node in its original source document; or -1 if not available
    */

    public int getLineNumber() {
        return -1;
    }

    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public int compareOrder(NodeInfo other) {
        if (other instanceof SiblingCountingNode) {
            return Navigator.compareOrder(this, (SiblingCountingNode)other);
        } else {
            // it's presumably a Namespace Node
            return -other.compareOrder(this);
        }
    }

    /**
    * Return the string value of the node. The interpretation of this depends on the type
    * of node. For an element it is the accumulated character content of the element,
    * including descendant elements.
    * @return the string value of the node
    */

    public String getStringValue() {
        return getStringValueCS().toString();

    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        switch (nodeKind) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                NodeList children1 = node.getChildNodes();
                StringBuffer sb1 = new StringBuffer(16);
                expandStringValue(children1, sb1);
                return sb1;              

            case Type.ATTRIBUTE:
                return emptyIfNull(getValue(node)); // previously used xml.client attr cast
                
            case Type.TEXT:
                if (span == 1) {
                    return emptyIfNull(getValue(node));
                } else {
                    FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
                    Node textNode = node;
                    for (int i=0; i<span; i++) {
                        fsb.append(emptyIfNull(getValue(textNode)));
                        textNode = textNode.getNextSibling();
                    }
                    return fsb.condense();
                }

            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return emptyIfNull(getValue(node));

            default:
                return "";
        }
    }
    
   /**
    * Use this alternative as Node class uses nodeValue always - 
    * this is now deprecated - there was an attempt to fix pre IE9 problem with
    * reporting values of head child elements - such as title - but
    * this failed with innerHTML property
    * - Issue with IE8 when value is boolean - need to convert to string
    */
	private static final native String getValue(Node n) /*-{
		var v;
		if (n.nodeValue) {
			v = n.nodeValue;
		} else {
			v = n.value;
		}
		if(v == undefined){
			return v;
		}
		return String(v);
	}-*/;
	
	private static final native String getTypeOfNodeValue(Node n) /*-{
		if (n.nodeValue) {
			return n.nodeValue + " nv";
		} else {
			return n.value + " v";
		}
    }-*/;

    /**
     * Treat a node value of null as an empty string.
     * @param s the node value
     * @return a zero-length string if s is null, otherwise s
     */

    private static String emptyIfNull(String s) {
        return (s==null ? "" : s);
    }

    private static void expandStringValue(NodeList list, StringBuffer sb) {
        final int len = list.getLength();
        for (int i = 0; i < len; i++) {
            Node child = list.getItem(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    expandStringValue(child.getChildNodes(), sb);                    
                    break;
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    break;
                default:
                    sb.append(getValue(child));
            }
        }
    }

	/**
	* Get name code. The name code is a coded form of the node name: two nodes
	* with the same name code have the same namespace URI, the same local name,
	* and the same prefix. By masking the name code with &0xfffff, you get a
	* fingerprint: two nodes with the same fingerprint have the same local name
	* and namespace URI.
    * @see client.net.sf.saxon.ce.om.NamePool#allocate allocate
	*/

	public int getNameCode() {
        if (namecode != -1) {
            // this is a memo function
            return namecode;
        }
        int nodeKind = getNodeKind();
        if (nodeKind == Type.ELEMENT || nodeKind == Type.ATTRIBUTE) {
            String prefix = getPrefix();
            if (prefix==null) {
                prefix = "";
            }
            namecode = docWrapper.getNamePool().allocate(prefix, getURI(), getLocalPart());
            return namecode;
        } else if (nodeKind == Type.PROCESSING_INSTRUCTION ) {
            namecode = docWrapper.getNamePool().allocate("", "", getLocalPart());
            return namecode;
        } else {
            return -1;
        }
	}

	/**
	* Get fingerprint. The fingerprint is a coded form of the expanded name
	* of the node: two nodes
	* with the same name code have the same namespace URI and the same local name.
	* A fingerprint of -1 should be returned for a node with no name.
	*/

	public int getFingerprint() {
        int nc = getNameCode();
        if (nc == -1) {
            return -1;
        }
	    return nc&0xfffff;
	}

    /**
    * Get the local part of the name of this node. This is the name after the ":" if any.
    * @return the local part of the name. For an unnamed node, returns null, except for
     * un unnamed namespace node, which returns "".
    */
	
	public String getRawLocalName() {
		return getLocalName(node);
	}

    public String getLocalPart() {
        switch (getNodeKind()) {
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
            	// was node.getNodeName().toLower();
            	return getDomCorrectedName(getLocalName(node));
            case Type.PROCESSING_INSTRUCTION:
                return node.getNodeName();
            default:
                return null;
        }
    }
    // assume this is xml if there is a namespaceURI property
    private native String getLocalName(Node node) /*-{
    	if (node.localName) {
    		return node.localName;
    	} else {
    		var fullname = node.nodeName;
    		var name;
    		var pos = fullname.indexOf(':');
    		if (pos > -1) {
    			return fullname.substring(pos + 1);
    		} else { return fullname; }
    	}
    }-*/;

    /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
     * @return The URI of the namespace of this node. For an unnamed node,
     *     or for a node with an empty prefix, return an empty
     *     string.
    */
    public String getURI() {
    	return  getNodeURI(node, false);
    }

    private String getNodeURI(Node lNode, boolean isLocalAttribute) {
        NodeInfo element;
        // isLocalAttribute is true when method called internally to get uri of
        // an attribute belonging to this element
        if (isLocalAttribute || nodeKind == Type.ELEMENT) {
        	element = this;
        } else if (nodeKind == Type.ATTRIBUTE) {
        	element = this.getParent();
        } else {
        	 return "";
        }
        
    	String uri = getNodeNamespace(lNode);
    	boolean isHTML = (docWrapper.getDocType() == DocType.HTML);
    	// don't return XHTML namespace for HTML docs that have no XHTML namespace declaration
    	if (uri == null) {
    		if (isHTML) return "";
    	// some browsers add XHTML namespace to all HTML elements - whether namespace declared or not:
    	} else if (isHTML && uri.equals(NamespaceConstant.XHTML)) {
    		return "";
    	} else {
    		return uri;
    	}
    	
    	// Otherwise we have to work it out the hard way...
    	
        if (lNode.getNodeName().startsWith("xml:")) {
            return NamespaceConstant.XML;
        }
        
        String[] parts;
        try {
            parts = NameChecker.getQNameParts(lNode.getNodeName());
        } catch (QNameException e) {
            throw new IllegalStateException("Invalid QName in DOM node. " + e);
        }

        if ((isLocalAttribute || nodeKind == Type.ATTRIBUTE) && parts[0].length() == 0) {
            // for an attribute, no prefix means no namespace
            uri = "";
        } else {
            AxisIterator nsiter = element.iterateAxis(Axis.NAMESPACE);
            while (true) {
                NodeInfo ns = (NodeInfo)nsiter.next();
                if (ns == null) {
                	break;
                }
                if (ns.getLocalPart().equals(parts[0])) {
                    uri = ns.getStringValue();
                    break;
                }
            }
            if (uri == null) {
                if (parts[0].length() == 0) {
                    uri = "";
                } else {
                    throw new IllegalStateException("Undeclared namespace prefix in DOM input: " + parts[0]);
                }
            }
        }

        return uri;
    }
    
    public final native String getNodeNamespace(Node node) /*-{
    	if (node.namespaceURI) {
    		return node.namespaceURI;
    	} else {
    		return null;
    	}
    }-*/;

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     * @return The prefix of the name of the node.
     */

    public String getPrefix() {
        int kind = getNodeKind();
        if (kind == Type.ELEMENT || kind == Type.ATTRIBUTE) {
        	return getNodePrefix(node);
        }
        return "";
    }
    
    private native String getNodePrefix(Node node) /*-{
    	if (node.prefix) {
    		return node.prefix;
    	} else {
    		return "";
    	}
    }-*/;

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        switch (nodeKind) {
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
                return getDomCorrectedName(node.getNodeName());
            case Type.PROCESSING_INSTRUCTION:
                return node.getNodeName();
            default:
                return "";
        }
    }
    
    /**
     * Set the name to lower-case if the node is an HTML
     * or XHTML node type - but preserve case for other XML.
     */
    
    private String getDomCorrectedName(String name) {
        if (name == null) {
        	return null;
        }
    	switch (docWrapper.getDocType()) {
	    	case NONHTML:
	    		return name;
	    	case HTML:
	    		return name.toLowerCase();
	    	case XHTML:
	    		String ns = getURI();
	    		if (ns != null && ns.equals(NamespaceConstant.XHTML)) {
	    			return name.toLowerCase();
	    		} else {
	    			return name;
	    		}
	    	default:
	    		// if unknown - assume HEAD etc. is from HTML document
	    		if (name.matches("[A-Z]+")) {
	    			return name.toLowerCase();
	    		} else {
	    		    return name;
	    		}
    	}
    }

    /**
    * Get the NodeInfo object representing the parent of this node
    */

    public NodeInfo getParent() {
        if (parent==null) {
            switch (getNodeKind()) {
            case Type.ATTRIBUTE:
                throw new IllegalStateException("parent of attribute node is unknown");
            default:
                Node p = node.getParentNode();
                if (p==null) {
                    return null;
                } else {
                    parent = makeWrapper(p, docWrapper);
                }
            }
        }
        return parent;
    }

    /**
     * Get the index position of this node among its siblings (starting from 0).
     * In the case of a text node that maps to several adjacent siblings in the DOM,
     * the numbering actually refers to the position of the underlying DOM nodes;
     * thus the sibling position for the text node is that of the first DOM node
     * to which it relates, and the numbering of subsequent XPath nodes is not necessarily
     * consecutive.
     */

    public int getSiblingPosition() {
        if (index == -1) {
            switch (nodeKind) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    int ix = 0;
                    Node start = node;
                    while (true) {
                        start = start.getPreviousSibling();
                        if (start == null) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }
                case Type.ATTRIBUTE:
                    ix = 0;
                    int fp = getFingerprint();
                    AxisIterator iter = parent.iterateAxis(Axis.ATTRIBUTE);
                    while (true) {
                        NodeInfo n = (NodeInfo)iter.next();
                        if (n==null || n.getFingerprint()==fp) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }

                case Type.NAMESPACE:
                    ix = 0;
                    fp = getFingerprint();
                    iter = parent.iterateAxis(Axis.NAMESPACE);
                    while (true) {
                        NodeInfo n = (NodeInfo)iter.next();
                        if (n==null || n.getFingerprint()==fp) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }
                default:
                    index = 0;
                    return index;
            }
        }
        return index;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
                if (nodeKind==Type.DOCUMENT) {
                    return EmptyIterator.getInstance();
                }
                return new Navigator.AncestorEnumeration(this, false);

            case Axis.ANCESTOR_OR_SELF:
                if (nodeKind==Type.DOCUMENT) {
                    return SingleNodeIterator.makeIterator(this);
                }
                return new Navigator.AncestorEnumeration(this, true);

            case Axis.ATTRIBUTE:
                if (nodeKind!=Type.ELEMENT) {
                    return EmptyIterator.getInstance();
                }
                HTMLAttributeNode[] nodes = getAltAttributes();
                return new NodeArrayIterator(nodes);

            case Axis.CHILD:
                if (hasChildNodes()) {
                    return new Navigator.EmptyTextFilter(new ChildEnumeration(this, true, true, false));
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT:
                if (hasChildNodes()) {
                    return new Navigator.DescendantEnumeration(this, false, true);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                 return new Navigator.DescendantEnumeration(this, true, true);

            case Axis.FOLLOWING:
                 return new Navigator.FollowingEnumeration(this);

            case Axis.FOLLOWING_SIBLING:
                 switch (nodeKind) {
                    case Type.DOCUMENT:
                    case Type.ATTRIBUTE:
                    case Type.NAMESPACE:
                        return EmptyIterator.getInstance();
                    default:
                        return new Navigator.EmptyTextFilter(
                                new ChildEnumeration(this, false, true, false));
                 }

            case Axis.NAMESPACE:
                 if (nodeKind!=Type.ELEMENT) {
                     return EmptyIterator.getInstance();
                 }
                 return NamespaceNode.makeIterator(this, AnyNodeTest.getInstance());

            case Axis.PARENT:
                 getParent();
                 return SingleNodeIterator.makeIterator(parent);

            case Axis.PRECEDING:
                 return new Navigator.PrecedingEnumeration(this, false);

            case Axis.PRECEDING_SIBLING:
                 switch (nodeKind) {
                    case Type.DOCUMENT:
                    case Type.ATTRIBUTE:
                    case Type.NAMESPACE:
                        return EmptyIterator.getInstance();
                    default:
                        return new Navigator.EmptyTextFilter(
                                new ChildEnumeration(this, false, false, false));
                 }

            case Axis.SELF:
                 return SingleNodeIterator.makeIterator(this);

            case Axis.PRECEDING_OR_ANCESTOR:
                 return new Navigator.PrecedingEnumeration(this, true);

            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        if (axisNumber == Axis.CHILD && nodeTest.getPrimitiveType() == Type.ELEMENT) {
            // common case: avoid creating wrappers for the text nodes
            if (hasChildNodes()) {
                return new Navigator.AxisFilter(
                        new ChildEnumeration(this, true, true, true), nodeTest);
            } else {
                return EmptyIterator.getInstance();
            }
        }

        if (docWrapper.getDocType() != DocType.NONHTML && axisNumber == Axis.ATTRIBUTE && nodeTest instanceof NameTest) {
            if (nodeKind == Type.ELEMENT) {
            	int fp = nodeTest.getFingerprint();
                String uri = getNamePool().getURI(fp);
                String name = getNamePool().getLocalName(fp);
                String prefix = getNamePool().getPrefix(fp);
                String value;
                boolean keepUri = false;
                if (NamespaceConstant.HTML_PROP.equals(uri)) {
                    value = ((Element)node).getPropertyString(name);
                } else if (NamespaceConstant.HTML_STYLE_PROP.equals(uri)) {
                	// unescape if style name starts with a '-'like -webkit-transition
                	if(name.length() > 1 && name.charAt(0) == '_' && name.charAt(1) == '-') {
                		name = name.substring(1);
                	}
                	name = HTMLWriter.getCamelCaseName(name);
                    value = ((Element)node).getStyle().getProperty(name);
                } else {
                	value = getAltAttribute(name);
                	if (docWrapper.getDocType() == DocType.XHTML) {
                		return new Navigator.AxisFilter(iterateAxis(axisNumber), nodeTest);
                	}
                }
                //HTML only (not XHTML) - so don't use prefix / namespace
                if (value == null || value.length()==0) {
                	if (getAttributeNames().contains(name)) {
                		return SingleNodeIterator.makeIterator(new HTMLAttributeNode(this, name, "", "", ""));
                	} else {
                		return EmptyIterator.getInstance();
                	}
                } else {
                    return SingleNodeIterator.makeIterator(new HTMLAttributeNode(this, name, "", "", value));
                }
            } else {
                return EmptyIterator.getInstance();
            }
        }
        return new Navigator.AxisFilter(iterateAxis(axisNumber), nodeTest);
    }
    
  
    /**
     * Workaround for IE issue when getAttribute won't return
     * a value for the style attribute - IE supports non-standard outerHTML
     */
    
    String getAltAttribute(String name) {
        String value = ((Element)node).getAttribute(name);
        // test for IE issue with style attribute:
        if(value.equals("[object]") && name.equals("style")) { // solves IE issue!
        	value = getStyleAttribute((Element)node);
        } else if (name.equals("class") && value.equals("")) {
        	value = ((Element)node).getAttribute("className"); //solves < IE8 issue!
        } else if (name.equals("for") && value.equals("")) {
        	value = ((Element)node).getAttribute("htmlFor"); 
        }
        return value;
    }
    
    private String getStyleAttribute(Element elem) {
    	// style attribute must exist and must be quoted properly
    	// IE9 does not include style attributes text if their value is not valid
    	// we therefore don't expect non-quoted styles like <h1 style=color id="abc"...
    	String value = getOuterHTML(elem);
    	
    	RegExp quotesPattern = RegExp.compile("(?:\".*?\"|\'.*?\'|[^\'\"]+|['\"])", "g"); // g=global: = all-matches 
    	RegExp stylePattern = RegExp.compile("[\\s]style\\s*="); // e.g. match: <h1 style=
    	MatchResult m = quotesPattern.exec(value);

    	int i = 0;
    	boolean styleFound = false;
    	String styleContent = "";
    	String nonQ = "";
    	while (quotesPattern.getLastIndex() > 0) {
    		if (i % 2 == 0) {
    			nonQ = m.getGroup(0); // not in quotes - so check
    			MatchResult ms = stylePattern.exec(nonQ);
    			styleFound = ms.getGroupCount() > 0;
    			if (!styleFound && nonQ.indexOf('>') > -1) {
    				break; // no more attributes
    			}
    		} else if (styleFound) {
    			styleContent = m.getGroup(0);
    			// remove enclosing quotes
    			styleContent = styleContent.substring(1, styleContent.length() - 1);
    			break;    			
    		}
    		i++;
    		m = quotesPattern.exec(value);
    	}	   	
     	return styleContent;
    }
    
    // Non IE browsers should return null - IE browsers > 7 should return null
    private static native String getOuterHTML(Element elem) /*-{
    		return elem.outerHTML;
    }-*/;
    
    /**
     * Native method to get all attributes for an element as an array of nodes
     * @param elem the element whose attributes are required
     * @return
     */
    
    private static native JsArray<Node> getAttributes(Element elem) /*-{
       return elem.attributes;
    }-*/;
    
  
    private int[] skipWhitespace(int pos, String value, boolean invert) {
		// return next char that isn't whitespace along with
		// the position offset. The invert argument lets you
		// skip non whitespace instead
		int offset = 0;
		char ch = '\0';
		while (true) {
			ch = value.charAt(pos + offset);
			// DOM whitespace may be normalized
			boolean isWhitespace = (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t'); 
			if (invert) {
				isWhitespace = !isWhitespace;
			}
			if (!(isWhitespace)) {
				break;
			}
			offset++;
			if(!(pos + offset < value.length())) {
				ch = '\0'; // we're at end of string
				break;
			}
		}
		return new int[] {offset, ch};
    }
    
    /**
     * Alternate to Native method to get all attributes for an element as an array of nodes
     * @param elem the element whose attributes are required. This alternate is required
     * because of sketchy support for the attributes property in various browsers including IE
     * @return
     */
    
    private HTMLAttributeNode[] attributeList = null;
    private ArrayList<String> attributeNames = null;
    private ArrayList<NamespaceBinding> namespaceBindings = null;
    
    private ArrayList<String> getAttributeNames() {
    	if (attributeNames != null) {
    		return attributeNames;
    	} else {
	    	getAltAttributes();
	    	return attributeNames;
    	}
    }
    
   
    private HTMLAttributeNode[] getAltAttributes() {
        if (attributeList != null) {
        	return attributeList;
        }
        Element elem = (Element)node;
    	
    	// Browser implementations of attributes property are potentially buggy - but preferred to
    	// parsing the string. But IE6 and IE7 both list all possible attributes whether they
    	// exist or not - so use outerHTML in these cases.
        int ieVersion = Configuration.getIeVersion();
    	if (ieVersion < 0 || ieVersion > 8) {
    		return getMainAttributes(elem);
    	}
    	
    	String value = getOuterHTML(elem);
    	
    	if (value == null) {
    		return getMainAttributes(elem);
    	}   	
    	
    	ArrayList<String> nodeNames = new ArrayList<String>();
    	ArrayList<String> xmlnsNames = new ArrayList<String>(); 
    	ArrayList<String> xmlnsUris = new ArrayList<String>();
    	namespaceBindings = new ArrayList<NamespaceBinding>();
    	
    	RegExp quotesPattern = RegExp.compile("(?:\"(.|\n)*?\"|\'(.|\n)*?\'|[^\'\"]+|['\"])", "gm"); // g=global: = all-matches m=multiline 
    	MatchResult m = quotesPattern.exec(value);

    	int i = 0;
    	String nonQ = "";
    	boolean awaitingXmlnsUri = false;
    	while (quotesPattern.getLastIndex() > 0) {
    		
    		if (i % 2 == 0) {
    			nonQ = m.getGroup(0); // not in quotes - so check
    			StringBuffer sb = new StringBuffer();
    			boolean endOfTag = false;
    			boolean isName = !(i == 0);// first part is not a name: e.g. \r\n<H1 style="
    			int start = 0;
    			if (i == 0) {
    				start = nonQ.indexOf('<') + 1;   				
    			}
    			for (int x = start; x < nonQ.length(); x++) {
    				int[] offsetChar = skipWhitespace(x, nonQ, false);
    				int offset = offsetChar[0];
    				
    				if(offset > 0 && !(isName)) {
    					// no whitespace allow in an unquoted value, so we're back on a name
    					isName = true;
    				}
    				char ch = (char)offsetChar[1];
    				if (ch == '\0') break;
    				if (ch == '=') {
    					// part after the '=' is the value, until next whitespace
    					isName = false;
    					String attName = sb.toString();
    					
    					if (attName.startsWith("xmlns")) {
    						xmlnsNames.add(attName);
    						awaitingXmlnsUri = true;
    					} else if(attName.length() != 0) {
    						nodeNames.add(attName);
    						awaitingXmlnsUri = false;
    					}

    					sb = new StringBuffer();
    					continue;
    				} else if (ch == '>') {
    					endOfTag = true;
    					break;
    				}
    				
    				if (isName) {
    					sb.append(ch);
    				}   				
    				x += offset;
    			} // end for
    			if (endOfTag) {
    				break;
    			}
    		} else if (awaitingXmlnsUri) {// ends if i % 2
    			xmlnsUris.add(m.getGroup(0));
    		}
    		i++;
    		m = quotesPattern.exec(value);
    	} // end while
    	
    	HTMLAttributeNode[] nodeArray = new HTMLAttributeNode[nodeNames.size()];
    	for (int y = 0; y < nodeNames.size(); y++) {
    		String name = nodeNames.get(y);
    		String nValue = getAltAttribute(name); // elem.getAttribute(name);
    		// must be html because using outerHTML attribute - so no namespaces
    		HTMLAttributeNode hNode = new HTMLAttributeNode(this, name, "", "", nValue);
    		nodeArray[y] = hNode;
    	}
        int count = 0;
    	for (String name: xmlnsNames) {
    		// use substring to exclude xmlns: prefix
    		boolean noPrefix = (name.length() == 5);
    		String prefix = (noPrefix)? "": name.substring(6);
    		// xmlns declarations with prefixes can't be fetched using getAttribute
    		String attValue = (noPrefix)? getAltAttribute(name) : trimEdgeChars(xmlnsUris.get(count)); 
    		namespaceBindings.add(new NamespaceBinding(prefix, attValue));
    		count++;
    	}
    	attributeList = nodeArray; // for later use
    	attributeNames = nodeNames;
     	return nodeArray;
    }
    
    private String trimEdgeChars(String name) {
    	return name.substring(1, name.length() - 1);
    }
    
    private HTMLAttributeNode[] getMainAttributes(Element elem) {
        JsArray<Node> attributes = getAttributes((Element)node);
        ArrayList<String> nodeNames = new ArrayList<String>();
    	
        int len = attributes.length();
        ArrayList<HTMLAttributeNode> nodeAtts = new ArrayList<HTMLAttributeNode>();
        namespaceBindings = new ArrayList<NamespaceBinding>();
        // assume attributes within HTML doc have no namespace - unless they have a prefix
        boolean getNamespaces = (docWrapper.getDocType() != DocType.HTML);
        for (int i=0; i<len; i++) {
        	Node attNode = attributes.get(i);
        	String name = attNode.getNodeName();
        	String val = "";
        	try {
        	val = getValue(attNode); // changed for compatibility
        	} catch(Exception e){
        		String rp = getTypeOfNodeValue(attNode);
        		String a = "a"; //kilroy
        	}
        	if (name.startsWith("xmlns:")) {
        		namespaceBindings.add(new NamespaceBinding(name.substring(6),val));
        	} else if (name.equals("xmlns")){
        		namespaceBindings.add(new NamespaceBinding("",val));
        	} else {
        		if(getNamespaces || name.indexOf(':') > -1){
        			String pfx = getNodePrefix(attNode);
        			String uri = getNodeURI(attNode, true);
        			String local = getLocalName(attNode);
        			if(uri == null){uri = "";} // xhtml attributes use xlink namespace!
        			nodeAtts.add(new HTMLAttributeNode(this, local, pfx, uri, val));
        		} else {
        			nodeAtts.add(new HTMLAttributeNode(this, name, "", "", val));
        		}
	            nodeNames.add(name);
        	}
        }
        HTMLAttributeNode[] nodes = new HTMLAttributeNode[nodeAtts.size()];
        nodeAtts.toArray(nodes);
        
    	attributeList = nodes; // for later use
    	attributeNames = nodeNames;
        return nodes;
    }

    /**
    * Get the root node - always a document node with this tree implementation
    * @return the NodeInfo representing the containing document
    */

    public NodeInfo getRoot() {
        return docWrapper;
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document
    */

    public DocumentInfo getDocumentRoot() {
        return docWrapper;
    }

    /**
    * Determine whether the node has any children. <br />
    * Note: the result is equivalent to <br />
    * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
    */

    public boolean hasChildNodes() {
        // An attribute node has child text nodes
        return nodeKind != Type.ATTRIBUTE && node.hasChildNodes();
    }

    /**
    * Get a character string that uniquely identifies this node.
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @param buffer a buffer to contain a string that uniquely identifies this node, across all
    * documents
     *
     */

    public void generateId(FastStringBuffer buffer) {
        Navigator.appendSequentialKey(this, buffer, true);
        //buffer.append(Navigator.getSequentialKey(this));
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return getDocumentRoot().getDocumentNumber();
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int copyOptions) throws XPathException {
        Navigator.copy(this, out, docWrapper.getNamePool(), copyOptions);
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
    	if (nodeKind == Node.ELEMENT_NODE) {
	    	if (namespaceBindings == null) {
	    		this.getAltAttributes();
	    	}
	    	NamespaceBinding[] bindings = new NamespaceBinding[namespaceBindings.size()];
	    	namespaceBindings.toArray(bindings);
	        return bindings;
    	} else {
    		return null;
    	}
    }

    /**
    * The class ChildEnumeration handles not only the child axis, but also the
    * following-sibling and preceding-sibling axes. It can also iterate the children
    * of the start node in reverse order, something that is needed to support the
    * preceding and preceding-or-ancestor axes (the latter being used by xsl:number)
    */

    private final class ChildEnumeration extends AxisIteratorImpl {

        private HTMLNodeWrapper start;
        private HTMLNodeWrapper commonParent;
        private boolean downwards;  // iterate children of start node (not siblings)
        private boolean forwards;   // iterate in document order (not reverse order)
        private boolean elementsOnly;
        NodeList childNodes;
        private int childNodesLength;
        private int ix;             // index of the current DOM node within childNodes;
                                    // in the case of adjacent text nodes, index of the first in the group
        private int currentSpan;    // number of DOM nodes mapping to the current XPath node

        public ChildEnumeration(HTMLNodeWrapper start,
                                boolean downwards, boolean forwards, boolean elementsOnly)  {
            this.start = start;
            this.downwards = downwards;
            this.forwards = forwards;
            this.elementsOnly = elementsOnly;
            position = 0;
            currentSpan = 1;

            if (downwards) {
                commonParent = start;
            } else {
                commonParent = (HTMLNodeWrapper)start.getParent();
            }

            childNodes = commonParent.node.getChildNodes();
            childNodesLength = childNodes.getLength();
            if (downwards) {
                currentSpan = 1;
                if (forwards) {
                    ix = -1;                        // just before first
                } else {
                    ix = childNodesLength;          // just after last
                }
            } else {
                ix = start.getSiblingPosition();    // at current node
                currentSpan = start.span;
            }
        }

        /**
         * Starting with ix positioned at a node, which in the last in a span, calculate the length
         * of the span, that is the number of DOM nodes mapped to this XPath node.
         * @return the number of nodes spanned
         */

        private int skipPrecedingTextNodes() {
            int count = 0;
            while (ix >= count) {
                Node node = childNodes.getItem(ix - count);
                short kind = node.getNodeType();
                if (kind == Node.TEXT_NODE) {
                    count++;
                } else {
                    break;
                }
            }
            return (count == 0 ? 1 : count);
        }

        /**
         * Starting with ix positioned at a node, which in the first in a span, calculate the length
         * of the span, that is the number of DOM nodes mapped to this XPath node.
         * @return the number of nodes spanned
         */

        private int skipFollowingTextNodes() {
            int count = 0;
            int pos = ix;
            final int len = childNodesLength;
            while (pos < len) {
                Node node = childNodes.getItem(pos);
                short kind = node.getNodeType();
                if (kind == Node.TEXT_NODE) {
                    pos++;
                    count++;
                } else {
                    break;
                }
            }
            return (count == 0 ? 1 : count);
        }

        public Item next() {
            while (true) {
                if (forwards) {
                    ix += currentSpan;
                    if (ix >= childNodesLength) {
                        position = -1;
                        return null;
                    } else {
                        currentSpan = skipFollowingTextNodes();
                        Node currentDomNode = childNodes.getItem(ix);
                        switch (currentDomNode.getNodeType()) {
                        	case Type.PROCESSING_INSTRUCTION:
	                        	if(elementsOnly || "XML".equalsIgnoreCase(currentDomNode.getNodeName())){
	                        		continue;
	                        	} else {
	                        		break;
	                        	}
                            case Type.DOCUMENT_TYPE:
                                continue;
                            case Node.ELEMENT_NODE:
                                break;
                            default:
                                if (elementsOnly) {
                                    continue;
                                } else {
                                    break;
                                }
                        }
                        HTMLNodeWrapper wrapper = makeWrapper(currentDomNode, docWrapper, commonParent, ix);
                        wrapper.span = currentSpan;
                        position++;
                        return current = wrapper;
                    }
                } else {
                    ix--;
                    if (ix < 0) {
                        position = -1;
                        return null;
                    } else {
                        currentSpan = skipPrecedingTextNodes();
                        ix -= (currentSpan - 1);
                        Node currentDomNode = childNodes.getItem(ix);
                        switch (currentDomNode.getNodeType()) {
	                    	case Type.PROCESSING_INSTRUCTION:
	                        	if(elementsOnly || "XML".equalsIgnoreCase(currentDomNode.getNodeName())){
	                        		continue;
	                        	} else {
	                        		break;
	                        	}
                            case Type.DOCUMENT_TYPE:
                                continue;
                            case Node.ELEMENT_NODE:
                                break;
                            default:
                                if (elementsOnly) {
                                    continue;
                                } else {
                                    break;
                                }
                        }
                        HTMLNodeWrapper wrapper = makeWrapper(currentDomNode, docWrapper, commonParent, ix);
                        wrapper.span = currentSpan;
                        position++;
                        return current = wrapper;
                    }
                }
            }
        }

        public SequenceIterator getAnother() {
            return new ChildEnumeration(start, downwards, forwards, elementsOnly);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
        }

    } // end of class ChildEnumeration


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.