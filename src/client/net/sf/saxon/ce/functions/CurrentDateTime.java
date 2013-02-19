package client.net.sf.saxon.ce.functions;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionVisitor;
import client.net.sf.saxon.ce.expr.StaticProperty;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.StandardNames;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.BuiltInAtomicType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.DateTimeValue;

/**
* This class implements the XPath 2.0 functions
 * current-date(), current-time(), and current-dateTime(), as
 * well as the function implicit-timezone(). The value that is required
 * is inferred from the type of result required.
*/


public class CurrentDateTime extends SystemFunction {

    public CurrentDateTime newInstance() {
        return new CurrentDateTime();
    }   

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * (because the value of the expression depends on the runtime context)
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        // current date/time is part of the context, but it is fixed for a transformation, so
        // we don't need to manage it as a dependency: expressions using it can be freely
        // rearranged
       return StaticProperty.DEPENDS_ON_RUNTIME_ENVIRONMENT;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        final DateTimeValue dt = DateTimeValue.getCurrentDateTime(context);
        final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        final int targetType = getItemType(th).getPrimitiveType();
        switch (targetType) {
            case StandardNames.XS_DATE_TIME:
                return dt;
            case StandardNames.XS_DATE:
                return dt.convert(BuiltInAtomicType.DATE, true).asAtomic();
            case StandardNames.XS_TIME:
                return dt.convert(BuiltInAtomicType.TIME, true).asAtomic();
            case StandardNames.XS_DAY_TIME_DURATION:
            case StandardNames.XS_DURATION:
                return dt.getComponent(Component.TIMEZONE);
            default:
                throw new IllegalArgumentException("Wrong target type for current date/time");
        }
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
