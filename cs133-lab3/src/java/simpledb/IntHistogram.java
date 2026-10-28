package simpledb;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

    private int numBuckets;
    private int bucketSize;
    private int lastBucketSize;
    private int minVal;
    private int maxVal;
    private int[] histogram;
    private int numTuples;

    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     *
     * Note: if the number of buckets exceeds the number of distinct integers between min and max, 
     * some buckets may remain empty (don't create buckets with non-integer widths).
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
        numBuckets = buckets;
        minVal = min;
        maxVal = max;
        bucketSize = (max - min + 1) / numBuckets;
        lastBucketSize = bucketSize + ((max - min + 1) % numBuckets);

        // 1 bucket for each value
        if(max - min + 1 < numBuckets) {
            bucketSize = 1;
            lastBucketSize = 1;
            numBuckets = max - min + 1;
        }

        histogram = new int[numBuckets];
        numTuples = 0;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
        int bucketNum = (v - minVal) / bucketSize;

        if(bucketNum > numBuckets - 1) {
            // Last bucket - assume we won't call addValue() on illegal value
            histogram[numBuckets - 1]++;
        } else {
            histogram[bucketNum]++;
        }
        numTuples++;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
        if (op == Predicate.Op.EQUALS) {
            // Value outside legal range
            if(v < minVal || v > maxVal) {
                return 0.0;
            }

            int bucketNum = (v - minVal) / bucketSize;
            int width = bucketSize;
            if(bucketNum == numBuckets - 1) {
                width = lastBucketSize;
            }

            if(bucketNum > numBuckets - 1) {
                // Last bucket
                return (((double) histogram[numBuckets - 1]) / width) / numTuples; 
            } else {
                return (((double) histogram[bucketNum]) / width) / numTuples; 
            }

        } else if (op == Predicate.Op.GREATER_THAN) {
            // Value outside legal range
            if (v < minVal) {
                return 1.0;
            } else if(v >= maxVal) {
                return 0.0;
            }

            int bucketNum = (v - minVal) / bucketSize;
            double bucketFrac = ((double) histogram[bucketNum]) / numTuples;

            int lastVal = (bucketNum + 1) * bucketSize + minVal - 1;
            int width = bucketSize;
            if(bucketNum == numBuckets - 1) {
                width = lastBucketSize;
            }
            double bucketPart = ((double) (lastVal - v)) / width;

            double selectivity = bucketFrac * bucketPart;

            // Add other buckets greater than this one
            for(int b = bucketNum + 1; b < numBuckets; b++) {
                if(b == numBuckets - 1) {
                    selectivity += (((double) histogram[b]) / lastBucketSize) / numTuples;
                } else {
                    selectivity += (((double) histogram[b]) / bucketSize) / numTuples; 
                }
            }

            return selectivity;

        // Other cases - can all be rewritten in terms of equals/greater than
        } else if (op == Predicate.Op.LESS_THAN) {
            return 1.0 - estimateSelectivity(Predicate.Op.GREATER_THAN, v) - estimateSelectivity(Predicate.Op.EQUALS, v);
        } else if (op == Predicate.Op.LESS_THAN_OR_EQ) {
            return 1.0 - estimateSelectivity(Predicate.Op.GREATER_THAN, v);
        } else if (op == Predicate.Op.GREATER_THAN_OR_EQ) {
            return 1.0 - estimateSelectivity(Predicate.Op.LESS_THAN, v);
        } else if (op == Predicate.Op.LIKE) {
            return estimateSelectivity(Predicate.Op.EQUALS, v);
        } else {
            // op == Predicate.Op.NOT_EQUALS
            return 1.0 - estimateSelectivity(Predicate.Op.EQUALS, v);
        }
    }

    /**
     * @return
     *     the average selectivity of this histogram.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It could be used to
     *     implement a more efficient optimization
     *
     * Not necessary for lab 3
     * */
    public double avgSelectivity()
    {
        // TODO: some code goes here
        return 0.5;
    }

    /**
     * (Optional) A String representation of the contents of this histogram
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // TODO: some code goes here
        return null;
    }
}
