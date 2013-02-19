package client.net.sf.saxon.ce.om;

import client.net.sf.saxon.ce.Configuration;


/**
 * AttributeCollection represents the collection of attributes available on a particular element
 * node. It is modelled on the SAX2 Attributes interface, but is extended firstly to work with
 * Saxon NamePools, and secondly to provide type information as required by the XPath 2.0 data model.
 */

public class AttributeCollection {

    // Attribute values are maintained as an array of Strings. Everything else is maintained
    // in the form of integers.

    private Configuration config;
    private String[] values = null;
    private int[] codes = null;
    private int used = 0;

    // Empty attribute collection. The caller is trusted not to try and modify it.

    public static AttributeCollection EMPTY_ATTRIBUTE_COLLECTION =
            new AttributeCollection((Configuration)null);

    /**
     * Create an empty attribute list.
     * @param config the Saxon Configuration
     */

    public AttributeCollection(Configuration config) {
        this.config = config;
        used = 0;
    }

    /**
     * Create an attribute list as a copy of an existing attribute list
     * @param atts the existing attribute list to be copied
     * @return the copied attribute list. Note that if the original attribute list
     * is empty, the method returns the singleton object {@link #EMPTY_ATTRIBUTE_COLLECTION};
     * this case must therefore be handled specially if the returned attribute list is to
     * be modified.
     */

    public static AttributeCollection copy(AttributeCollection atts) {
        if (atts.getLength() == 0) {
            return EMPTY_ATTRIBUTE_COLLECTION;
        }
        AttributeCollection t = new AttributeCollection(atts.config);
        t.used = atts.used;
        t.values = new String[atts.used];
        t.codes = new int[atts.used];
        System.arraycopy(atts.values, 0, t.values, 0, atts.used);
        System.arraycopy(atts.codes, 0, t.codes, 0, atts.used);
        return t;
    }

    /**
     * Add an attribute to an attribute list. The parameters correspond
     * to the parameters of the {@link client.net.sf.saxon.ce.event.Receiver#attribute(int, CharSequence)}
     * method. There is no check that the name of the attribute is distinct from other attributes
     * already in the collection: this check must be made by the caller.
     *
     * @param nameCode Integer representing the attribute name.
     * @param value    The attribute value (must not be null)
     */

    public void addAttribute(int nameCode, String value) {
        if (values == null) {
            values = new String[5];
            codes = new int[5];
            used = 0;
        }
        if (values.length == used) {
            int newsize = (used == 0 ? 5 : used * 2);
            String[] v2 = new String[newsize];
            int[] c2 = new int[newsize];
            System.arraycopy(values, 0, v2, 0, used);
            System.arraycopy(codes, 0, c2, 0, used);
            values = v2;
            codes = c2;
        }
        codes[used] = nameCode;
        values[used++] = value;
    }

    /**
     * Clear the attribute list. This removes the values but doesn't free the memory used.
     * free the memory, use clear() then compact().
     */

    public void clear() {
        used = 0;
    }

    /**
     * Compact the attribute list to avoid wasting memory
     */

    public void compact() {
        if (used == 0) {
            codes = null;
            values = null;
        } else if (values.length > used) {
            String[] v2 = new String[used];
            int[] c2 = new int[used];
            System.arraycopy(values, 0, v2, 0, used);
            System.arraycopy(codes, 0, c2, 0, used);
            values = v2;
            codes = c2;
        }
    }

    /**
     * Return the number of attributes in the list.
     *
     * @return The number of attributes that have been created in this attribute collection. This is the number
     * of slots used in the list, including any slots allocated to attributes that have since been deleted.
     * Such slots are not reused, to preserve attribute identity.
     */

    public int getLength() {
        return (values == null ? 0 : used);
    }

