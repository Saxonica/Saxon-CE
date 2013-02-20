package client.net.sf.saxon.ce.tree.util;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.*;
import client.net.sf.saxon.ce.tree.wrapper.SiblingCountingNode;
import client.net.sf.saxon.ce.type.Type;

import java.util.ArrayList;
import java.util.List;


/**
 * The Navigator class provides helper classes for navigating a tree, irrespective
 * of its implementation
 *
 * @author Michael H. Kay
 */


public final class Navigator {

    // Class is never instantiated
    private Navigator() {
    }

    /**
     * Helper method to get an attribute of an element
     * @param element the element node
     * @param uri the attribute name URI
     * @param local the local name of the attribute
     */

    public static String getAttributeValue(NodeInfo element, String uri, String local) {
        NameTest test = new NameTest(Type.ATTRIBUTE, uri, local, element.getNamePool());
        AxisIterator iterator = element.iterateAxis(Axis.ATTRIBUTE, test);
        NodeInfo attribute = (NodeInfo)iterator.next();
        if (attribute == null) {
            return null;
        } else {
            return attribute.getStringValue();
        }

    }

    /**
     * Helper method to get the base URI of an element or processing instruction node
     * @param node the node whose base URI is required
     * @return the base URI of the node
     * @since 8.7
     */

    public static String getBaseURI(NodeInfo node) {
        String xmlBase = getAttributeValue(node, NamespaceConstant.XML, "base");
        if (xmlBase != null) {
            URI baseURI;
            try {
                baseURI = new URI(xmlBase, true);
                if (!baseURI.isAbsolute()) {
                    NodeInfo parent = node.getParent();
                    if (parent == null) {
                        // We have a parentless element with a relative xml:base attribute.
                        // See for example test XQTS fn-base-uri-10 and base-uri-27
                        URI base = new URI(node.getSystemId());
                        URI resolved = (xmlBase.length()==0 ? base : base.resolve(baseURI.toString()));
                        return resolved.toString();
                    }
                    String startSystemId = node.getSystemId();
                    String parentSystemId = parent.getSystemId();
                    URI base = new URI(startSystemId.equals(parentSystemId) ? parent.getBaseURI() : startSystemId);
                    baseURI = (xmlBase.length()==0 ? base : base.resolve(baseURI.toString()));
                }
            } catch (URI.URISyntaxException e) {
                // xml:base is an invalid URI. Just return it as is: the operation that needs the base URI
                // will probably fail as a result.     \
                return xmlBase;
            }
            return baseURI.toString();
        }
        String startSystemId = node.getSystemId();
        NodeInfo parent = node.getParent();
        if (parent == null) {
            return startSystemId;
        }
        String parentSystemId = parent.getSystemId();
        if (startSystemId.equals(parentSystemId)) {
            return parent.getBaseURI();
        } else {
            return startSystemId;
        }
    }

    /**
     * Get an absolute XPath expression that identifies a given node within its document
     *
     * @param node the node whose path is required. If null is supplied,
     *             an empty string is returned - this fact is used in making a recursive call
     *             for a parentless node.
     * @return a path expression that can be used to retrieve the node
     */

    public static String getPath(NodeInfo node) {
        return getPath(node, null);
    }

    /**
     * Get an absolute XPath expression that identifies a given node within its document
     *
     * @param node the node whose path is required. If null is supplied,
     *             an empty string is returned - this fact is used in making a recursive call
     *             for a parentless node.
     * @param context the XPath dynamic evaluation context. May be null if no context is known
     * @return a path expression that can be used to retrieve the node
     */

