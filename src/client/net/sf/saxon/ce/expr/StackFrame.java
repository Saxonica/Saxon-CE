package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.expr.instruct.SlotManager;
import client.net.sf.saxon.ce.om.ValueRepresentation;

import java.util.Stack;

/**
 * This class represents a stack frame holding details of the variables used in a function or in
 * an XSLT template.
 */

public class StackFrame {
    protected SlotManager map;
    protected ValueRepresentation[] slots;
    protected Stack<ValueRepresentation> dynamicStack;

    public StackFrame (SlotManager map, ValueRepresentation[] slots) {
        this.map = map;
        this.slots = slots;
    }

    public SlotManager getStackFrameMap() {
        return map;
    }

    public ValueRepresentation[] getStackFrameValues() {
        return slots;
    }

    public void setStackFrameValues(ValueRepresentation[] values) {
        slots = values;
    }

    public StackFrame copy() {
        ValueRepresentation[] v2 = new ValueRepresentation[slots.length];
        System.arraycopy(slots, 0, v2, 0, slots.length);
        StackFrame s = new StackFrame(map, v2);
        if (dynamicStack != null) {
            s.dynamicStack = new Stack<ValueRepresentation>();
            s.dynamicStack.addAll(dynamicStack);
        }
        return s;
    }

    public void pushDynamicValue(ValueRepresentation value) {
        if (dynamicStack == null) {
            dynamicStack = new Stack<ValueRepresentation>();
        }
        dynamicStack.push(value);
    }

    public ValueRepresentation popDynamicValue() {
        return dynamicStack.pop();
    }

    public static final StackFrame EMPTY =
            new StackFrame(SlotManager.EMPTY, ValueRepresentation.EMPTY_VALUE_ARRAY);

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.