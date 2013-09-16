package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.functions.FormatDate;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;


/**
 * Implementation of the xs:gYearMonth data type
 */

public class GYearMonthValue extends GDateValue {

    private static RegExp regex =
            RegExp.compile("(-?[0-9]+-[0-9][0-9])(Z|[+-][0-9][0-9]:[0-9][0-9])?");

    private GYearMonthValue(){}

    public static ConversionResult makeGYearMonthValue(CharSequence value) {
        MatchResult m = regex.exec(Whitespace.trimWhitespace(value).toString());
        if (m == null) {
            return new ValidationFailure("Cannot convert '" + value + "' to a gYearMonth");
        }
        GYearMonthValue g = new GYearMonthValue();
        String base = m.getGroup(1);
        String tz = m.getGroup(2);
        String date = (base==null ? "" : base) + "-01" + (tz==null ? "" : tz);
        return setLexicalValue(g, date);
    }

    public GYearMonthValue(int year, int month, int tz) {
        this.year = year;
        this.month = month;
        this.day = 1;
        setTimezoneInMinutes(tz);
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public AtomicValue copy() {
        return new GYearMonthValue(year, month, getTimezoneInMinutes());
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.G_YEAR_MONTH;
    }

    /**
    * Convert to target data type
    *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public ConversionResult convert(AtomicType requiredType) {
        if (requiredType == AtomicType.ANY_ATOMIC || requiredType == AtomicType.G_YEAR_MONTH) {
            return this;
        } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == AtomicType.STRING) {
            return new StringValue(getStringValue());
        } else {
            return new ValidationFailure("Cannot convert gYearMonth to " + requiredType.getDisplayName(), "XPTY0004");
        }
    }

    public CharSequence getPrimitiveStringValue() {
        try {
            return FormatDate.formatDate(this, "[Y0001]-[M01][Z]", "en");
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.