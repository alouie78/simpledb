package simpledb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

/**
 * Computes some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private int groupField;
    private Type groupType;
    private int aggField;
    private Op oper;
    private ConcurrentHashMap<Object, Integer> runningAgg;
    private ConcurrentHashMap<Object, Integer> avgCount;

    private String groupFieldName;
    private String aggFieldName;

    private ArrayList<Tuple> tuples;
    private TupleDesc td;


    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        groupField = gbfield;
        groupType = gbfieldtype;
        aggField = afield;
        oper = what;
        runningAgg = new ConcurrentHashMap<Object, Integer>();
        avgCount = new ConcurrentHashMap<Object, Integer>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor. See Aggregator.java for more.
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {

        aggFieldName = tup.getTupleDesc().getFieldName(aggField);

        IntField aggFieldVal = (IntField) tup.getField(aggField);
        int agg = aggFieldVal.getValue();
        
        if (groupField == Aggregator.NO_GROUPING) {
            if (runningAgg.containsKey(0)) {
                if(oper == Op.COUNT) {
                    int currentAgg = runningAgg.get(0);
                    runningAgg.replace(0, currentAgg + 1);
                } else if(oper == Op.AVG) {
                    // Increment both count and sum
                    int currentCt = avgCount.get(0);
                    avgCount.replace(0, currentCt + 1);

                    // Keep track of sum
                    int currentAgg = runningAgg.get(0);
                    runningAgg.replace(0, currentAgg + agg);
                } else if(oper == Op.MAX) {
                    int currentAgg = runningAgg.get(0);
                    runningAgg.replace(0, Math.max(currentAgg, agg));
                } else if(oper == Op.MIN) {
                    int currentAgg = runningAgg.get(0);
                    runningAgg.replace(0, Math.min(currentAgg, agg));
                } else { // oper == Op.SUM
                    int currentAgg = runningAgg.get(0);
                    runningAgg.replace(0, currentAgg + agg);
                }

            } else {
                // Not already here, make new group
                if(oper == Op.COUNT) {
                    runningAgg.put(0, 1);
                } else if (oper == Op.AVG) { 
                    runningAgg.put(0, agg);
                    avgCount.put(0, 1);
                } else {
                    runningAgg.put(0, agg);
                }
            }
        } else {
            groupFieldName = tup.getTupleDesc().getFieldName(groupField);
            Field grp = tup.getField(groupField);
            if (runningAgg.containsKey(grp)) {
                if(oper == Op.COUNT) {
                    int currentAgg = runningAgg.get(grp);
                    runningAgg.replace(grp, currentAgg + 1);
                } else if(oper == Op.AVG) {
                    // Increment both count and sum
                    int currentCt = avgCount.get(grp);
                    avgCount.replace(grp, currentCt + 1);

                    // Keep track of sum
                    int currentAgg = runningAgg.get(grp);
                    runningAgg.replace(grp, currentAgg + agg);
                } else if(oper == Op.MAX) {
                    int currentAgg = runningAgg.get(grp);
                    runningAgg.replace(grp, Math.max(currentAgg, agg));
                } else if(oper == Op.MIN) {
                    int currentAgg = runningAgg.get(grp);
                    runningAgg.replace(grp, Math.min(currentAgg, agg));
                } else { // oper == Op.SUM
                    int currentAgg = runningAgg.get(grp);
                    runningAgg.replace(grp, currentAgg + agg);
                }

            } else {
                // Not already here, make new group
                if(oper == Op.COUNT) {
                    runningAgg.put(grp, 1);
                } else if (oper == Op.AVG) { 
                    runningAgg.put(grp, agg);
                    avgCount.put(grp, 1);
                } else {
                    runningAgg.put(grp, agg);
                }
            }
        }
    }

    /**
     * Returns a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public DbIterator iterator() {
        tuples = new ArrayList<Tuple>();
        Set<Object> keySet = runningAgg.keySet();

        if (groupType == Type.INT_TYPE) {
            td = new TupleDesc(new Type[]{Type.INT_TYPE, Type.INT_TYPE}, new String[]{groupFieldName, aggFieldName});

            if (oper == Op.AVG) {
                for (Object key : keySet) {
                    Tuple currentTuple = new Tuple(td);
                    currentTuple.setField(0, (Field) key);
                    currentTuple.setField(1, new IntField(runningAgg.get(key) / avgCount.get(key)));
                    tuples.add(currentTuple);
                }
            } else {
                for (Object key : keySet) {
                    Tuple currentTuple = new Tuple(td);
                    currentTuple.setField(0, (Field) key);
                    currentTuple.setField(1, new IntField(runningAgg.get(key)));
                    tuples.add(currentTuple);
                }
            }

        } else if (groupType == Type.STRING_TYPE) {
            td = new TupleDesc(new Type[]{Type.STRING_TYPE, Type.INT_TYPE}, new String[]{groupFieldName, aggFieldName});

            if (oper == Op.AVG) {
                for (Object key : keySet) {
                    Tuple currentTuple = new Tuple(td);
                    currentTuple.setField(0, (Field) key);
                    currentTuple.setField(1, new IntField(runningAgg.get(key) / avgCount.get(key)));
                    tuples.add(currentTuple);
                }
            } else {
                for (Object key : keySet) {
                    Tuple currentTuple = new Tuple(td);
                    currentTuple.setField(0, (Field) key);
                    currentTuple.setField(1, new IntField(runningAgg.get(key)));
                    tuples.add(currentTuple);
                }
            }
        } else {
            td = new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{aggFieldName});

            if (oper == Op.AVG) {
                for (Object key : keySet) {
                    Tuple currentTuple = new Tuple(td);
                    currentTuple.setField(0, new IntField(runningAgg.get(key) / avgCount.get(key)));
                    tuples.add(currentTuple);
                }
            } else {
                for (Object key : keySet) {
                    Tuple currentTuple = new Tuple(td);
                    currentTuple.setField(0, new IntField(runningAgg.get(key)));
                    tuples.add(currentTuple);
                }
            }
        }

        return new TupleIterator(td, tuples);
    }

}
