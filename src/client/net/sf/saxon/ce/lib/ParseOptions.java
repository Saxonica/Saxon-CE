package client.net.sf.saxon.ce.lib;

import client.net.sf.saxon.ce.value.Whitespace;


/**
 * This class defines options for parsing a source document
 */

public class ParseOptions  {

    private int dtdValidation = Validation.DEFAULT;
    private int stripSpace = Whitespace.UNSPECIFIED;
    private Boolean lineNumbering = null;
    private Boolean xIncludeAware = null;

    /**
     * Create a ParseOptions object with default options set
     */

    public ParseOptions() {}

    /**
     * Set the space-stripping action to be applied to the source document
     * @param stripAction one of {@link client.net.sf.saxon.ce.value.Whitespace#IGNORABLE},
     * {@link client.net.sf.saxon.ce.value.Whitespace#ALL}, or {@link client.net.sf.saxon.ce.value.Whitespace#NONE}
     */

    public void setStripSpace(int stripAction) {
        stripSpace = stripAction;
    }

    /**
     * Get the space-stripping action to be applied to the source document
     * @return one of {@link client.net.sf.saxon.ce.value.Whitespace#IGNORABLE},
     * {@link client.net.sf.saxon.ce.value.Whitespace#ALL}, or {@link client.net.sf.saxon.ce.value.Whitespace#NONE}
     */

    public int getStripSpace() {
        return (stripSpace == Whitespace.UNSPECIFIED ? Whitespace.IGNORABLE : stripSpace);
    }

    /**
      * Set whether or not DTD validation of this source is required
      * @param option one of {@link Validation#STRICT},  {@link Validation#LAX},
      * {@link Validation#STRIP}, {@link Validation#DEFAULT}.
     *
     * <p>The value {@link Validation#LAX} indicates that DTD validation is
     * requested, but validation failures are treated as warnings only.</p>
      */

     public void setDTDValidationMode(int option) {
         dtdValidation = option;
     }

     /**
      * Get whether or not DTD validation of this source is required
      * @return the validation mode requested, or {@link Validation#DEFAULT}
      * to use the default validation mode from the Configuration.
      *
      * <p>The value {@link Validation#LAX} indicates that DTD validation is
     * requested, but validation failures are treated as warnings only.</p>
      */

     public int getDTDValidationMode() {
         return dtdValidation;
     }

    /**
     * Set whether line numbers are to be maintained in the constructed document
     * @param lineNumbering true if line numbers are to be maintained
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = Boolean.valueOf(lineNumbering);
    }

    /**
     * Get whether line numbers are to be maintained in the constructed document
     * @return true if line numbers are maintained
     */

    public boolean isLineNumbering() {
        return lineNumbering != null && lineNumbering.booleanValue();
    }

    /**
     * <p>Get state of XInclude processing.</p>
     *
     * @return current state of XInclude processing. Default value is false.
     */

    public boolean isXIncludeAware() {
        return xIncludeAware != null && xIncludeAware.booleanValue();
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.