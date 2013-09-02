package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AtomicValue;

/**
 * This class is used for generating complex content, that is, the content of an
 * element or document node. It enforces the rules on the order of events within
 * complex content (attributes and namespaces must come first), and it implements
 * part of the namespace fixup rules, in particular, it ensures that there is a
 * namespace node for the namespace used in the element name and in each attribute
 * name.
 *
 * <p>The same ComplexContentOutputter may be used for generating an entire XML
 * document; it is not necessary to create a new outputter for each element node.</p>
 *
 * @author Michael H. Kay
 */

public final class ComplexContentOutputter extends SequenceReceiver {

    private Receiver nextReceiver;
            // the next receiver in the output pipeline

    private int pendingStartTagDepth = -2;
            // -2 means we are at the top level, or immediately within a document node
            // -1 means we are in the content of an element node whose start tag is complete
    private StructuredQName pendingStartTag;
    private int level = -1; // records the number of startDocument or startElement events
                            // that have not yet been closed. Note that startDocument and startElement
                            // events may be arbitrarily nested; startDocument and endDocument
                            // are ignored unless they occur at the outermost level, except that they
                            // still change the level number
    private boolean[] currentLevelIsDocument = new boolean[20];
    private Boolean elementIsInNullNamespace;
    private StructuredQName[] pendingAttCode = new StructuredQName[20];
    private String[] pendingAttValue = new String[20];
    private int pendingAttListSize = 0;

    private NamespaceBinding[] pendingNSList = new NamespaceBinding[20];
    private int pendingNSListSize = 0;

    private int startElementProperties;
    private boolean declaresDefaultNamespace;
    private boolean started = false;

    /**
     * Create a ComplexContentOutputter
     */

