package client.net.sf.saxon.ce.om;

 /**
 * This class encapsulates a string used as the value of the document-uri() property of a document,
 * together with a normalized representation of the string used for equality comparisons. The idea
 * is that on Windows systems, document URIs are compared using case-blind comparison, but the original
 * case is retained for display purposes.
 */
public class DocumentURI {

    public final static boolean CASE_BLIND_FILES = false;

    private String displayValue;
    private String normalizedValue;

    /**
     * Create a DocumentURI object that wraps a given URI
     * @param uri the URI to be wrapped. Must not be null
     * @throws NullPointerException if uri is null
     */

    public DocumentURI(String uri) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.displayValue = uri;
        this.normalizedValue = normalizeURI(uri);
    }

    @Override
    public String toString() {
        return displayValue;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DocumentURI && normalizedValue.equals(((DocumentURI)obj).normalizedValue);
    }

    @Override
    public int hashCode() {
        return normalizedValue.hashCode();
    }

    /**
     * Normalize the representation of file: URIs to give better equality matching than straight
     * string comparison. The main purpose is (a) to eliminate the distinction between "file:/" and
     * "file:///", and (b) to normalize case in the case of Windows filenames: especially the distinction
     * between "file:/C:" and "file:/c:".
     * @param uri the URI to be normalized
     * @return the normalized URI.
     */

    public static String normalizeURI(String uri) {
        return uri;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


