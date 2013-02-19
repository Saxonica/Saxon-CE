package client.net.sf.saxon.ce.lib;

import client.net.sf.saxon.ce.functions.EscapeURI;
import client.net.sf.saxon.ce.tree.util.URI;

/**
 * This class checks whether a string is a valid URI. Different checking rules can be chosen by including
 * a different URIChecker  used when the value is checked.
 */
public class StandardURIChecker {

    private static StandardURIChecker THE_INSTANCE = new StandardURIChecker();

    public static StandardURIChecker getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Protected constructor to allow subclassing
     */

    protected StandardURIChecker() {}

    /**
     * Validate a string to determine whether it is a valid URI
     * @param value the string to be checked
     * @return true if the string is considered to be a valid URI
     */

    public boolean isValidURI(CharSequence value) {

        String sv = value.toString();

        // Allow zero-length strings (RFC2396 is ambivalent on this point)
        if (sv.length() == 0) {
            return true;
        }

        // Allow a string if the java.net.URI class accepts it
        try {
            new URI(sv);
            return true;
        } catch (URI.URISyntaxException e) {
            // keep trying
            // Note: it's expensive to throw exceptions on a success path, so we keep a cache.
        }

        // Allow a string if it can be escaped into a form that java.net.URI accepts
        sv = EscapeURI.iriToUri(sv).toString();
        try {
            new URI(sv);
            return true;
        } catch (URI.URISyntaxException e) {
            return false;
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


