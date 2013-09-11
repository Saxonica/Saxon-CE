package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.functions.FormatDate;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.ValidationFailure;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Implementation of the xs:gDay data type
 */

public class GDayValue extends GDateValue {

    private static RegExp regex =
            RegExp.compile("---([0-9][0-9])(Z|[+-][0-9][0-9]:[0-9][0-9])?");

    private GDayValue(){}

    public static ConversionResult makeGDayValue(CharSequence value) {
        MatchResult m = regex.exec(Whitespace.trimWhitespace(value).toString());
        if (m == null) {
            return new ValidationFailure("Cannot convert '" + value + "' to a gDay");
        }
        GDayValue g = new GDayValue();
        String base = m.getGroup(1);
        String tz = m.getGroup(2);
        String date = "2000-01-" + (base==null ? "" : base) + (tz==null ? "" : tz);
        return setLexicalValue(g, date);
    }

    public GDayValue(int day, int tz) {
        this.year = 2000;
        this.month = 1;
        this.day = day;
        setTimezoneInMinutes(tz);
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public AtomicValue copy() {
        return new GDayValue(day, getTimezoneInMinutes());
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getItemType() {
        return BuiltInAtomicType.G_DAY;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        if (requiredType == BuiltInAtomicType.ANY_ATOMIC || requiredType == BuiltInAtomicType.G_DAY) {
            return this;
        } else if (requiredType == BuiltInAtomicType.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(getStringValue());
        } else if (requiredType == BuiltInAtomicType.STRING) {
            return new StringValue(getStringValue());
        } else {
            return new ValidationFailure("Cannot convert gDay to " + requiredType.getDisplayName(), "XPTY0004");
        }
    }

    public CharSequence getPrimitiveStringValue() {
        try {
            return FormatDate.formatDate(this, "---[D01][Z]", "en");
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.