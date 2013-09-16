package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.functions.FormatDate;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;


/**
 * Implementation of the xs:gYear data type
 */

public class GMonthDayValue extends GDateValue {

    private static RegExp regex =
            RegExp.compile("--([0-9][0-9]-[0-9][0-9])(Z|[+-][0-9][0-9]:[0-9][0-9])?");

    private GMonthDayValue(){}

    public static ConversionResult makeGMonthDayValue(CharSequence value) {
        MatchResult m = regex.exec(Whitespace.trimWhitespace(value).toString());
        if (m == null) {
            return new ValidationFailure("Cannot convert '" + value + "' to a gMonthDay");
        }
        GMonthDayValue g = new GMonthDayValue();
        String base = m.getGroup(1);
        String tz = m.getGroup(2);
        String date = "2000-" + (base==null ? "" : base) + (tz==null ? "" : tz);
        return setLexicalValue(g, date);
    }

    public GMonthDayValue(int month, int day, int tz) {
        this.year = 2000;
        this.month = month;
        this.day = day;
        setTimezoneInMinutes(tz);
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public AtomicValue copy() {
        return new GMonthDayValue(month, day, getTimezoneInMinutes());
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.G_MONTH_DAY;
    }

    /**
    * Convert to target data type
    *
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public ConversionResult convert(AtomicType requiredType)  {
        if (requiredType == AtomicType.ANY_ATOMIC || requiredType == AtomicType.G_MONTH_DAY) {
            return this;
        } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == AtomicType.STRING) {
            return new StringValue(getStringValue());
        } else {
            return new ValidationFailure("Cannot convert gMonthDay to " + requiredType.getDisplayName(), "XPTY0004");
        }
    }

    public CharSequence getPrimitiveStringValue() {
        try {
            return FormatDate.formatDate(this, "--[M01]-[D01][Z]", "en");
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.