package client.net.sf.saxon.ce.expr.sort;

/**
 * An object used as a comparison key. Two XPath atomic values are equal under the "eq" operator
 * if and only if their comparison keys are equal under the Java equals() method.
 */

public class ComparisonKey {
    int category;
    Object value;

    /**
     * Create a comparison key for a value in a particular category. The "category" here represents a
     * set of primitive types that allow mutual comparison (so all numeric values are in the same category).
     * @param category the category
     * @param value the value within the category
     */

    public ComparisonKey(int category, Object value) {
        this.category = category;
        this.value = value;
    }

    /**
     * Test if two comparison keys are equal
     * @param other the other comparison key
     * @return true if they are equal
     * @throws ClassCastException if the other object is not a ComparisonKey
     */
    public boolean equals(Object other) {
        if (other instanceof ComparisonKey) {
            ComparisonKey otherKey = (ComparisonKey)other;
            return category == otherKey.category &&
                    value.equals(otherKey.value);
        } else {
            throw new ClassCastException("Cannot compare a ComparisonKey to an object of a different class");
        }
    }

    /**
     * Get a hashcode for a comparison key. If two comparison keys are equal, they must have the same hash code.
     * @return the hash code.
     */
    public int hashCode() {
        return value.hashCode() ^ category;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
