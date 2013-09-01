package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;


/**
 * Implementation of the xs:gMonth data type
 */

public class GMonthValue extends GDateValue {

    private static RegExp regex =
            //Pattern.compile("--([0-9][0-9])(--)?(Z|[+-][0-9][0-9]:[0-9][0-9])?");
            RegExp.compile("--([0-9][0-9])(Z|[+-][0-9][0-9]:[0-9][0-9])?");
            // The commented-out pattern tolerates the bogus format --MM-- which was wrongly permitted by the original schema spec

    private GMonthValue(){}

    public static ConversionResult makeGMonthValue(CharSequence value) {
        GMonthValue g = new GMonthValue();
        MatchResult m = regex.exec(Whitespace.trimWhitespace(value).toString());
        if (m == null) {
            return new ValidationFailure("Cannot convert '" + value + "' to a gMonth");
        }
        String base = m.getGroup(1);
        String tz = m.getGroup(2);
        String date = "2000-" + (base==null ? "" : base) + "-01" + (tz==null ? "" : tz);
        g.typeLabel = BuiltInAtomicType.G_MONTH;
        return setLexicalValue(g, date);
    }

    public GMonthValue(int month, int tz) {
        this(month, tz, BuiltInAtomicType.G_MONTH);
    }

    public GMonthValue(int month, int tz, BuiltInAtomicType type) {
        this.year = 2000;
        this.month = month;
        this.day = 1;
        setTimezoneInMinutes(tz);
        this.typeLabel = type;
    }

     /**
     * Make a copy of this date, time, or dateTime value
      */

     public AtomicValue copy() {
        GMonthValue v = new GMonthValue(month, getTimezoneInMinutes());
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.G_MONTH;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.G_MONTH) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValueCS());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValueCS());
        } else {
            ValidationFailure err = new ValidationFailure("Cannot convert gMonth to " +
                    requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    public CharSequence getPrimitiveStringValue() {

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);

        sb.append("--");
        appendTwoDigits(sb, month);

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

    }

    /**
     * Add a duration to this date/time value
     *
     * @param duration the duration to be added (which might be negative)
     * @return a new date/time value representing the result of adding the duration. The original
     *         object is not modified.
     * @throws client.net.sf.saxon.ce.trans.XPathException
     *
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        XPathException err = new XPathException("Cannot add a duration to an xs:gMonth");
        err.setErrorCode("XPTY0004");
        throw err;
    }

    /**
     * Return a new date, time, or dateTime with the same normalized value, but
     * in a different timezone
     *
     * @param tz the new timezone, in minutes
     * @return the date/time in the new timezone
     */

    public CalendarValue adjustTimezone(int tz) {
        DateTimeValue dt = (DateTimeValue)toDateTime().adjustTimezone(tz);
        return new GMonthValue(dt.getMonth(), dt.getTimezoneInMinutes());
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.