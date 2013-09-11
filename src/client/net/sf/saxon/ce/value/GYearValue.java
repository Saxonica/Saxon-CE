package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.functions.FormatDate;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;


/**
 * Implementation of the xs:gYear data type
 */

public class GYearValue extends GDateValue {

    private static RegExp regex =
            RegExp.compile("(-?[0-9]+)(Z|[+-][0-9][0-9]:[0-9][0-9])?");

    private GYearValue(){}

    public static ConversionResult makeGYearValue(CharSequence value) {
        GYearValue g = new GYearValue();
        MatchResult m = regex.exec(Whitespace.trimWhitespace(value).toString());
        if (m == null) {
            return new ValidationFailure("Cannot convert '" + value + "' to a gYear");
        }
        String base = m.getGroup(1);
        String tz = m.getGroup(2);
        String date = (base==null ? "" : base) + "-01-01" + (tz==null ? "" : tz);
        return setLexicalValue(g, date);
    }

    public GYearValue(int year, int tz) {
        this.year = year;
        this.month = 1;
        this.day = 1;
        setTimezoneInMinutes(tz);
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public AtomicValue copy() {
        return new GYearValue(year, getTimezoneInMinutes());
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getItemType() {
        return BuiltInAtomicType.G_YEAR;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate)  {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.G_YEAR) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValue());
        } else {
            return new ValidationFailure("Cannot convert gYear to " + requiredType.getDisplayName(), "XPTY0004");
        }
    }

    public CharSequence getPrimitiveStringValue() {
        try {
            return FormatDate.formatDate(this, "[Y0001][Z]", "en");
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.