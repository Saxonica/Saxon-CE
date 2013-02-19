package client.net.sf.saxon.ce.tree.util;

/**
 * This interface is primarily for the purposes of reporting where
 * an error occurred in the XML source or transformation instructions.
 */
public interface SourceLocator {

    /**
     * Return the system identifier for the current document event.
     *
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.</p>
     *
     * @return A string containing the system identifier, or null
     *         if none is available.
     */
    public String getSystemId();

    /**
     * Return a string identifying the current location for the purpose
     * of error messages. Designed to be suitable for use as the LOCATION in a phrase
     * such as "Error XXX at LOCATION". May be null if no location information is available
     */

    public String getLocation();


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
