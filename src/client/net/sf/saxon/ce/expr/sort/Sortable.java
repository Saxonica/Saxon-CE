package client.net.sf.saxon.ce.expr.sort;



/**
 * A Sortable is an object that can be sorted using the QuickSort method.
 *
 * @author Michael H. Kay
 *
 */

public interface Sortable {

    /**
    * Compare two objects within this Sortable, identified by their position.
    * @return <0 if obj[a]<obj[b], 0 if obj[a]=obj[b], >0 if obj[a]>obj[b]
    */

    public int compare(int a, int b);

    /**
    * Swap two objects within this Sortable, identified by their position.
    */

    public void swap(int a, int b);

}


// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.