package client.net.sf.saxon.ce.expr.z;


import client.net.sf.saxon.ce.tree.util.FastStringBuffer;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Set of int values. This class is modelled on the java.net.Set interface, but it does
 * not implement this interface, because the set members are int's rather than Objects.
 * <p/>
 * This implementation of a set of integers is optimized to use very little storage
 * and to provide fast comparison of two sets. The equals() method determines whether
 * two sets contain the same integers.
 * <p/>
 * This implementation is not efficient at adding new integers to the set. It creates a new
 * array each time you do that.
 * <p/>
 * Not thread safe.
 *
 * @author Michael Kay
 */
public class IntArraySet extends AbstractIntSet implements Serializable, IntSet {

    public static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * The array of integers, which will always be sorted
     */

    private int[] contents;

    /**
     * Hashcode, evaluated lazily
     */

    private int hashCode = -1;

    /**
     *  Create an empty set
     */
    public IntArraySet() {
        contents = EMPTY_INT_ARRAY;
    }

    /**
     * Create a set containing integers from the specified IntHashSet
     * @param input the set to be copied
     */

    public IntArraySet(IntHashSet input) {
        // exploits the fact that getValues() constructs a new array
        contents = input.getValues();
        //System.err.println("new IntArraySet(" + contents.length + ")");
        Arrays.sort(contents);
    }

    /**
     * Create one IntArraySet as a copy of another
     * @param input the set to be copied
     */

    public IntArraySet(IntArraySet input) {
        contents = new int[input.contents.length];
        System.arraycopy(input.contents, 0, contents, 0, contents.length);
    }

    public IntSet copy() {
        IntArraySet i2 = new IntArraySet();
        i2.contents = new int[contents.length];
        System.arraycopy(contents, 0, i2.contents, 0, contents.length);
        //i2.contents = Arrays.copyOf(contents, contents.length);
        return i2;
    }

    public IntSet mutableCopy() {
        return copy();
    }

    public void clear() {
        contents = EMPTY_INT_ARRAY;
        hashCode = -1;
    }

    public int size() {
        return contents.length;
    }

    public boolean isEmpty() {
        return contents.length == 0;
    }

    /**
     * Get the set of integer values as an array
     * @return a sorted array of integers
     */

    public int[] getValues() {
        return contents;
    }


    public boolean contains(int value) {
        return Arrays.binarySearch(contents, value) >= 0;
    }

    public boolean remove(int value) {
        hashCode = -1;
        int pos = Arrays.binarySearch(contents, value);
        if (pos < 0) {
            return false;
        }
        int[] newArray = new int[contents.length - 1];
        if (pos > 0) {
            // copy the items before the one that's being removed
            System.arraycopy(contents, 0, newArray, 0, pos);
        }
        if (pos < newArray.length) {
            // copy the items after the one that's being removed
            System.arraycopy(contents, pos+1, newArray, pos, contents.length - pos);
        }
        contents = newArray;
        return true;
    }

    /**
     * Add an integer to the set
     * @param value the integer to be added
     * @return true if the integer was added, false if it was already present
     */

    public boolean add(int value) {
        hashCode = -1;
        if (contents.length == 0) {
            contents = new int[] {value};
            return true;
        }
        int pos = Arrays.binarySearch(contents, value);
        if (pos >= 0) {
            return false;   // value was already present
        }
        pos = -pos - 1;     // new insertion point
        int[] newArray = new int[contents.length + 1];
        if (pos > 0) {
            // copy the items before the insertion point
            System.arraycopy(contents, 0, newArray, 0, pos);
        }
        newArray[pos] = value;
        if (pos < contents.length) {
            // copy the items after the insertion point
            System.arraycopy(contents, pos, newArray, pos+1, newArray.length - pos);
        }
        contents = newArray;
        return true;
    }

    /**
     * Get the first value in the set.
     * @return the first value in the set, in sorted order
     * @throws ArrayIndexOutOfBoundsException if the set is empty
     */

    public int getFirst() {
        return contents[0];
    }

    /**
     * Get an iterator over the values
     * @return an iterator over the values, which will be delivered in sorted order
     */

