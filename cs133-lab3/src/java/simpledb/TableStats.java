package simpledb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query. 
 * 
 * This class is not needed in implementing lab1 and lab2.
 */
public class TableStats {
    // Note: the constructor you will implement is below these static methods

    private static final ConcurrentHashMap<String, TableStats> statsMap = new ConcurrentHashMap<String, TableStats>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }

    public static void setStatsMap(HashMap<String,TableStats> s)
    {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;

    // instance variables
    int tableID;
    int IOCostPerPage;
    ConcurrentHashMap<String, Object> tableHists = new ConcurrentHashMap<String, Object>();
    TupleDesc tupleDesc;
    int numTuples;

    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     * 
     * @param tableid
     *            The table over which to compute statistics
     * @param ioCostPerPage
     *            The cost per page of IO. This doesn't differentiate between
     *            sequential-scan IO and disk seeks.
     */
    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // Note: See project description for hint on using a Transaction

        // TODO: some code goes here
        tableID = tableid;
        IOCostPerPage = ioCostPerPage;
        numTuples = 0;

        try {
            Transaction t = new Transaction();
            t.start();
            SeqScan s = new SeqScan(t.getId(), tableid, "t");
            tupleDesc = s.getTupleDesc();

            s.open();

            ConcurrentHashMap<String, Integer> minByCol = new ConcurrentHashMap<String, Integer>();    
            ConcurrentHashMap<String, Integer> maxByCol = new ConcurrentHashMap<String, Integer>();    
            
            while (s.hasNext()) {
                Tuple tuple = s.next();
                numTuples++;

                // populate mins and maxs
                for (int i = 0; i < tupleDesc.numFields(); i++) {
                    Field f = tuple.getField(i);

                    if (f.getType() == Type.INT_TYPE) {
                        IntField intField = (IntField) f;
                        int value = intField.getValue();

                        if (minByCol.containsKey(tupleDesc.getFieldName(i))) {
                            if (minByCol.get(tupleDesc.getFieldName(i)) > value) {
                                minByCol.put(tupleDesc.getFieldName(i), value);
                            }
                        } else {
                            minByCol.put(tupleDesc.getFieldName(i), value);
                        }
                        
                        if (maxByCol.containsKey(tupleDesc.getFieldName(i))) {
                            if (maxByCol.get(tupleDesc.getFieldName(i)) < value) {
                                maxByCol.put(tupleDesc.getFieldName(i), value);
                            }
                        } else {
                            maxByCol.put(tupleDesc.getFieldName(i), value);
                        }
                    }
                }
            }

            for (int i = 0; i < tupleDesc.numFields(); i++) {
                String field = tupleDesc.getFieldName(i);
                Object fieldHist;

                if (tupleDesc.getFieldType(i) == Type.INT_TYPE) {
                    fieldHist = new IntHistogram(NUM_HIST_BINS, minByCol.get(field), maxByCol.get(field));
                } else {
                    fieldHist = new StringHistogram(NUM_HIST_BINS);
                }

                tableHists.put(field, fieldHist);
            }

            // rewind to scan again
            s.rewind();
            s.open();

            while (s.hasNext()) {
                Tuple tuple = s.next();
                
                // populate mins and maxs
                for (int i = 0; i < tupleDesc.numFields(); i++) {
                    Field f = tuple.getField(i);
                    String field = tupleDesc.getFieldName(i);

                    if (f.getType() == Type.INT_TYPE) {
                        IntField intField = (IntField) f;
                        int value = intField.getValue();

                        IntHistogram currentHist = (IntHistogram) tableHists.get(field);
                        currentHist.addValue(value);

                        tableHists.put(field, currentHist);
                    } else {
                        StringField stringField = (StringField) f;
                        String value = stringField.getValue();

                        StringHistogram currentHist = (StringHistogram) tableHists.get(field);
                        currentHist.addValue(value);

                        tableHists.put(field, currentHist);
                    }
                }
            }

            t.commit(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * 
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     * 
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        HeapFile db = (HeapFile) Database.getCatalog().getDatabaseFile(tableID);   
        return (double) db.numPages() * IOCostPerPage;
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     * 
     * @param selectivityFactor
     *            The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        return (int) (numTuples * selectivityFactor);
    }

    /**
     * The average selectivity of the field under op.
     * @param field
     *        the index of the field
     * @param op
     *        the operator in the predicate
     * The semantic of the method is that, given the table, and then given a
     * tuple, of which we do not know the value of the field, return the
     * expected selectivity. You may estimate this value from the histograms.
     *
     * Not necessary for lab 3
     * */
    public double avgSelectivity(int field, Predicate.Op op) {
        // TODO: some code goes here
        return 0.5;
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     * 
     * @param field
     *            The field over which the predicate ranges
     * @param op
     *            The logical operation in the predicate
     * @param constant
     *            The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        String fieldName = tupleDesc.getFieldName(field);
        
        if (tupleDesc.getFieldType(field) == Type.INT_TYPE) {
            IntField intField = (IntField) constant;

            IntHistogram currentHist = (IntHistogram) tableHists.get(fieldName);
            return currentHist.estimateSelectivity(op, intField.getValue());
        } else {
            StringField stringField = (StringField) constant;

            StringHistogram currentHist = (StringHistogram) tableHists.get(fieldName);
            return currentHist.estimateSelectivity(op, stringField.getValue());
        }
    }

    /**
     * return the total number of tuples in this table
     * */
    public int totalTuples() {
        return numTuples;
    }

}
