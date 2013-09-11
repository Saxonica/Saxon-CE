package client.net.sf.saxon.ce.js;

import client.net.sf.saxon.ce.Configuration;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper;
import client.net.sf.saxon.ce.dom.HTMLDocumentWrapper.DocType;
import client.net.sf.saxon.ce.dom.HTMLNodeWrapper;
import client.net.sf.saxon.ce.dom.XMLDOM;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.om.ValueRepresentation;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.EmptyIterator;
import client.net.sf.saxon.ce.tree.iter.JsArrayIterator;
import client.net.sf.saxon.ce.tree.iter.SingletonIterator;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.*;
import client.net.sf.saxon.ce.value.StringValue;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Event;

import java.util.logging.Logger;

/**
 * This class implements Saxon-CE extension functions designed to allow interoperability between XSLT
 * and JavaScript.
 */
public class IXSLFunction extends FunctionCall {

    private String localName;
    private Logger logger = Logger.getLogger("IXSLFunction");

    private static int injectCount = 0;

    public IXSLFunction(String localName, Expression[] arguments) {
        this.localName = localName;
        setArguments(arguments);
    }

    @Override
    public StructuredQName getFunctionName() {
        return new StructuredQName(NamespaceConstant.IXSL, "", localName);
    }

    @Override
    protected void checkArguments(ExpressionVisitor visitor) throws XPathException {

    }

    @Override
    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    @Override
    public ItemType getItemType() {
        return AnyItemType.getInstance();
    }

