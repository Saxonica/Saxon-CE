package client.net.sf.saxon.ce.om;

/**
 * This interface represents a document node as defined in the XPath 2.0 data model.
 * It extends NodeInfo, which is used to represent any node. Every document node must
 * be an instance of DocumentInfo.
 * <p>
 * The interface supports two methods in addition to those for NodeInfo: one to find
 * elements given their ID value, and one to locate unparsed entities. In addition,
 * document nodes have an important property that is not true of nodes in general:
 * two distinct Java DocumentInfo objects never represent the same document node.
 * So the Java "==" operator returns the same result as the {@link NodeInfo#isSameNodeInfo}
 * method.
 * <p>
 * This interface is part of the Saxon public API, and as such (from Saxon8.4 onwards)
 * those methods that form part of the stable public API are labelled with a JavaDoc "since" tag
 * to indicate when they were added to the product.
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public interface DocumentInfo extends NodeInfo {

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @return the element with the given ID, or null if there is no such ID
     *     present (or if the parser has not notified attributes as being of
     *     type ID)
     * @since 8.4.
     */

    public NodeInfo selectID(String id);

    /**
     * Set user data on the document node. The user data can be retrieved subsequently
     * using {@link #getUserData}
     * @param key A string giving the name of the property to be set. Clients are responsible
     * for choosing a key that is likely to be unique. Must not be null. Keys used internally
     * by Saxon are prefixed "claxon:".
     * @param value The value to be set for the property. May be null, which effectively
     * removes the existing value for the property.
     */

    public void setUserData(String key, Object value);

    /**
     * Get user data held in the document node. This retrieves properties previously set using
     * {@link #setUserData}
     * @param key A string giving the name of the property to be retrieved.
     * @return the value of the property, or null if the property has not been defined.
     */

    public Object getUserData(String key);

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