    public static String getPath(NodeInfo node, XPathContext context) {
        if (node == null) {
            return "";
        }
        String pre;
        NodeInfo parent = node.getParent();
        // System.err.println("node = " + node + " parent = " + parent);

        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                return "/";
            case Type.ELEMENT:
                if (parent == null) {
                    return node.getDisplayName();
                } else {
                    pre = getPath(parent, context);
                    if (pre.equals("/")) {
                        return '/' + node.getDisplayName();
                    } else {
                        int position = 1;
                        int count = 0;
                        AxisIterator siblings = parent.iterateAxis(Axis.CHILD, new NameTest(node));
                        while (true) {
                            NodeInfo sib = (NodeInfo)siblings.next();
                            if (sib == null) {
                                break;
                            }
                            count++;
                            if (sib.isSameNodeInfo(node)) {
                                position = count;
                            }
                        }
                        try {
                            String index = (count == 1 ? "" : "[" + position + "]");
                            return pre + '/' + node.getDisplayName() + index;
                        } catch (UnsupportedOperationException e) {
                            // Happens when streaming
                            return pre + '/' + node.getDisplayName();
                        }
                    }
                }
            case Type.ATTRIBUTE:
                return getPath(parent, context) + "/@" + node.getDisplayName();
            case Type.TEXT:
                pre = getPath(parent, context);
                return (pre.equals("/") ? "" : pre) +
                        "/text()[" + getNumberSimple(node, context) + ']';
            case Type.COMMENT:
                pre = getPath(parent, context);
                return (pre.equals("/") ? "" : pre) +
                        "/comment()[" + getNumberSimple(node, context) + ']';
            case Type.PROCESSING_INSTRUCTION:
                pre = getPath(parent, context);
                return (pre.equals("/") ? "" : pre) +
                        "/processing-instruction()[" + getNumberSimple(node, context) + ']';
            case Type.NAMESPACE:
                String test = node.getLocalPart();
                if (test.length() == 0) {
                    // default namespace: need a node-test that selects unnamed nodes only
                    test = "*[not(local-name()]";
                }
                return getPath(parent, context) + "/namespace::" + test;
            default:
                return "";
        }
    }

    /**
     * Get simple node number. This is defined as one plus the number of previous siblings of the
     * same node type and name. It is not accessible directly in XSL.
     *
     * @param node    The node whose number is required
     * @param context Used for remembering previous result, for
     *                performance. May be null.
     * @return the node number, as defined above
     * @throws XPathException if any error occurs
     */

    public static int getNumberSimple(NodeInfo node, XPathContext context)  {

        //checkNumberable(node);

        int fingerprint = node.getFingerprint();
        NodeTest same;

        if (fingerprint == -1) {
            same = NodeKindTest.makeNodeKindTest(node.getNodeKind());
        } else {
            same = new NameTest(node);
        }

        Controller controller = (context == null ? null : context.getController());
        AxisIterator preceding = node.iterateAxis(Axis.PRECEDING_SIBLING, same);

        int i = 1;
        while (true) {
            NodeInfo prev = (NodeInfo)preceding.next();
            if (prev == null) {
                break;
            }

            if (controller != null) {
                int memo = controller.getRememberedNumber(prev);
                if (memo > 0) {
                    memo += i;
                    controller.setRememberedNumber(node, memo);
                    return memo;
                }
            }

            i++;
        }

        if (controller != null) {
            controller.setRememberedNumber(node, i);
        }
        return i;
    }

    /**
     * Get node number (level="single"). If the current node matches the supplied pattern, the returned
     * number is one plus the number of previous siblings that match the pattern. Otherwise,
     * return the element number of the nearest ancestor that matches the supplied pattern.
     *
     * @param node    the current node, the one whose node number is required
     * @param count   Pattern that identifies which nodes should be
     *                counted. Default (null) is the element name if the current node is
     *                an element, or "node()" otherwise.
     * @param from    Pattern that specifies where counting starts from.
     *                Default (null) is the root node. (This parameter does not seem
     *                useful but is included for the sake of XSLT conformance.)
     * @param context the dynamic context of the transformation, used if
     *                the patterns reference context values (e.g. variables)
     * @return the node number established as follows: go to the nearest
     *         ancestor-or-self that matches the 'count' pattern and that is a
     *         descendant of the nearest ancestor that matches the 'from' pattern.
     *         Return one plus the nunber of preceding siblings of that ancestor
     *         that match the 'count' pattern. If there is no such ancestor,
     *         return 0.
     * @throws XPathException when any error occurs in processing
     */

    public static int getNumberSingle(NodeInfo node, Pattern count,
                                      Pattern from, XPathContext context) throws XPathException {

        if (count == null && from == null) {
            return getNumberSimple(node, context);
        }

        boolean knownToMatch = false;
        if (count == null) {
            if (node.getFingerprint() == -1) {	// unnamed node
                count = new NodeTestPattern(NodeKindTest.makeNodeKindTest(node.getNodeKind()));
            } else {
                count = new NodeTestPattern(new NameTest(node));
            }
            knownToMatch = true;
        }

        NodeInfo target = node;
        while (!(knownToMatch || count.matches(target, context))) {
            target = target.getParent();
            if (target == null) {
                return 0;
            }
            if (from != null && from.matches(target, context)) {
                return 0;
            }
        }

        // we've found the ancestor to count from

        SequenceIterator preceding =
                target.iterateAxis(Axis.PRECEDING_SIBLING, count.getNodeTest());
        // pass the filter condition down to the axis enumeration where possible
        boolean alreadyChecked = (count instanceof NodeTestPattern);
        int i = 1;
        while (true) {
            NodeInfo p = (NodeInfo)preceding.next();
            if (p == null) {
                return i;
            }
            if (alreadyChecked || count.matches(p, context)) {
                i++;
            }
        }
    }

    /**
     * Get node number (level="any").
     * Return one plus the number of previous nodes in the
     * document that match the supplied pattern
     *
     * @param inst                   Identifies the xsl:number expression; this is relevant
     *                               when the function is memoised to support repeated use of the same
     *                               instruction to number multiple nodes
     * @param node                   The node being numbered
     * @param count                  Pattern that identifies which nodes should be
     *                               counted. Default (null) is the element name if the current node is
     *                               an element, or "node()" otherwise.
     * @param from                   Pattern that specifies where counting starts from.
     *                               Default (null) is the root node. Only nodes at or after the first (most
     *                               recent) node that matches the 'from' pattern are counted.
     * @param context                The dynamic context for the transformation
     * @param hasVariablesInPatterns if the count or from patterns
     *                               contain variables, then it's not safe to get the answer by adding
     *                               one to the number of the most recent node that matches
     * @return one plus the number of nodes that precede the current node,
     *         that match the count pattern, and that follow the first node that
     *         matches the from pattern if specified.
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *
     */

    public static int getNumberAny(Expression inst, NodeInfo node, Pattern count,
                                   Pattern from, XPathContext context, boolean hasVariablesInPatterns) throws XPathException {

        NodeInfo memoNode = null;
        int memoNumber = 0;
        Controller controller = context.getController();
        boolean memoise = (!hasVariablesInPatterns) && from==null;
        if (memoise) {
            Object[] memo = (Object[])controller.getUserData(inst, "xsl:number");
            if (memo != null) {
                memoNode = (NodeInfo)memo[0];
                memoNumber = ((Integer)memo[1]).intValue();
            }
        }

        int num = 0;
        if (count == null) {
            if (node.getFingerprint() == -1) {	// unnamed node
                count = new NodeTestPattern(NodeKindTest.makeNodeKindTest(node.getNodeKind()));
            } else {
                count = new NodeTestPattern(new NameTest(node));
            }
            num = 1;
        } else if (count.matches(node, context)) {
            num = 1;
        }

        // We use a special axis invented for the purpose: the union of the preceding and
        // ancestor axes, but in reverse document order

        // Pass part of the filtering down to the axis iterator if possible
        NodeTest filter;
        if (from == null) {
            filter = count.getNodeTest();
        } else if (from.getNodeKind() == Type.ELEMENT && count.getNodeKind() == Type.ELEMENT) {
            filter = NodeKindTest.ELEMENT;
        } else {
            filter = AnyNodeTest.getInstance();
        }

        if (from != null && from.matches(node, context)) {
            return num;
        }

        SequenceIterator preceding =
                node.iterateAxis(Axis.PRECEDING_OR_ANCESTOR, filter);
       
        while (true) {
            NodeInfo prev = (NodeInfo)preceding.next();
            if (prev == null) {
                break;
            }

            if (count.matches(prev, context)) {
                if (num == 1 && memoNode != null && prev.isSameNodeInfo(memoNode)) {
                    num = memoNumber + 1;
                    break;
                }
                num++;
            }

            if (from != null && from.matches(prev, context)) {
                break;
            }
        }

        if (memoise) {
            Object[] memo = new Object[2];
            memo[0] = node;
            memo[1] = Integer.valueOf(num);
            controller.setUserData(inst, "xsl:number", memo);
        }
        return num;
    }

    /**
     * Get node number (level="multiple").
     * Return a vector giving the hierarchic position of this node. See the XSLT spec for details.
     *
     * @param node    The node to be numbered
     * @param count   Pattern that identifies which nodes (ancestors and
     *                their previous siblings) should be counted. Default (null) is the
     *                element name if the current node is an element, or "node()"
     *                otherwise.
     * @param from    Pattern that specifies where counting starts from.
     *                Default (null) is the root node. Only nodes below the first (most
     *                recent) node that matches the 'from' pattern are counted.
     * @param context The dynamic context for the transformation
     * @return a vector containing for each ancestor-or-self that matches the
     *         count pattern and that is below the nearest node that matches the
     *         from pattern, an Integer which is one greater than the number of
     *         previous siblings that match the count pattern.
     * @throws XPathException
     */

    public static List getNumberMulti(NodeInfo node, Pattern count,
                                      Pattern from, XPathContext context) throws XPathException {

        //checkNumberable(node);

        ArrayList v = new ArrayList(5);

        if (count == null) {
            if (node.getFingerprint() == -1) {    // unnamed node
                count = new NodeTestPattern(NodeKindTest.makeNodeKindTest(node.getNodeKind()));
            } else {
                count = new NodeTestPattern(new NameTest(node));
            }
        }

        NodeInfo curr = node;

        while (true) {
            if (count.matches(curr, context)) {
                int num = getNumberSingle(curr, count, null, context);
                v.add(0, new Long(num));
            }
            curr = curr.getParent();
            if (curr == null) {
                break;
            }
            if (from != null && from.matches(curr, context)) {
                break;
            }
        }

        return v;
    }

    /**
     * Generic (model-independent) implementation of deep copy algorithm for nodes.
     * This is available for use by any node implementations that choose to use it.
     *
     * @param node            The node to be copied
     * @param out             The receiver to which events will be sent
     * @param namePool        Namepool holding the name codes (used only to resolve namespace
     *                        codes)
     * @param copyOptions     Options for copying namespaces, type annotations, etc,
     *                        as defined in {@link client.net.sf.saxon.ce.om.CopyOptions}
     * @throws XPathException on any failure reported by the Receiver
     */

    public static void copy(NodeInfo node,
                            Receiver out,
                            NamePool namePool,
                            int copyOptions
    ) throws XPathException {

        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                {
                    out.startDocument();
                    AxisIterator children0 = node.iterateAxis(Axis.CHILD, AnyNodeTest.getInstance());
                    while (true) {
                        NodeInfo child = (NodeInfo)children0.next();
                        if (child == null) {
                            break;
                        }
                        child.copy(out, copyOptions);
                    }
                    out.endDocument();
                    break;
                }
            case Type.ELEMENT:
                {
                    out.startElement(node.getNameCode(), 0);

                    // output the namespaces

                    if ((copyOptions & CopyOptions.LOCAL_NAMESPACES) != 0) {
                        NamespaceBinding[] localNamespaces = node.getDeclaredNamespaces(null);
                        for (int i=0; i<localNamespaces.length; i++) {
                            NamespaceBinding ns = localNamespaces[i];
                            if (ns == null) {
                                break;
                            }
                            out.namespace(ns, 0);
                        }
                    } else if ((copyOptions & CopyOptions.ALL_NAMESPACES) != 0) {
                        NamespaceIterator.sendNamespaces(node, out);
                    }

                    // output the attributes

                    AxisIterator attributes = node.iterateAxis(Axis.ATTRIBUTE, AnyNodeTest.getInstance());
                    while (true) {
                        NodeInfo att = (NodeInfo)attributes.next();
                        if (att == null) {
                            break;
                        }
                        att.copy(out, copyOptions);
                    }

                    // notify the start of content

                    out.startContent();

                    // output the children

                    AxisIterator children = node.iterateAxis(Axis.CHILD, AnyNodeTest.getInstance());
                    while (true) {
                        NodeInfo child = (NodeInfo)children.next();
                        if (child == null) {
                            break;
                        }
                        child.copy(out, copyOptions);
                    }

                    // finally the end tag

                    out.endElement();
                    return;
                }
            case Type.ATTRIBUTE:
                {
                    out.attribute(node.getNameCode(), node.getStringValueCS());
                    return;
                }
            case Type.TEXT:
                {
                    CharSequence value = node.getStringValueCS();
                    if (value.length() != 0) {
                        // zero-length text nodes can arise from external model wrappers
                        out.characters(value);
                    }
                    return;
                }
            case Type.COMMENT:
                {
                    out.comment(node.getStringValueCS());
                    return;
                }
            case Type.PROCESSING_INSTRUCTION:
                {
                    out.processingInstruction(node.getLocalPart(), node.getStringValueCS());
                    return;
                }
            case Type.NAMESPACE:
                {
                    out.namespace(new NamespaceBinding(node.getLocalPart(), node.getStringValue()), 0);
                    return;
                }
            default:

        }
    }

    /**
     * Generic (model-independent) method to determine the relative position of two
     * node in document order. The nodes must be in the same tree.
     *
     * @param first  The first node
     * @param second The second node, whose position is to be compared with the first node
     * @return -1 if this node precedes the other node, +1 if it follows the other
     *         node, or 0 if they are the same node. (In this case, isSameNode() will always
     *         return true, and the two nodes will produce the same result for generateId())
     */

    public static int compareOrder(SiblingCountingNode first, SiblingCountingNode second) {

        // are they the same node?
        if (first.isSameNodeInfo(second)) {
            return 0;
        }

        NodeInfo firstParent = first.getParent();
        if (firstParent == null) {
            // first node is the root
            return -1;
        }

        NodeInfo secondParent = second.getParent();
        if (secondParent == null) {
            // second node is the root
            return +1;
        }

        // do they have the same parent (common case)?
        if (firstParent.isSameNodeInfo(secondParent)) {
            int cat1 = nodeCategories[first.getNodeKind()];
            int cat2 = nodeCategories[second.getNodeKind()];
            if (cat1 == cat2) {
                return first.getSiblingPosition() - second.getSiblingPosition();
            } else {
                return cat1 - cat2;
            }
        }

        // find the depths of both nodes in the tree
        int depth1 = 0;
        int depth2 = 0;
        NodeInfo p1 = first;
        NodeInfo p2 = second;
        while (p1 != null) {
            depth1++;
            p1 = p1.getParent();
        }
        while (p2 != null) {
            depth2++;
            p2 = p2.getParent();
        }
        // move up one branch of the tree so we have two nodes on the same level

        p1 = first;
        while (depth1 > depth2) {
            p1 = p1.getParent();
            if (p1.isSameNodeInfo(second)) {
                return +1;
            }
            depth1--;
        }

        p2 = second;
        while (depth2 > depth1) {
            p2 = p2.getParent();
            if (p2.isSameNodeInfo(first)) {
                return -1;
            }
            depth2--;
        }

        // now move up both branches in sync until we find a common parent
        while (true) {
            NodeInfo par1 = p1.getParent();
            NodeInfo par2 = p2.getParent();
            if (par1 == null || par2 == null) {
                throw new NullPointerException("Node order comparison - internal error");
            }
            if (par1.isSameNodeInfo(par2)) {
                if (p1.getNodeKind() == Type.ATTRIBUTE && p2.getNodeKind() != Type.ATTRIBUTE) {
                    return -1;  // attributes first
                }
                if (p1.getNodeKind() != Type.ATTRIBUTE && p2.getNodeKind() == Type.ATTRIBUTE) {
                    return +1;  // attributes first
                }
                return ((SiblingCountingNode)p1).getSiblingPosition() -
                        ((SiblingCountingNode)p2).getSiblingPosition();
            }
            p1 = par1;
            p2 = par2;
        }
    }

    /**
     * Classify node kinds into categories for sorting into document order:
     * 0 = document, 1 = namespace, 2 = attribute, 3 = (element, text, comment, pi)
     */

    private static int[] nodeCategories = {
        -1, //0 = not used
        3, //1 = element
        2, //2 = attribute
        3, //3 = text
        -1, -1, -1, //4,5,6 = not used
        3, //7 = processing-instruction
        3, //8 = comment
        0, //9 = document
        -1, -1, -1, //10,11,12 = not used
        1   //13 = namespace
    };

    /**
     * Get a character string that uniquely identifies this node and that collates nodes
     * into document order
     * @param node the node whose unique identifier is reuqired
     * @param sb a buffer to which the unique identifier will be appended
     * @param addDocNr true if a unique document number is to be included in the information
     */

    public static void appendSequentialKey(SiblingCountingNode node, FastStringBuffer sb, boolean addDocNr) {
        if (addDocNr) {
            sb.append('w');
            sb.append(Long.toString(node.getDocumentNumber()));
        }
        if (node.getNodeKind() != Type.DOCUMENT) {
            NodeInfo parent = node.getParent();
            if (parent != null) {
                appendSequentialKey(((SiblingCountingNode)parent), sb, false);
            }
        }
        sb.append(alphaKey(node.getSiblingPosition()));
        switch (node.getNodeKind()) {
            case Type.ATTRIBUTE:
                sb.append('A');
                break;
            case Type.NAMESPACE:
                sb.append('N');
                break;
            case Type.TEXT:
                sb.append('T');
                break;
            case Type.COMMENT:
                sb.append('C');
                break;
            case Type.PROCESSING_INSTRUCTION:
                sb.append('P');
                break;
        }
    }


    /**
     * Construct an alphabetic key from an positive integer; the key collates in the same sequence
     * as the integer
     *
     * @param value The positive integer key value (negative values are treated as zero).
     * @return the alphabetic key value
     */

    public static String alphaKey(int value) {
        if (value < 1) {
            return "a";
        }
        if (value < 10) {
            return "b" + value;
        }
        if (value < 100) {
            return "c" + value;
        }
        if (value < 1000) {
            return "d" + value;
        }
        if (value < 10000) {
            return "e" + value;
        }
        if (value < 100000) {
            return "f" + value;
        }
        if (value < 1000000) {
            return "g" + value;
        }
        if (value < 10000000) {
            return "h" + value;
        }
        if (value < 100000000) {
            return "i" + value;
        }
        if (value < 1000000000) {
            return "j" + value;
        }
        return "k" + value;
    }

    /**
     * Test if one node is an ancestor-or-self of another
     *
     * @param a the putative ancestor-or-self node
     * @param d the putative descendant node
     * @return true if a is an ancestor-or-self of d
     */

    public static boolean isAncestorOrSelf(NodeInfo a, NodeInfo d) {
        // Generic implementation
        NodeInfo p = d;
        while (p != null) {
            if (a.isSameNodeInfo(p)) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }


    ///////////////////////////////////////////////////////////////////////////////
    // Helper classes to support axis iteration
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Create an iterator over a singleton node, if it exists and matches a nodetest;
     * otherwise return an empty iterator
     * @param node the singleton node, or null if the node does not exist
     * @param nodeTest the test to be applied
     * @return an iterator over the node if it exists and matches the test.
     */

    public static AxisIterator filteredSingleton(NodeInfo node, NodeTest nodeTest) {
        if (node != null && nodeTest.matches(node)) {
            return SingleNodeIterator.makeIterator(node);
        } else {
            return EmptyIterator.getInstance();
        }
    }

    /**
     * AxisFilter is an iterator that applies a NodeTest filter to
     * the nodes returned by an underlying AxisIterator.
     */

    public static class AxisFilter extends AxisIteratorImpl {
        private AxisIterator base;
        private NodeTest nodeTest;

        /**
         * Construct a AxisFilter
         *
         * @param base the underlying iterator that returns all the nodes on
         *             a required axis. This must not be an atomizing iterator!
         * @param test a NodeTest that is applied to each node returned by the
         *             underlying AxisIterator; only those nodes that pass the NodeTest are
         *             returned by the AxisFilter
         */

        public AxisFilter(AxisIterator base, NodeTest test) {
            this.base = base;
            nodeTest = test;
            position = 0;
        }

        public Item next() {
            while (true) {
                current = (NodeInfo)base.next();
                if (current == null) {
                    position = -1;
                    return null;
                }
                if (nodeTest.matches(current)) {
                    position++;
                    return current;
                }
            }
        }

        public SequenceIterator getAnother() {
            return new AxisFilter((AxisIterator)base.getAnother(), nodeTest);
        }
    }

    /**
     * EmptyTextFilter is an iterator that applies removes any zero-length text
     * nodes returned by an underlying AxisIterator.
     */

    public static class EmptyTextFilter extends AxisIteratorImpl {
        private AxisIterator base;

        /**
         * Construct an EmptyTextFilter
         *
         * @param base the underlying iterator that returns all the nodes on
         *             a required axis. This must not be an atomizing iterator
         */

        public EmptyTextFilter(AxisIterator base) {
            this.base = base;
            position = 0;
        }

        public Item next() {
            while (true) {
                current = (NodeInfo)base.next();
                if (current == null) {
                    position = -1;
                    return null;
                }
                if (!(current.getNodeKind() == Type.TEXT && current.getStringValueCS().length() == 0)) {
                    position++;
                    return current;
                }
            }
        }

        public SequenceIterator getAnother() {
            return new EmptyTextFilter((AxisIterator)base.getAnother());
        }
    }


    /**
     * BaseEnumeration is an abstract implementation of an AxisIterator, it
     * simplifies the implementation of the underlying AxisIterator by requiring
     * it to provide only two methods: advance(), and getAnother().
     *
     * <p>BaseEnumeration takes responsibility for incrementing position
     * when next() is called. The advance() method in a subclass should therefore
     * not modify position.</p>
     */

    public static abstract class BaseEnumeration extends AxisIteratorImpl {

        public final Item next() {
            advance();
            if (current == null) {
                position = -1;
            } else {
                position++;
            }
            return current;
        }

        /**
         * The advance() method must be provided in each concrete implementation.
         * It must leave the variable current set to the next node to be returned in the
         * iteration, or to null if there are no more nodes to be returned.
         */

        public abstract void advance();

        public abstract SequenceIterator getAnother();

    }

    /**
     * General-purpose implementation of the ancestor and ancestor-or-self axes
     */

    public static final class AncestorEnumeration extends BaseEnumeration {

        private boolean includeSelf;
        private boolean atStart;
        private NodeInfo start;

        /**
         * Create an iterator over the ancestor or ancestor-or-self axis
         * @param start the initial context node
         * @param includeSelf true if the "self" node is to be included
         */

        public AncestorEnumeration(NodeInfo start, boolean includeSelf) {
            this.start = start;
            this.includeSelf = includeSelf;
            current = start;
            atStart = true;
        }

        public void advance() {
            if (atStart) {
                atStart = false;
                if (includeSelf) {
                    return;
                }
            }
            current = (current == null ? null : current.getParent());
        }

        public SequenceIterator getAnother() {
            return new AncestorEnumeration(start, includeSelf);
        }

    } // end of class AncestorEnumeration

    /**
     * General-purpose implementation of the descendant and descendant-or-self axes,
     * in terms of the child axis.
     * But it also has the option to return the descendants in reverse document order;
     * this is used when evaluating the preceding axis. Note that the includeSelf option
     * should not be used when scanning in reverse order, as the self node will always be
     * returned first.
     */

    public static final class DescendantEnumeration extends BaseEnumeration {

        private AxisIterator children = null;
        private AxisIterator descendants = null;
        private NodeInfo start;
        private boolean includeSelf;
        private boolean forwards;
        private boolean atEnd = false;

        /**
         * Create an iterator over the descendant or descendant-or-self axis
         * @param start the initial context node
         * @param includeSelf true if the "self" node is to be included
         * @param forwards true for a forwards iteration, false for reverse order
         */

        public DescendantEnumeration(NodeInfo start,
                                     boolean includeSelf, boolean forwards) {
            this.start = start;
            this.includeSelf = includeSelf;
            this.forwards = forwards;
        }

        public void advance() {
            if (descendants != null) {
                NodeInfo nextd = (NodeInfo)descendants.next();
                if (nextd != null) {
                    current = nextd;
                    return;
                } else {
                    descendants = null;
                }
            }
            if (children != null) {
                NodeInfo n = (NodeInfo)children.next();
                if (n != null) {
                    if (n.hasChildNodes()) {
                        if (forwards) {
                            descendants = new DescendantEnumeration(n, false, forwards);
                            current = n;
                        } else {
                            descendants = new DescendantEnumeration(n, true, forwards);
                            advance();
                        }
                    } else {
                        current = n;
                    }
                } else {
                    if (forwards || !includeSelf) {
                        current = null;
                    } else {
                        atEnd = true;
                        children = null;
                        current = start;
                    }
                }
            } else if (atEnd) {
                // we're just finishing a backwards scan
                current = null;
            } else {
                // we're just starting...
                if (start.hasChildNodes()) {
                    //children = new XMLNodeWrapper.ChildEnumeration(start, true, forwards);
                    children = start.iterateAxis(Axis.CHILD);
                    if (!forwards) {
                        try {
                            List list = new ArrayList(20);
                            SequenceIterator forwards = start.iterateAxis(Axis.CHILD);
                            while (true) {
                                Item n = forwards.next();
                                if (n == null) {
                                    break;
                                }
                                list.add(n);
                            }
                            NodeInfo[] nodes = new NodeInfo[list.size()];
                            nodes = (NodeInfo[])list.toArray(nodes);
                            children = new ReverseNodeArrayIterator(nodes, 0, nodes.length);
                        } catch (XPathException e) {
                            throw new AssertionError("Internal error in Navigator#descendantEnumeration: " + e.getMessage());
                            // shouldn't happen.
                        }
                    }
                } else {
                    children = EmptyIterator.getInstance();
                }
                if (forwards && includeSelf) {
                    current = start;
                } else {
                    advance();
                }
            }
        }

        public SequenceIterator getAnother() {
            return new DescendantEnumeration(start, includeSelf, forwards);
        }

    } // end of class DescendantEnumeration

    /**
     * General purpose implementation of the following axis, in terms of the
     * ancestor, child, and following-sibling axes
     */

    public static final class FollowingEnumeration extends BaseEnumeration {
        private NodeInfo start;
        private AxisIterator ancestorEnum = null;
        private AxisIterator siblingEnum = null;
        private AxisIterator descendEnum = null;

        /**
         * Create an iterator over the "following" axis
         * @param start the initial context node
         */

        public FollowingEnumeration(NodeInfo start) {
            this.start = start;
            ancestorEnum = new AncestorEnumeration(start, false);
            switch (start.getNodeKind()) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    //siblingEnum = new XMLNodeWrapper.ChildEnumeration(start, false, true);
                    // gets following siblings
                    siblingEnum = start.iterateAxis(Axis.FOLLOWING_SIBLING);
                    break;
                case Type.ATTRIBUTE:
                case Type.NAMESPACE:
                    //siblingEnum = new XMLNodeWrapper.ChildEnumeration((XMLNodeWrapper)start.getParent(), true, true);
                    // gets children of the attribute's parent node
                    NodeInfo parent = start.getParent();
                    if (parent == null) {
                        siblingEnum = EmptyIterator.getInstance();
                    } else {
                        siblingEnum = parent.iterateAxis(Axis.CHILD);
                    }
                    break;
                default:
                    siblingEnum = EmptyIterator.getInstance();
            }
            //advance();
        }

        public void advance() {
            if (descendEnum != null) {
                NodeInfo nextd = (NodeInfo)descendEnum.next();
                if (nextd != null) {
                    current = nextd;
                    return;
                } else {
                    descendEnum = null;
                }
            }
            if (siblingEnum != null) {
                NodeInfo nexts = (NodeInfo)siblingEnum.next();
                if (nexts != null) {
                    current = nexts;
                    NodeInfo n = current;
                    if (n.hasChildNodes()) {
                        descendEnum = new DescendantEnumeration(n, false, true);
                    } else {
                        descendEnum = null;
                    }
                    return;
                } else {
                    descendEnum = null;
                    siblingEnum = null;
                }
            }
            NodeInfo nexta = (NodeInfo)ancestorEnum.next();
            if (nexta != null) {
                current = nexta;
                NodeInfo n = current;
                if (n.getNodeKind() == Type.DOCUMENT) {
                    siblingEnum = EmptyIterator.getInstance();
                } else {
                    //siblingEnum = new XMLNodeWrapper.ChildEnumeration(next, false, true);
                    siblingEnum = n.iterateAxis(Axis.FOLLOWING_SIBLING);
                }
                advance();
            } else {
                current = null;
            }
        }

        public SequenceIterator getAnother() {
            return new FollowingEnumeration(start);
        }

    } // end of class FollowingEnumeration

    /**
     * Helper method to iterate over the preceding axis, or Saxon's internal
     * preceding-or-ancestor axis, by making use of the ancestor, descendant, and
     * preceding-sibling axes.
     */

    public static final class PrecedingEnumeration extends BaseEnumeration {

        private NodeInfo start;
        private AxisIterator ancestorEnum = null;
        private AxisIterator siblingEnum = null;
        private AxisIterator descendEnum = null;
        private boolean includeAncestors;

        /**
         * Create an iterator for the preceding or "preceding-or-ancestor" axis (the latter being
         * used internall to support xsl:number)
         * @param start the initial context node
         * @param includeAncestors true if ancestors of the initial context node are to be included
         * in the result
         */

        public PrecedingEnumeration(NodeInfo start, boolean includeAncestors) {
            this.start = start;
            this.includeAncestors = includeAncestors;
            ancestorEnum = new AncestorEnumeration(start, false);
            switch (start.getNodeKind()) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    // get preceding-sibling enumeration
                    siblingEnum = start.iterateAxis(Axis.PRECEDING_SIBLING);
                    break;
                default:
                    siblingEnum = EmptyIterator.getInstance();
            }
        }

        public void advance() {
            if (descendEnum != null) {
                NodeInfo nextd = (NodeInfo)descendEnum.next();
                if (nextd != null) {
                    current = nextd;
                    return;
                } else {
                    descendEnum = null;
                }
            }
            if (siblingEnum != null) {
                NodeInfo nexts = (NodeInfo)siblingEnum.next();
                if (nexts != null) {
                    if (nexts.hasChildNodes()) {
                        descendEnum = new DescendantEnumeration(nexts, true, false);
                        advance();
                    } else {
                        descendEnum = null;
                        current = nexts;
                    }
                    return;
                } else {
                    descendEnum = null;
                    siblingEnum = null;
                }
            }
            NodeInfo nexta = (NodeInfo)ancestorEnum.next();
            if (nexta != null) {
                current = nexta;
                NodeInfo n = current;
                if (n.getNodeKind() == Type.DOCUMENT) {
                    siblingEnum = EmptyIterator.getInstance();
                } else {
                    siblingEnum = n.iterateAxis(Axis.PRECEDING_SIBLING);
                }
                if (!includeAncestors) {
                    advance();
                }
            } else {
                current = null;
            }
        }

        public SequenceIterator getAnother() {
            return new PrecedingEnumeration(start, includeAncestors);
        }

    } // end of class PrecedingEnumeration


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
