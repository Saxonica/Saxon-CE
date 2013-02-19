package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;


/**
  * The Stripper class performs whitespace stripping according to the rules of
  * the xsl:strip-space and xsl:preserve-space instructions.
  * It maintains details of which elements need to be stripped.
  * The code is written to act as a SAX-like filter to do the stripping.
  * @author Michael H. Kay
  */


public abstract class Stripper extends ProxyReceiver {

    // stripStack is used to hold information used while stripping nodes. We avoid allocating
    // space on the tree itself to keep the size of nodes down. Each entry on the stack is two
    // booleans, one indicates the current value of xml-space is "preserve", the other indicates
    // that we are in a space-preserving element.

    // We implement our own stack to avoid the overhead of allocating objects. The two booleans
    // are held as the ls bits of a byte.

    private byte[] stripStack = new byte[100];
    private int top = 0;
    public final static Template STRIP = new Template(){};
    public final static Template PRESERVE = new Template();

    /**
     * Get a clean copy of this stripper. The new copy shares the same PipelineConfiguration
     * as the original, but the underlying receiver (that is, the destination for post-stripping
     * events) is left uninitialized.
     */

    public abstract Stripper getAnother();

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param fingerprint Identifies the name of the element whose whitespace is to
     * be preserved
     * @return ALWAYS_PRESERVE if the element is in the set of white-space preserving
     *  element types, ALWAYS_STRIP if the element is to be stripped regardless of the
     * xml:space setting, and STRIP_DEFAULT otherwise
     * @throws XPathException if the rules are ambiguous and ambiguities are to be
     * reported as errors
    */

    public abstract byte isSpacePreserving(int fingerprint) throws XPathException;

    public static final byte ALWAYS_PRESERVE = 0x01;    // whitespace always preserved (e.g. xsl:text)
    public static final byte ALWAYS_STRIP = 0x02;       // whitespace always stripped (e.g. xsl:choose)
    public static final byte STRIP_DEFAULT = 0x00;      // no special action
    public static final byte PRESERVE_PARENT = 0x04;    // parent element specifies xml:space="preserve"
    public static final byte CANNOT_STRIP = 0x08;       // type annotation indicates simple typed content


    /**
    * Callback interface for SAX: not for application use
    */

    public void open () throws XPathException {
        // System.err.println("Stripper#startDocument()");
        top = 0;
        stripStack[top] = ALWAYS_PRESERVE;             // {xml:preserve = false, preserve this element = true}
        super.open();
    }

    public void startElement(int nameCode, int properties) throws XPathException
    {
    	// System.err.println("startElement " + nameCode);
        nextReceiver.startElement(nameCode, properties);

        byte preserveParent = stripStack[top];
        byte preserve = (byte)(preserveParent & PRESERVE_PARENT);

        byte elementStrip = isSpacePreserving(nameCode & NamePool.FP_MASK);
        if (elementStrip == ALWAYS_PRESERVE) {
            preserve |= ALWAYS_PRESERVE;
        } else if (elementStrip == ALWAYS_STRIP) {
            preserve |= ALWAYS_STRIP;
        }

        // put "preserve" value on top of stack

        top++;
        if (top >= stripStack.length) {
            byte[] newStack = new byte[top*2];
            System.arraycopy(stripStack, 0, newStack, 0, top);
            stripStack = newStack;
        }
        stripStack[top] = preserve;
    }

    public void attribute(int nameCode, CharSequence value)
    throws XPathException {

        // test for xml:space="preserve" | "default"

        if ((nameCode & 0xfffff) == StandardNames.XML_SPACE) {
            if (value.toString().equals("preserve")) {
                stripStack[top] |= PRESERVE_PARENT;
            } else {
                stripStack[top] &= ~PRESERVE_PARENT;
            }
        }
        nextReceiver.attribute(nameCode, value);
    }

    /**
    * Handle an end-of-element event
    */

    public void endElement () throws XPathException
    {
        nextReceiver.endElement();
        top--;
    }

    /**
    * Handle a text node
    */

    public void characters(CharSequence chars) throws XPathException
    {
        // assume adjacent chunks of text are already concatenated

        if (((((stripStack[top] & (ALWAYS_PRESERVE | PRESERVE_PARENT | CANNOT_STRIP)) != 0) &&
                (stripStack[top] & ALWAYS_STRIP) == 0)
                || !Whitespace.isWhite(chars))
                && chars.length() > 0) {
            nextReceiver.characters(chars);
        }
    }

    /**
     * Ask whether this Receiver (or the downstream pipeline) makes any use of the type annotations
     * supplied on element and attribute events
     * @return true if the Receiver makes any use of this information. If false, the caller
     *         may supply untyped nodes instead of supplying the type annotation
     */

    public boolean usesTypeAnnotations() {
        return true;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
