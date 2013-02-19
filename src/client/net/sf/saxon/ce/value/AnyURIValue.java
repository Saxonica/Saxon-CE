package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.type.AtomicType;
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
 * <p>This implementation of xs:anyURI allows any string to be contained in the value space. To check that
 * the URI is valid according to some set of syntax rules, the caller should invoke a {@link client.net.sf.saxon.ce.lib.StandardURIChecker}
 * before constructing the AnyURIValue.</p>
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
        typeLabel = BuiltInAtomicType.ANY_URI;
    }

    /**
     * Constructor for a user-defined subtype of anyURI
     * @param value the String value. Null is taken as equivalent to "".
     * @param type  a user-defined subtype of anyURI. It is the caller's responsibility
     *              to ensure that this is actually a subtype of anyURI, and that the value conforms
     *              to the definition of this type.
     */

    public AnyURIValue(CharSequence value, AtomicType type) {
        this.value = (value == null ? "" : Whitespace.collapseWhitespace(value).toString());
        typeLabel = type;
    }


    /**
     * Create a copy of this atomic value, with a different type label
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        AnyURIValue v = new AnyURIValue(value);
        v.noSurrogates = noSurrogates;
        v.typeLabel = typeLabel;
        return v;
    }

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.ANY_URI;
    }

    /**
     * Convert to target data type
     * @param requiredType integer code representing the item type required
     * @return the result of the conversion, or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        int req = requiredType.getPrimitiveType();
        switch (req) {
            case StandardNames.XS_ANY_ATOMIC_TYPE:
            case StandardNames.XS_ANY_URI:
                return this;
            case StandardNames.XS_UNTYPED_ATOMIC:
                return new UntypedAtomicValue(value);
            case StandardNames.XS_STRING:
                return new StringValue(value);
             default:
                ValidationFailure err = new ValidationFailure("Cannot convert anyURI to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                return err;
        }
    }


    public static String decode(String s) {
        // Evaluates all escapes in s, applying UTF-8 decoding if needed.  Assumes
        // that escapes are well-formed syntactically, i.e., of the form %XX.  If a
        // sequence of escaped octets is not valid UTF-8 then the erroneous octets
        // are replaced with '\uFFFD'.
        // Exception: any "%" found between "[]" is left alone. It is an IPv6 literal
        //            with a scope_id
        //

        // TODO:CLAXON implement this
        return s;
//        if (s == null) {
//            return s;
//        }
//        int n = s.length();
//        if (n == 0) {
//            return s;
//        }
//        if (s.indexOf('%') < 0) {
//            return s;
//        }
//
//        FastStringBuffer sb = new FastStringBuffer(n);
//
//        // This is not horribly efficient, but it will do for now
//        char c = s.charAt(0);
//        boolean betweenBrackets = false;
//
//        for (int i = 0; i < n;) {
//            assert c == s.charAt(i);    // Loop invariant
//            if (c == '[') {
//                betweenBrackets = true;
//            } else if (betweenBrackets && c == ']') {
//                betweenBrackets = false;
//            }
//            if (c != '%' || betweenBrackets) {
//                sb.append(c);
//                if (++i >= n) {
//                    break;
//                }
//                c = s.charAt(i);
//                continue;
//            }
//            bb.clear();
//            for (; ;) {
//                assert (n - i >= 2);
//                bb.put(hex(s.charAt(++i), s.charAt(++i)));
//                if (++i >= n) {
//                    break;
//                }
//                c = s.charAt(i);
//                if (c != '%') {
//                    break;
//                }
//            }
//            bb.flip();
//            sb.append(utf8.decode(bb));
//        }
//
//        return sb.toString();
    }

    private static byte hex(char high, char low) {
        return (byte)((hexToDec(high)<<4) | hexToDec(low));
    }

    private static int hexToDec(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        } else {
            return 0;
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
