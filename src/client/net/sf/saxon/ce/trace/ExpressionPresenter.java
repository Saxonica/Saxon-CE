package client.net.sf.saxon.ce.trace;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.om.NamePool;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.TypeHierarchy;

/**
 * This class handles the display of an abstract expression tree in an XML format
 * with some slight resemblence to XQueryX
 */
public class ExpressionPresenter {

    private Configuration config;
    /*@Nullable*/ private Receiver receiver;
    int depth = 0;
    boolean inStartTag = false;

    /**
     * Make an ExpressionPresenter that writes indented output to the standard error output
     * destination of the Configuration
     * @param config the Saxon configuration
     */

    public ExpressionPresenter(Configuration config) {
        //this(config, config.getStandardErrorOutput());
    }

    /**
     * Make an ExpressionPresenter that writes indented output to a specified output stream
     * @param config the Saxon configuration
     * @param out the output stream
     */

    public ExpressionPresenter(Configuration config, String out) { // - was: , OutputStream out) {
        Object props = makeDefaultProperties();
//        try {
//            receiver = config.getSerializerFactory().getReceiver(
//                            new StreamResult(out),
//                            config.makePipelineConfiguration(),
//                            props);
//        } catch (XPathException err) {
//            err.printStackTrace();
//            throw new InternalError(err.getMessage());
//        }
//        this.config = config;
//        try {
//            receiver.open();
//            receiver.startDocument(0);
//        } catch (XPathException err) {
//            err.printStackTrace();
//            throw new InternalError(err.getMessage());
//        }
    }

    /**
     * Make an ExpressionPresenter for a given Configuration using a user-supplied Receiver
     * to accept the output
     * @param config the Configuration
     * @param receiver the user-supplied Receiver
     */

    public ExpressionPresenter(Configuration config, /*@NotNull*/ Receiver receiver) {
        this.config = config;
        this.receiver = receiver;
//        try {
//            receiver.open();
//            receiver.startDocument(0);
//        } catch (XPathException err) {
//            err.printStackTrace();
//            throw new InternalError(err.getMessage());
//        }
    }

    /**
     * Make a receiver, using default output properties, with serialized output going
     * to a specified OutputStream
     * @param config the Configuration
     * @param out the OutputStream
     * @return a Receiver that directs serialized output to this output stream
     * @throws XPathException
     */

    /*@Nullable*/ public static Receiver defaultDestination(/*@NotNull*/ Configuration config, String out) throws XPathException {
        Object props = makeDefaultProperties();
        return null;
//        return config.getSerializerFactory().getReceiver(
//                        new StreamResult(out),
//                        config.makePipelineConfiguration(),
//                        props);
    }


    /**
     * Make a Properties object containing defaulted serialization attributes for the expression tree
     * @return a default set of properties
     */

    /*@NotNull*/ public static Object makeDefaultProperties() {
        Object props = new Object(); // was java.util.Properties
//        props.setProperty(OutputKeys.METHOD, "xml");
//        props.setProperty(OutputKeys.INDENT, "yes");
//        props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        //props.setProperty(SaxonOutputKeys.INDENT_SPACES, "2");
        return props;
    }

    /**
     * Start an element
     * @param name the name of the element
     * @return the depth of the tree before this element: for diagnostics, this can be compared
     * with the value returned by endElement
     */

    public int startElement(String name) {
    	 return -1;
//        try {
//            if (inStartTag) {
//                receiver.startContent();
//                inStartTag = false;
//            }
//            receiver.startElement(new NoNamespaceName(name), Untyped.getInstance(), 0, 0);
//        } catch (XPathException err) {
//            err.printStackTrace();
//            throw new InternalError(err.getMessage());
//        }
//        inStartTag = true;
//        return depth++;
    }

    /**
     * Output an attribute node
     * @param name the name of the attribute
     * @param value the value of the attribute
     */

    public void emitAttribute(String name, String value) {
//        try {
//            receiver.attribute(new NoNamespaceName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, 0, 0);
//        } catch (XPathException err) {
//            err.printStackTrace();
//            throw new InternalError(err.getMessage());
//        }
    }

    /**
     * End an element in the expression tree
     * @return the depth of the tree after ending this element. For diagnostics, this can be compared with the
     * value returned by startElement()
     */

    public int endElement() {
        try {
            if (inStartTag) {
                receiver.startContent();
                inStartTag = false;
            }
            receiver.endElement();
        } catch (XPathException err) {
            err.printStackTrace();
        }
        return --depth;
    }

    /**
     * Start a child element in the output
     * @param name the name of the child element
     */

    public void startSubsidiaryElement(String name) {
        startElement(name);
    }

    /**
     * End a child element in the output
     */

    public void endSubsidiaryElement() {
        endElement();
    }

    /**
     * Close the output
     */

    public void close() {
        try {
            receiver.endDocument();
            receiver.close();
        } catch (XPathException err) {
            err.printStackTrace();
        }
    }

    /**
     * Get the Saxon configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the name pool
     * @return the name pool
     */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Get the type hierarchy cache
     * @return the type hierarchy cache
     */

    public TypeHierarchy getTypeHierarchy() {
        return config.getTypeHierarchy();
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.