    @Override
    protected int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    @Override
    protected int computeSpecialProperties() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }

    private static native JavaScriptObject jsWindow()
        /*-{
            return $wnd;
        }-*/;

    /**
     * Call a JavaScript function (a method on a JavaScript object)
     *
     * @param target    the JavaScript object owning the method
     * @param member    the name of the function to be called as a string
     * @param arguments the arguments to the method call
     * @return the result of evaluating the expression. To ensure that this is always
     *         an Object, it is returned as a type/value pair
     */

    private static native JavaScriptObject jsCall(JavaScriptObject target, String member, JavaScriptObject arguments)
    /*-{
        var v = target[member].apply(target, arguments);
        return { type: typeof v, value: v}
    }-*/;

    private static native Object jsProperty(JavaScriptObject target, String member)
        /*-{
            return target[member];
        }-*/;

    /**
     * Set a JavaScript property value (a property of a JavaScript object)
     *
     * @param target the JavaScript object owning the method
     * @param member the name of the property to be fetched as a string - may be
     *               a chained set of properties e.g. loction.href
     */
    public static native void setProperty(JavaScriptObject target, String member, Object value)
    /*-{
        var props = member.split('.');
        var newTarget = target;
        for (var count = 0; count < props.length - 1; count++) {
            newTarget = newTarget[props[count]];
        }
        newTarget[props[props.length - 1]] = value;

    }-*/;

    /**
     * Get a JavaScript property (a property of a JavaScript object)
     *
     * @param target the JavaScript object owning the method
     * @param member the name of the property to be fetched as a string - may be
     *               a chained set of properties e.g. loction.href
     * @return the result of evaluating the expression. To ensure that this is always
     *         an Object, it is returned as a type/value pair
     */
    private static native JavaScriptObject jsSplitPropertyTypeAndValue(JavaScriptObject target, String member)
    /*-{
        var props = member.split('.');
        var v = target;
        for (var count = 0; count < props.length; count++) {
            v = v[props[count]];
        }
        return { type: typeof v, value: v}
    }-*/;

    private static native double jsNumberProperty(JavaScriptObject target, String member)
        /*-{
            return target[member];
        }-*/;

    private static native boolean jsBooleanProperty(JavaScriptObject target, String member)
        /*-{
            return target[member];
        }-*/;

    public static native JavaScriptObject jsArray(int length) /*-{
        return new Array(length);
    }-*/;

    public static native void jsSetArrayItem(JavaScriptObject array, int index, Object value) /*-{
        array[index] = value;
    }-*/;

    private static native Object jsGetArrayItem(JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    private static native int jsGetArrayLength(JavaScriptObject array) /*-{
        return array.length;
    }-*/;

    /**
     * Special issues with determining if an array - because other object
     * types such as Window are represented as arrays of length 1
     */
    private static native boolean isJsArray(JavaScriptObject obj) /*-{
        return (obj.length != undefined);
    }-*/;

    @SuppressWarnings("rawtypes")
    public static SequenceIterator convertFromJavaScript(Object jsValue, Configuration config) {
        if (jsValue == null) {
            return EmptyIterator.getInstance();
        } else if (jsValue instanceof String) {
            return SingletonIterator.makeIterator(new StringValue((String) jsValue));
        } else if (jsValue instanceof Double) {
            return SingletonIterator.makeIterator(new DoubleValue((Double) jsValue));
        } else if (jsValue instanceof Boolean) {
            return SingletonIterator.makeIterator(BooleanValue.get((Boolean) jsValue));
        } else if (!(jsValue instanceof JavaScriptObject)) {
            return EmptyIterator.getInstance();
        }

        JavaScriptObject jsObj = (JavaScriptObject) jsValue;
        short nodeType = getNodeType(jsObj);
        if (nodeType == -1) {
            if (isJsArray(jsObj) && jsGetArrayLength(jsObj) > 1) {
                return new JsArrayIterator((JsArray) jsObj, config);
            } else {
                return SingletonIterator.makeIterator(new JSObjectValue(jsObj));
            }
        }

        com.google.gwt.dom.client.Document page = ((Node) jsValue).getOwnerDocument();
        if (page == null) {
            com.google.gwt.dom.client.Document doc = (Document) jsValue;
            DocType jsDocType = (doc == Document.get()) ? DocType.UNKNOWN : DocType.NONHTML;
            HTMLDocumentWrapper docWrapper = new HTMLDocumentWrapper(doc, doc.getURL(), config, jsDocType);
            return SingletonIterator.makeIterator(docWrapper);
        } else {
            DocType jsDocType = (page == Document.get()) ? DocType.UNKNOWN : DocType.NONHTML;
            HTMLDocumentWrapper htmlDoc = new HTMLDocumentWrapper(page, page.getURL(), config, jsDocType);
            HTMLNodeWrapper htmlNode = htmlDoc.wrap((Node) jsValue);
            return SingletonIterator.makeIterator(htmlNode);
        }
    }


    private static native short getNodeType(JavaScriptObject obj) /*-{
        var out = obj.nodeType;
        return (out == null) ? -1 : out;
    }-*/;

    public static Object convertToJavaScript(ValueRepresentation val) throws XPathException {
        if (val == null || val instanceof EmptySequence) {
            return null;
        }
        if (val instanceof Item) {
            if (val instanceof StringValue) {
                return ((StringValue) val).getStringValue();
            } else if (val instanceof BooleanValue) {
                return ((BooleanValue) val).getBooleanValue();
            } else if (val instanceof NumericValue) {
                return ((NumericValue) val).getDoubleValue();
            } else if (val instanceof HTMLNodeWrapper) {
                return ((HTMLNodeWrapper) val).getUnderlyingNode();
            } else if (val instanceof JSObjectValue) {
                return ((JSObjectValue) val).getJavaScriptObject();
            } else {
                throw new XPathException("Cannot convert " + val.getClass() + " to Javascript object");
            }
        } else {
            int seqLength = ((Value) val).getLength();
            if (seqLength == 0) {
                return null;
            } else if (seqLength == 1) {
                return convertToJavaScript(((Value) val).itemAt(0));
            } else {
                return convertSequenceToArray(val, seqLength);
            }
        }
    }

    private static JavaScriptObject convertSequenceToArray(ValueRepresentation val, int seqLength) throws XPathException {
        JavaScriptObject jsItems = jsArray(seqLength);
        SequenceIterator iterator = Value.asIterator(val);
        int i = 0;
        while (true) {
            Item item = iterator.next();
            if (item == null) {
                break;
            }
            Object jsObject = convertToJavaScript(item);
            jsSetArrayItem(jsItems, i, jsObject);
            i++;
        }
        return jsItems;
    }

    private Object getValueFromTypeValuePair(JavaScriptObject pair) {
        String type = (String) jsProperty(pair, "type");
        if ("number".equals(type)) {
            return new Double(jsNumberProperty(pair, "value"));
        } else if ("boolean".equals(type)) {
            return Boolean.valueOf(jsBooleanProperty(pair, "value"));
        } else {
            return jsProperty(pair, "value");
        }
    }

    private SequenceIterator evaluateJsFunction(String script, XPathContext context) throws XPathException {
        injectCount++;
        String fnName = "fnName" + injectCount;
        script = script.trim();
        String fnScript = "function " + fnName + "() { return " + script + "; }";
        JSObjectValue item = new JSObjectValue(jsWindow());
        JavaScriptObject target = (JavaScriptObject) convertToJavaScript(item);

        ScriptInjector.FromString fs = new ScriptInjector.FromString(fnScript);
        fs.setWindow(target);
        fs.inject();
        JavaScriptObject jsArgs = jsArray(0);
        try {
            Object result = getValueFromTypeValuePair(jsCall(target, fnName, jsArgs));
            return convertFromJavaScript(result, context.getConfiguration());
        } catch (JavaScriptException jexc) {
            throw (new XPathException("JavaScriptException: " + jexc.getDescription() +
                    "\noccurred on evaluating:\n" + script));
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        try {
            if (localName.equals("window")) {
                Item item = new JSObjectValue(jsWindow());
                return SingletonIterator.makeIterator(item);

            } else if (localName.equals("eval")) {
                String script = argument[0].evaluateAsString(context).toString();
                return evaluateJsFunction(script, context);

            } else if (localName.equals("call")) {
                ValueRepresentation itemVal = (ValueRepresentation) argument[0].evaluateItem(context);
                JavaScriptObject target = (JavaScriptObject) convertToJavaScript(itemVal);
                if (target != null) {
                    String method = argument[1].evaluateAsString(context).toString();
                    JavaScriptObject jsArgs = jsArray(argument.length - 2);
                    for (int i = 2; i < argument.length; i++) {
                        ValueRepresentation val = SequenceExtent.makeSequenceExtent(argument[i].iterate(context));
                        jsSetArrayItem(jsArgs, i - 2, convertToJavaScript(val));
                    }
                    // Issue: if following throws an exception - GWT doesn't always allow it to be caught - it's rethrown
                    // as a GWT unhandled exception
                    try {
                        JavaScriptObject jsObj = jsCall(target, method, jsArgs);
                        Object result = getValueFromTypeValuePair(jsObj);
                        return convertFromJavaScript(result, context.getConfiguration());
                    } catch (Exception e) {
                        boolean doRetry = false;

                        // try again setting any null values to zero-length arrays
                        for (int i = 0; i < argument.length - 2; i++) {
                            if (jsGetArrayItem(jsArgs, i) == null) {
                                jsSetArrayItem(jsArgs, i, jsArray(0));
                                doRetry = true;
                            }
                        }
                        if (doRetry) {
                            try {
                                Object result = getValueFromTypeValuePair(jsCall(target, method, jsArgs));
                                return convertFromJavaScript(result, context.getConfiguration());
                            } catch (Exception e2) {
                            }
                        }
                        // if we get this far then throw exception - recovery failed
                        throw (new XPathException("JavaScriptException in ixsl:call(): Object does not support property or method '" +
                                method + "' with " + (argument.length - 2) + " argument(s)."));
                    }
                } else {
                    throw (new XPathException("JavaScriptException in ixsl:call(): Call target object is null or undefined"));
                }
            } else if (localName.equals("get")) {
                ValueRepresentation itemVal = (ValueRepresentation) argument[0].evaluateItem(context);
                JavaScriptObject target = (JavaScriptObject) convertToJavaScript(itemVal);
                if (target != null) {
                    String property = argument[1].evaluateAsString(context).toString();
                    Object result;
                    try {
                        result = getValueFromTypeValuePair(jsSplitPropertyTypeAndValue(target, property));
                    } catch (Exception e) {
                        throw (new XPathException("JavaScriptException in ixsl:get() for property: " + property));
                    }
                    return convertFromJavaScript(result, context.getConfiguration());
                } else {
                    throw (new XPathException("JavaScriptException in ixsl:get(): Get target object is null or undefined"));
                }
            } else if (localName.equals("page")) {
                return SingletonIterator.makeIterator(context.getConfiguration().getHostPage());
            } else if (localName.equals("source")) {
                return SingletonIterator.makeIterator(context.getController().getSourceNode());
            } else if (localName.equals("event")) {
                Event event = (Event) context.getController().getUserData("Saxon-CE", "current-event");
                return SingletonIterator.makeIterator(new JSObjectValue(event));
            } else if (localName.equals("parse-xml")) {
                String data = argument[0].evaluateAsString(context).toString();
                return convertFromJavaScript(XMLDOM.parseXML(data), context.getConfiguration());
            } else {
                // previously there was no warning - strictly - this should be caught at compile time
                logger.warning("No such IXSL function: '" + localName + "' - empty sequence returned");
                return EmptyIterator.getInstance();
            } // end of else
        } // end of try block
        catch (XPathException e) {
            e.maybeSetLocation(this.getSourceLocator());
            throw e;
        } catch (Exception e) {
            XPathException xe = new XPathException("Exception in ixsl:" + localName + "() " + e.getMessage());
            xe.maybeSetLocation(this.getSourceLocator());
            throw xe;
        }

    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


