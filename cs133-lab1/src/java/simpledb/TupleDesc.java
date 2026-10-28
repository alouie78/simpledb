package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * */
        public final Type fieldType;

        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        return itemAr.iterator();
    }

    private static final long serialVersionUID = 1L;
    private ArrayList<TDItem> itemAr;
    private int tupleLength;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // Pass in initial capacity typeAr.length (shouldn't need to resize)
        itemAr = new ArrayList<TDItem>(typeAr.length);
        tupleLength = typeAr.length;

        // transfer (type, field)s into itemAr
        for (int i = 0; i < typeAr.length; i++) {
            itemAr.add(i, new TDItem(typeAr[i], fieldAr[i]));
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // Pass in initial capacity typeAr.length (shouldn't need to resize)
        itemAr = new ArrayList<TDItem>(typeAr.length);
        tupleLength = typeAr.length;

        // transfer (type, field)s into itemAr
        for (int i = 0; i < typeAr.length; i++) {
            itemAr.add(i, new TDItem(typeAr[i], null));
        }
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        return itemAr.size();
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     * 
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        if (i < 0 || i >= tupleLength) {
            throw new NoSuchElementException("field index " + i + 
                " out of bounds for length " + tupleLength + ", no such element");
        } else {
            return itemAr.get(i).fieldName;
        }
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     * 
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        if (i < 0 || i >= tupleLength) {
            throw new NoSuchElementException("field index " + i + 
                " out of bounds for length " + tupleLength + ", no such element");
        } else {
            return itemAr.get(i).fieldType;
        }    
    }

    /**
     * Find the index of the field with a given name.
     * No match if name is null.
     * 
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        if (name == null) {
            throw new NoSuchElementException("could not find field with name null");
        }

        int i = 0;
        for (TDItem item : itemAr) {
            if (item.fieldName != null && item.fieldName.equals(name)) {
                return i;
            }
            i++;
        }
        throw new NoSuchElementException("could not find field with name " + name);
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     * @see Type#getSizeInBytes
     */
    public int getSizeInBytes() {
        int numBytes = 0;
        for (TDItem item : itemAr) {
            int size = item.fieldType.getSizeInBytes();
            numBytes += size;
        }
        return numBytes;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     * 
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        int combinedLength = td1.tupleLength + td2.tupleLength;

        // Combine types and names into new arrays, then create TupleDesc
        Type[] combinedTypes = new Type[combinedLength];
        String[] combinedNames = new String[combinedLength];

        for (int i = 0; i < td1.tupleLength; i++) {
            combinedTypes[i] = td1.getFieldType(i);
            combinedNames[i] = td1.getFieldName(i);
        }
        for (int i = td1.tupleLength; i < combinedLength; i++) {
            combinedTypes[i] = td2.getFieldType(i - td1.tupleLength);
            combinedNames[i] = td2.getFieldName(i - td1.tupleLength);
        }

        return new TupleDesc(combinedTypes, combinedNames);
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i. It does not matter if the field names are equal.
     * 
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */
    public boolean equals(Object o) {
        if (!(o instanceof TupleDesc)) {
            return false;
        } else if (((TupleDesc) o).tupleLength != this.tupleLength) {
            return false;
        } else {
            TupleDesc other = (TupleDesc) o;
            for (int i = 0; i < this.tupleLength; i++) {
                if (this.getFieldType(i) != other.getFieldType(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldName[0](fieldType[0]), ..., fieldName[M](fieldType[M])", although
     * the exact format does not matter.
     * 
     * @return String describing this descriptor.
     */
    public String toString() {
        String str = "";
        for(TDItem item : itemAr) {
            str = str + item.fieldName + "(" + item.fieldType + "), ";
        }

        // Cut off final two characters if we added them
        if (str.length() == 0) {
            return str;
        } else {
            return str.substring(0, str.length() - 2);
        }
    }
}
