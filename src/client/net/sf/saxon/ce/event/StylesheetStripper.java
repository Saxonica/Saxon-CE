package client.net.sf.saxon.ce.event;

import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.Arrays;


/**
  * The Stripper class performs whitespace stripping according to the rules of
  * the xsl:strip-space and xsl:preserve-space instructions.
  * It maintains details of which elements need to be stripped.
  * The code is written to act as a SAX-like filter to do the stripping.
  * @author Michael H. Kay
  */


public class StylesheetStripper extends ProxyReceiver {

    // stripStack is used to hold information used while stripping nodes. We avoid allocating
    // space on the tree itself to keep the size of nodes down. Each entry on the stack is two
    // booleans, one indicates the current value of xml-space is "preserve", the other indicates
    // that we are in a space-preserving element.

    // We implement our own stack to avoid the overhead of allocating objects. The two booleans
    // are held as the ls bits of a byte.

    private byte[] stripStack = new byte[100];
    private int top = 0;


    public static final byte ALWAYS_PRESERVE = 0x01;    // whitespace always preserved (e.g. xsl:text)
    public static final byte ALWAYS_STRIP = 0x02;       // whitespace always stripped (e.g. xsl:choose)
    public static final byte STRIP_DEFAULT = 0x00;      // no special action
    public static final byte PRESERVE_PARENT = 0x04;    // parent element specifies xml:space="preserve"
    public static final byte CANNOT_STRIP = 0x08;       // type annotation indicates simple typed content

    //    Any child of one of the following elements is removed from the tree,
    //    regardless of any xml:space attributes. Note that this array must be in numeric
    //    order for binary chop to work correctly.

    private static final String[] specials = {
            "analyze-string", "apply-imports", "apply-templates",
            "attribute-set", "call-template", "character-map", "choose",
            "stylesheet", "transform" };


    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param elementName identifies the element being tested
    */

    public byte isSpacePreserving(StructuredQName elementName) {
        if (elementName.getNamespaceURI().equals(NamespaceConstant.XSLT)) {
            String local = elementName.getLocalName();
            if (local.equals("text")) {
                return ALWAYS_PRESERVE;
            }

            if (Arrays.binarySearch(specials, local) >= 0) {
                return ALWAYS_STRIP;
            }
        }
        return STRIP_DEFAULT;
    }


    /**
    * Callback interface for SAX: not for application use
    */

    public void open () throws XPathException {
        // System.err.println("Stripper#startDocument()");
        top = 0;
        stripStack[top] = ALWAYS_PRESERVE;             // {xml:preserve = false, preserve this element = true}
        super.open();
    }

    public void startElement(StructuredQName qName, int properties) throws XPathException
    {
    	// System.err.println("startElement " + nameCode);
        nextReceiver.startElement(qName, properties);

        byte preserveParent = stripStack[top];
        byte preserve = (byte)(preserveParent & PRESERVE_PARENT);

        byte elementStrip = isSpacePreserving(qName);
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

    public void attribute(StructuredQName nameCode, CharSequence value)
    throws XPathException {

        // test for xml:space="preserve" | "default"

        if (nameCode.equals(XML_SPACE)) {
            if (value.toString().equals("preserve")) {
                stripStack[top] |= PRESERVE_PARENT;
            } else {
                stripStack[top] &= ~PRESERVE_PARENT;
            }
        }
        nextReceiver.attribute(nameCode, value);
    }

    private final static StructuredQName XML_SPACE = new StructuredQName("xml", NamespaceConstant.XML, "space");

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

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