    public ComplexContentOutputter() {
        //System.err.println("ComplexContentOutputter init");
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
     * Set the receiver (to handle the next stage in the pipeline) directly
     * @param receiver the receiver to handle the next stage in the pipeline
     */

    public void setReceiver(Receiver receiver) {
        this.nextReceiver = receiver;
    }

    /**
     * Test whether any content has been written to this ComplexContentOutputter
     * @return true if content has been written
     */

    public boolean contentHasBeenWritten() {
        return started;
    }

    /**
     * Start the output process
     */

    public void open() throws XPathException {
        nextReceiver.open();
        previousAtomic = false;
    }

    /**
     * Start of a document node.
    */

    public void startDocument() throws XPathException {
        level++;
        if (level == 0) {
            nextReceiver.startDocument();
        } else if (pendingStartTagDepth >= 0) {
            startContent();
            pendingStartTagDepth = -2;
        }
        previousAtomic = false;
        if (currentLevelIsDocument.length < level+1) {
            boolean[] b2 = new boolean[level*2];
            System.arraycopy(currentLevelIsDocument, 0, b2, 0, level);
            currentLevelIsDocument = b2;
        }
        currentLevelIsDocument[level] = true;
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (level == 0) {
            nextReceiver.endDocument();
        }
        previousAtomic = false;
        level--;
    }



    /**
    * Produce text content output. <BR>
    * Special characters are escaped using XML/HTML conventions if the output format
    * requires it.
    * @param s The String to be output
    * @exception XPathException for any failure
    */

    public void characters(CharSequence s) throws XPathException {
        previousAtomic = false;
        if (s==null) return;
        int len = s.length();
        if (len==0) return;
        if (pendingStartTagDepth >= 0) {
            startContent();
        }
        nextReceiver.characters(s);
    }

    /**
    * Output an element start tag. <br>
    * The actual output of the tag is deferred until all attributes have been output
    * using attribute().
     * @param qName The element name code
     */

    public void startElement(StructuredQName qName, int properties) throws XPathException {
        // System.err.println("StartElement " + nameCode);
        level++;
        started = true;
        if (pendingStartTagDepth >= 0) {
            startContent();
        }
        startElementProperties = properties;
        pendingAttListSize = 0;
        pendingNSListSize = 0;
        pendingStartTag = qName;
        pendingStartTagDepth = 1;
        elementIsInNullNamespace = null; // meaning not yet computed
        declaresDefaultNamespace = false;
        previousAtomic = false;
        if (currentLevelIsDocument.length < level+1) {
            boolean[] b2 = new boolean[level*2];
            System.arraycopy(currentLevelIsDocument, 0, b2, 0, level);
            currentLevelIsDocument = b2;
        }
        currentLevelIsDocument[level] = false;
    }


    /**
    * Output a namespace declaration. <br>
    * This is added to a list of pending namespaces for the current start tag.
    * If there is already another declaration of the same prefix, this one is
    * ignored, unless the REJECT_DUPLICATES flag is set, in which case this is an error.
    * Note that unlike SAX2 startPrefixMapping(), this call is made AFTER writing the start tag.
    * @param nsBinding The namespace code
    * @throws XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void namespace(NamespaceBinding nsBinding, int properties)
    throws XPathException {

        // System.err.println("Write namespace prefix=" + (nscode>>16) + " uri=" + (nscode&0xffff));
        if (pendingStartTagDepth < 0) {
            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.NAMESPACE,
                    nsBinding.getPrefix(),
                    pendingStartTagDepth == -2);
        }

        // elimination of namespaces already present on an outer element of the
        // result tree is done by the NamespaceReducer.

        // Handle declarations whose prefix is duplicated for this element.

        boolean rejectDuplicates = (properties & ReceiverOptions.REJECT_DUPLICATES) != 0;

        for (int i=0; i<pendingNSListSize; i++) {
            if (nsBinding.equals(pendingNSList[i])) {
                // same prefix and URI: ignore this duplicate
                return;
            }
            if (nsBinding.getPrefix().equals(pendingNSList[i].getPrefix())) {
                if (pendingNSList[i].equals(NamespaceBinding.DEFAULT_UNDECLARATION) || nsBinding.equals(NamespaceBinding.DEFAULT_UNDECLARATION)) {
                    // xmlns="" overridden by xmlns="abc"
                    pendingNSList[i] = nsBinding;
                } else if (rejectDuplicates) {
                    String prefix = nsBinding.getPrefix();
                    String uri1 =nsBinding.getURI();
                    String uri2 = pendingNSList[i].getURI();
                    XPathException err = new XPathException("Cannot create two namespace nodes with the same prefix mapped to different URIs (prefix=" +
                            (prefix.length() == 0 ? "\"\"" : prefix) + ", URI=" +
                            (uri1.length() == 0 ? "\"\"" : uri1) + ", URI=" +
                            (uri2.length() == 0 ? "\"\"" : uri2) + ")");
                    err.setErrorCode("XTDE0430");
                    throw err;
                } else {
        		    // same prefix, do a quick exit
        		    return;
        		}
        	}
        }

        // It is an error to output a namespace node for the default namespace if the element
        // itself is in the null namespace, as the resulting element could not be serialized

        if (((nsBinding.getPrefix().isEmpty())) && !nsBinding.getURI().isEmpty()) {
            declaresDefaultNamespace = true;
            if (elementIsInNullNamespace == null) {
                elementIsInNullNamespace = Boolean.valueOf(
                        pendingStartTag.getNamespaceURI().equals(NamespaceConstant.NULL));
            }
            if (elementIsInNullNamespace.booleanValue()) {
                XPathException err = new XPathException("Cannot output a namespace node for the default namespace when the element is in no namespace");
                err.setErrorCode("XTDE0440");
                throw err;
            }
        }

        // if it's not a duplicate namespace, add it to the list for this start tag

        if (pendingNSListSize+1 > pendingNSList.length) {
            NamespaceBinding[] newlist = new NamespaceBinding[pendingNSListSize * 2];
            System.arraycopy(pendingNSList, 0, newlist, 0, pendingNSListSize);
            pendingNSList = newlist;
        }
        pendingNSList[pendingNSListSize++] = nsBinding;
        previousAtomic = false;
    }


    /**
    * Output an attribute value. <br>
    * This is added to a list of pending attributes for the current start tag, overwriting
    * any previous attribute with the same name. <br>
    * This method should NOT be used to output namespace declarations.<br>
    *
     * @param nameCode The name of the attribute
     * @param value The value of the attribute
     * @throws XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void attribute(StructuredQName nameCode, CharSequence value) throws XPathException {
        //System.err.println("Write attribute " + nameCode + "=" + value + " to Outputter " + this);

        if (pendingStartTagDepth < 0) {
            // The complexity here is in identifying the right error message and error code

            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.ATTRIBUTE,
                    nameCode.getDisplayName(),
                    level<0 || currentLevelIsDocument[level]);
        }

        // if this is a duplicate attribute, overwrite the original (we're always in XSLT now...)

        for (int a=0; a<pendingAttListSize; a++) {
            if (pendingAttCode[a].equals(nameCode)) {
                pendingAttValue[a] = value.toString();
                    // we have to copy the CharSequence, because some kinds of CharSequence are mutable.
                return;
            }
        }

        // add this one to the list

        if (pendingAttListSize >= pendingAttCode.length) {
            StructuredQName[] attCode2 = new StructuredQName[pendingAttListSize*2];
            String[] attValue2 = new String[pendingAttListSize*2];
            System.arraycopy(pendingAttCode, 0, attCode2, 0, pendingAttListSize);
            System.arraycopy(pendingAttValue, 0, attValue2, 0, pendingAttListSize);
            pendingAttCode = attCode2;
            pendingAttValue = attValue2;
        }

        pendingAttCode[pendingAttListSize] = nameCode;
        pendingAttValue[pendingAttListSize] = value.toString();
        pendingAttListSize++;
        previousAtomic = false;
    }

	/**
	 * Check that the prefix for an element or attribute is acceptable, allocating a substitute
	 * prefix if not. The prefix is acceptable unless a namespace declaration has been
	 * written that assignes this prefix to a different namespace URI. This method
	 * also checks that the element or attribute namespace has been declared, and declares it
	 * if not.
     * @param nameCode the proposed name, including proposed prefix
     * @param seq sequence number, used for generating a substitute prefix when necessary
     * @return a nameCode to use in place of the proposed nameCode (or the original nameCode
     * if no change is needed)
	*/

	private StructuredQName checkProposedPrefix(StructuredQName nameCode, int seq) throws XPathException {
		String nsprefix = nameCode.getPrefix();
        String nsuri = nameCode.getNamespaceURI();

        for (int i=0; i<pendingNSListSize; i++) {
        	if (nsprefix.equals(pendingNSList[i].getPrefix())) {
        		// same prefix
        		if ((nsuri.equals(pendingNSList[i].getURI()))) {
        			// same URI
        			return nameCode;	// all is well
        		} else {
        			String prefix = getSubstitutePrefix(nsprefix, seq);

        			StructuredQName newCode = new StructuredQName(
        								prefix,
        								nsuri,
        								nameCode.getLocalName());
        			namespace(new NamespaceBinding(prefix, nsuri), 0);
        			return newCode;
        		}
        	}
        }
        // no declaration of this prefix: declare it now
        namespace(new NamespaceBinding(nsprefix, nsuri), 0);
        return nameCode;
    }

    /**
     * It is possible for a single output element to use the same prefix to refer to different
     * namespaces. In this case we have to generate an alternative prefix for uniqueness. The
     * one we generate is based on the sequential position of the element/attribute: this is
     * designed to ensure both uniqueness (with a high probability) and repeatability
     * @param prefix the proposed prefix
     * @param seq sequence number for use in the substitute prefix
     * @return a prefix to use in place of the one originally proposed
    */

    private String getSubstitutePrefix(String prefix, int seq) {
        return prefix + '_' + seq;
    }

    /**
    * Output an element end tag.
    */

    public void endElement() throws XPathException {
        //System.err.println("Write end tag " + this + " : " + name);
        if (pendingStartTagDepth >= 0) {
            startContent();
        } else {
            pendingStartTagDepth = -2;
        }

        // write the end tag

        nextReceiver.endElement();
        level--;
        previousAtomic = false;
    }

    /**
    * Write a comment
    */

    public void comment(CharSequence comment) throws XPathException {
        if (pendingStartTagDepth >= 0) {
            startContent();
        }
        nextReceiver.comment(comment);
        previousAtomic = false;
    }

    /**
    * Write a processing instruction
    */

    public void processingInstruction(String target, CharSequence data) throws XPathException {
        if (pendingStartTagDepth >= 0) {
            startContent();
        }
        nextReceiver.processingInstruction(target, data);
        previousAtomic = false;
    }

    /**
    * Append an arbitrary item (node or atomic value) to the output
     * @param item the item to be appended
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
 * need to be copied. Values are {@link client.net.sf.saxon.ce.om.NodeInfo#ALL_NAMESPACES},
 * {@link client.net.sf.saxon.ce.om.NodeInfo#LOCAL_NAMESPACES}, {@link client.net.sf.saxon.ce.om.NodeInfo#NO_NAMESPACES}
     */

    public void append(Item item, int copyNamespaces) throws XPathException {
        if (item == null) {
            //return;
        } else if (item instanceof AtomicValue) {
            if (previousAtomic) {
                characters(" ");
            }
            characters(item.getStringValue());
            previousAtomic = true;
        } else if (((NodeInfo)item).getNodeKind() == Type.DOCUMENT) {
            startDocument();
            SequenceIterator iter = ((NodeInfo)item).iterateAxis(Axis.CHILD);
            while (true) {
                Item it = iter.next();
                if (it == null) break;
                append(it, copyNamespaces);
            }
            endDocument();
            previousAtomic = false;
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


    /**
    * Close the output
    */

    public void close() throws XPathException {
        // System.err.println("Close " + this + " using emitter " + emitter.getClass());
        nextReceiver.close();
        previousAtomic = false;
    }

    /**
    * Flush out a pending start tag
    */

    public void startContent() throws XPathException {

        if (pendingStartTagDepth < 0) {
            // this can happen if the method is called from outside,
            // e.g. from a SequenceOutputter earlier in the pipeline
            return;
        }

        started = true;
        int props = startElementProperties;
        StructuredQName elcode = pendingStartTag;
        if (declaresDefaultNamespace || !elcode.getPrefix().equals("")) {
            // skip this check if the element is unprefixed and no xmlns="abc" declaration has been encountered
            elcode = checkProposedPrefix(pendingStartTag, 0);
            props = startElementProperties | ReceiverOptions.NAMESPACE_OK;
        }
        nextReceiver.startElement(elcode, props);

        for (int a=0; a<pendingAttListSize; a++) {
            StructuredQName attcode = pendingAttCode[a];
            if (!attcode.getPrefix().isEmpty()) {	// non-null prefix
                pendingAttCode[a] = checkProposedPrefix(attcode, a+1);
            }
        }

        for (int n=0; n<pendingNSListSize; n++) {
            nextReceiver.namespace(pendingNSList[n], 0);
        }

        for (int a=0; a<pendingAttListSize; a++) {
            nextReceiver.attribute( pendingAttCode[a],
                    pendingAttValue[a]
            );
        }

        nextReceiver.startContent();

        pendingAttListSize = 0;
        pendingNSListSize = 0;
        pendingStartTagDepth = -1;
        pendingStartTag = null;
        previousAtomic = false;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
