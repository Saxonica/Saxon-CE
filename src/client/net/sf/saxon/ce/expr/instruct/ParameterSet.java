package client.net.sf.saxon.ce.expr.instruct;
import client.net.sf.saxon.ce.om.Sequence;

/**
 * A ParameterSet is a set of parameters supplied when calling a template.
 * It is a collection of id-value pairs, the ids being numeric aliases for the parameter name,
 * unique within a stylesheet
 */

public class ParameterSet {
	private int[] keys;
    private Sequence[] values;
    private boolean[] typeChecked;
    private int used = 0;

    public static ParameterSet EMPTY_PARAMETER_SET = new ParameterSet(0);

    /**
     * Create an empty parameter set
     */

    public ParameterSet() {
        this(10);
    }

    /**
     * Create a parameter set specifying the initial capacity
     */

    public ParameterSet(int capacity) {
        keys = new int[capacity];
        values = new Sequence[capacity];
        typeChecked = new boolean[capacity];
    }

    /**
     * Create a parameter set as a copy of an existing parameter set
     */

    public ParameterSet(ParameterSet existing, int extra) {
        this(existing.used + extra);
        for (int i=0; i<existing.used; i++) {
            put(existing.keys[i], existing.values[i], existing.typeChecked[i]);
        }
    }

    /**
     * Add a parameter to the ParameterSet
     *
     * @param id The parameter id, representing its name.
     * @param value The value of the parameter
     * @param checked True if the caller has done static type checking against the required type
     */

    public void put (int id, Sequence value, boolean checked) {
        for (int i=0; i<used; i++) {
            if (keys[i] == id) {
                values[i] = value;
                typeChecked[i] = checked;
                return;
            }
        }
        if (used+1 > keys.length) {
            int newlength = (used<=5 ? 10 : used*2);
        	int[] newkeys = new int[newlength];
            boolean[] newChecked = new boolean[newlength];
            Sequence[] newvalues = new Sequence[newlength];
            System.arraycopy(values, 0, newvalues, 0, used);
            System.arraycopy(keys, 0, newkeys, 0, used);
            System.arraycopy(typeChecked, 0, newChecked, 0, used);
            values = newvalues;
            keys = newkeys;
        }
        keys[used] = id;
        typeChecked[used] = checked;
        values[used++] = value;
    }

    /**
     * Get the index position of a parameter
     *
     * @param id The numeric parameter id, representing its name.
     * @return The index position of the parameter, or -1 if not defined
     */

    public int getIndex (int id) {
        for (int i=0; i<used; i++) {
            if (keys[i] == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the value of the parameter at a given index
     */

    public Sequence getValue(int index) {
        return values[index];
    }

    /**
     * Determine whether the parameter at a given index has been type-checked
     */

    public boolean isTypeChecked(int index) {
        return typeChecked[index];
    }

    /**
     * Clear all values
     */

    public void clear() {
        used = 0;
    }

    public static final int NOT_SUPPLIED = 0;
    public static final int SUPPLIED = 1;
    public static final int SUPPLIED_AND_CHECKED = 2;

}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.