    public IntIterator iterator() {
        return new IntArraySetIterator();
    }

    /**
     * Form a new set that is the union of this set with another set.
     * @param other the other set
     * @return the union of the two sets
     */

    public IntSet union(IntSet other) {
        // Look for special cases: one set empty, or both sets equal
        if (size() == 0) {
            return other.copy();
        } else if (other.isEmpty()) {
            return copy();
        } else if (other == IntUniversalSet.getInstance()) {
            return other;
        } else if (other instanceof IntComplementSet) {
            return other.union(this);
        }
        if (equals(other)) {
            return copy();
        }
        if (other instanceof IntArraySet) {
            // Form the union by a merge of the two sorted arrays
            int[] merged = new int[size() + other.size()];
            int[] a = contents;
            int[] b = ((IntArraySet)other).contents;
            int m = a.length, n = b.length;
            int o=0, i=0, j=0;
            while (true) {
                if (a[i] < b[j]) {
                    merged[o++] = a[i++];
                } else if (b[j] < a[i]) {
                    merged[o++] = b[j++];
                } else {
                    merged[o++] = a[i++];
                    j++;
                }
                if (i == m) {
                    System.arraycopy(b, j, merged, o, n-j);
                    o += (n-j);
                    return make(merged, o);
                } else if (j == n) {
                    System.arraycopy(a, i, merged, o, m-i);
                    o += (m-i);
                    return make(merged, o);
                }
            }
        } else {
            return super.union(other);
        }
    }

    /**
     * Factory method to construct a set from an array of integers
     * @param in the array of integers, which must be in ascending order
     * @param size the number of elements in the array that are significant
     * @return the constructed set
     */

    public static IntArraySet make(int[] in, int size) {
        int[] out;
        if (in.length == size) {
            out = in;
        } else {
            out = new int[size];
            System.arraycopy(in, 0, out, 0, size);
        }
        return new IntArraySet(out);
    }

    private IntArraySet(int[] content) {
        contents = content;
    }

    public String toString() {
        FastStringBuffer sb = new FastStringBuffer(contents.length*4);
        for (int i=0; i<contents.length; i++) {
            if (i == contents.length - 1) {
                sb.append(contents[i] + "");
            } else if (contents[i]+1 != contents[i+1]) {
                sb.append(contents[i] + ",");
            } else {
                int j = i+1;
                while (contents[j] == contents[j-1]+1) {
                    j++;
                    if (j == contents.length) {
                        break;
                    }
                }
                sb.append(contents[i] + "-" + contents[j-1] + ",");
                i = j;
            }
        }
        return sb.toString();
    }


    /**
     * Test if this set has overlapping membership with another set
     */

//    public boolean containsSome(IntArraySet other) {
//        IntIterator it = other.iterator();
//        while (it.hasNext()) {
//            if (contains(it.next())) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Test whether this set has exactly the same members as another set
     */

    public boolean equals(Object other) {
        if (other instanceof IntArraySet) {
            IntArraySet s = (IntArraySet)other;
            return hashCode() == other.hashCode() && Arrays.equals(contents, s.contents);
        } else
            return other instanceof IntSet &&
                    contents.length == ((IntSet)other).size() &&
                    containsAll((IntSet)other);
    }

    /**
     * Construct a hash key that supports the equals() test
     */

    public int hashCode() {
        // Note, hashcodes are the same as those used by IntHashSet
        if (hashCode == -1) {
            int h = 936247625;
            IntIterator it = iterator();
            while (it.hasNext()) {
                h += it.next();
            }
            hashCode = h;
        }
        return hashCode;
    }

    /**
     * Iterator class
     */

    private class IntArraySetIterator implements IntIterator, Serializable {

        private int i = 0;

        public IntArraySetIterator() {
            i = 0;
        }

        public boolean hasNext() {
            return i < contents.length;
        }

        public int next() {
            return contents[i++];
        }
    }

//    public static void main(String[] args) {
//        int[] a = {0,20,21,33,44};
//        int[] b = {1,5,8,12,15};
//        IntArraySet x = new IntArraySet(a).union(new IntArraySet(b));
//        String s = x.toString();
//        System.out.println(s);
//    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.