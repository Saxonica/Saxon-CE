package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.om.StructuredQName;

import java.util.List;
import java.util.ArrayList;


/**
 * A SlotManager supports functions, templates, etc: specifically, any executable code that
 * requires a stack frame containing local variables. In XSLT a SlotManager underpins any
 * top-level element that can contain local variable declarations,
 * specifically, a top-level xsl:template, xsl:variable, xsl:param, or xsl:function element
 * or an xsl:attribute-set element or xsl:key element. In XQuery it underpins functions and
 * global variables. The purpose of the SlotManager is to allocate slot numbers to variables
 * in the stack, and to record how many slots are needed. A Debugger may define a subclass
 * with additional functionality.
*/

public class SlotManager {

    /**
     * An empty SlotManager
     */

    public static SlotManager EMPTY = new SlotManager(0);

    private ArrayList<StructuredQName> variableMap = new ArrayList<StructuredQName>(10);
            // values are StructuredQName objects representing the variable names
    private int numberOfVariables = 0;


    public SlotManager(){}

    /**
     * Create a SlotManager with a given number of slots
     */

    public SlotManager(int n) {
        numberOfVariables = n;
        variableMap = new ArrayList(n);
    }

    /**
    * Get number of variables (size of stack frame)
    */

    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    /**
     * Set the number of variables
     * @param numberOfVariables
     */

    public void setNumberOfVariables(int numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
        variableMap.trimToSize();
    }

    /**
    * Allocate a slot number for a variable
    */                                                

    public int allocateSlotNumber(StructuredQName qName) {
        variableMap.add(qName);
        return numberOfVariables++;
    }

    /**
     * Get the variable map (simply a list of variable names as structured QNames). Note that it
     * is possible for several variables to have the same name.
     * <p><b>Changed in Saxon 9.0 to return a list of StructuredQName values rather than integers</b></p>
     */

    public List<StructuredQName> getVariableMap() {
        return variableMap;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