    /**
     * Get the namecode of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The display name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public int getNameCode(int index) {
        if (codes == null) {
            return -1;
        }
        if (index < 0 || index >= used) {
            return -1;
        }

        return codes[index];
    }

    /**
     * Get the prefix of the name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The prefix of the attribute name as a string, or null if there
     *         is no attribute at that position. Returns "" for an attribute that
     *         has no prefix.
     */

    public String getPrefix(int index) {
        if (codes == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return config.getNamePool().getPrefix(getNameCode(index));
    }

    /**
     * Get the local name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getLocalName(int index) {
        if (codes == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return config.getNamePool().getLocalName(getNameCode(index));
    }

    /**
     * Get the namespace URI of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getURI(int index) {
        if (codes == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return config.getNamePool().getURI(getNameCode(index));
    }


    /**
     * Get the value of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The attribute value as a string, or null if
     *         there is no attribute at that position.
     */

    public String getValue(int index) {
        if (values == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return values[index];
    }

    /**
     * Get the value of an attribute (by name).
     *
     * @param uri       The namespace uri of the attribute.
     * @param localname The local name of the attribute.
     * @return The index position of the attribute
     */

    public String getValue(String uri, String localname) {
        int index = findByName(uri, localname);
        return (index < 0 ? null : getValue(index));
    }

    /**
     * Get the attribute value using its fingerprint
     */

    public String getValueByFingerprint(int fingerprint) {
        int index = findByFingerprint(fingerprint);
        return (index < 0 ? null : getValue(index));
    }

    /**
     * Get the index of an attribute, from its lexical QName
     *
     * @param qname The lexical QName of the attribute. The prefix must match.
     * @return The index position of the attribute
     */

    public int getIndex(String qname) {
        if (codes == null) {
            return -1;
        }
        if (qname.indexOf(':') < 0) {
            return findByName("", qname);
        }
        // Searching using prefix+localname is not recommended, but SAX allows it...
        String[] parts;
        try {
            parts = NameChecker.getQNameParts(qname);
        } catch (QNameException err) {
            return -1;
        }
        String prefix = parts[0];
        if (prefix.length() == 0) {
            return findByName("", qname);
        } else {
            String localName = parts[1];
            for (int i = 0; i < used; i++) {
                String lname = config.getNamePool().getLocalName(getNameCode(i));
                String ppref = config.getNamePool().getPrefix(getNameCode(i));
                if (localName.equals(lname) && prefix.equals(ppref)) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Get the index, given the fingerprint.
     * Return -1 if not found.
     */

    public int getIndexByFingerprint(int fingerprint) {
        return findByFingerprint(fingerprint);
    }

    /**
     * Get the value of an attribute (by lexical QName).
     *
     * @param name The attribute name (a lexical QName).
     * The prefix must match the prefix originally used. This method is defined in SAX, but is
     * not recommended except where the prefix is null.
     */

    public String getValue(String name) {
        int index = getIndex(name);
        return getValue(index);
    }

    /**
     * Find an attribute by expanded name
     * @param uri the namespace uri
     * @param localName the local name
     * @return the index of the attribute, or -1 if absent
     */

    private int findByName(String uri, String localName) {
        if (config == null) {
            return -1;		// indicates an empty attribute set
        }
        NamePool namePool = config.getNamePool();
        int f = namePool.getFingerprint(uri, localName);
        if (f == -1) {
            return -1;
        }
        return findByFingerprint(f);
    }

    /**
     * Find an attribute by fingerprint
     * @param fingerprint the fingerprint representing the name of the required attribute
     * @return the index of the attribute, or -1 if absent
     */

    private int findByFingerprint(int fingerprint) {
        if (codes == null) {
            return -1;
        }
        for (int i = 0; i < used; i++) {
            if (fingerprint == (codes[i] & NamePool.FP_MASK)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determine whether a given attribute has the is-ID property set
     */

    public boolean isId(int index) {
        return (codes[index] & NamePool.FP_MASK) == StandardNames.XML_ID;
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
