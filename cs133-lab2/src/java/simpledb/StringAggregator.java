package simpledb;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private int groupField;
    private Type groupType;
    private int aggField;
    private Op oper;
    private ConcurrentHashMap<Object, Integer> runningAgg;

    private String groupFieldName;
    private String aggFieldName;

    private ArrayList<Tuple> tuples;
    private TupleDesc td;
    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("operation is not count, invalid operation");
        } else {
            groupField = gbfield;
            groupType = gbfieldtype;
            aggField = afield;
            oper = what;
            runningAgg = new ConcurrentHashMap<Object, Integer>();
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        groupFieldName = tup.getTupleDesc().getFieldName(groupField);
        aggFieldName = tup.getTupleDesc().getFieldName(aggField);

        StringField aggFieldVal = (StringField) tup.getField(aggField);
        String agg = aggFieldVal.getValue();
        Field grp = tup.getField(groupField);
        
        if (groupField == Aggregator.NO_GROUPING) {
            if (runningAgg.containsKey(0)) {
                int currentAgg = runningAgg.get(0);
                runningAgg.replace(0, currentAgg + 1);
            } else {
                // Not already here, make new group
                runningAgg.put(0, 1);
            }
        } else {
            if (runningAgg.containsKey(grp)) {
                int currentAgg = runningAgg.get(grp);
                runningAgg.replace(grp, currentAgg + 1);
            } else {
                // Not already here, make new group
                runningAgg.put(grp, 1);
            }
        }
    }

    /**
     * Returns a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        tuples = new ArrayList<Tuple>();
        Set<Object> keySet = runningAgg.keySet();

        if (groupType == Type.INT_TYPE) {
            td = new TupleDesc(new Type[]{Type.INT_TYPE, Type.INT_TYPE}, new String[]{groupFieldName, aggFieldName});
            for (Object key : keySet) {
                Tuple currentTuple = new Tuple(td);
                currentTuple.setField(0, (Field) key);
                currentTuple.setField(1, new IntField(runningAgg.get(key)));
                tuples.add(currentTuple);
            }
        } else if (groupType == Type.STRING_TYPE) {
            td = new TupleDesc(new Type[]{Type.STRING_TYPE, Type.INT_TYPE}, new String[]{groupFieldName, aggFieldName});
            for (Object key : keySet) {
                Tuple currentTuple = new Tuple(td);
                currentTuple.setField(0, (Field) key);
                currentTuple.setField(1, new IntField(runningAgg.get(key)));
                tuples.add(currentTuple);
            }
        } else {
            td = new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{aggFieldName});
            for (Object key : keySet) {
                Tuple currentTuple = new Tuple(td);
                currentTuple.setField(0, new IntField(runningAgg.get(key)));
                tuples.add(currentTuple);
            }
        }

        return new TupleIterator(td, tuples);
    }

}
