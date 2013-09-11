package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;



/**
 * An XPath value of type xs:anyURI.
 * <p/>
 * <p>This is implemented as a subtype of StringValue even though xs:anyURI is not a subtype of
 * xs:string in the XPath type hierarchy. This enables type promotion from URI to String to happen
 * automatically in most cases where it is appropriate.</p>
 * <p/>
 * <p>This implementation of xs:anyURI allows any string to be contained in the value space,
 * reflecting the specification in XSD 1.1.</p>
 */

public final class AnyURIValue extends StringValue {

    public static final AnyURIValue EMPTY_URI = new AnyURIValue("");


    /**
     * Constructor
     * @param value the String value. Null is taken as equivalent to "". This constructor
     *        does not check that the value is a valid anyURI instance. It does however
     *        perform whitespace normalization.
     */

    public AnyURIValue(CharSequence value) {
        this.value = (value == null ? "" : Whitespace.collapseWhitespace(value).toString());
    }

    public BuiltInAtomicType getItemType() {
        return BuiltInAtomicType.ANY_URI;
    }

    /**
     * Convert to target data type
     * @param requiredType integer code representing the item type required
     * @return the result of the conversion, or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.ANY_URI) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(value);
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(value);
        } else {
            return new ValidationFailure("Cannot convert anyURI to " +
                    requiredType.getDisplayName(), "XPTY0004");
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
