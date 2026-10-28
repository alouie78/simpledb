package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;
    private Aggregator agg;
    private DbIterator childIter;
    private DbIterator iter;
    private int aggField;
    private int gbField;
    private Aggregator.Op oper;
    private boolean allChildDone;

    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of fetchNext().
     * 
     * 
     * @param child
     *            The DbIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(DbIterator child, int afield, int gfield, Aggregator.Op aop) {
        childIter = child;
        aggField = afield;
        gbField = gfield;
        allChildDone = false;
        Type afieldtype = child.getTupleDesc().getFieldType(afield);

        if(gbField == -1) {
            if (afieldtype == Type.INT_TYPE) {
                agg = new IntegerAggregator(Aggregator.NO_GROUPING, null, afield, aop);
            } else { // == Type.STRING_TYPE
                agg = new StringAggregator(Aggregator.NO_GROUPING, null, afield, aop);
            }
        } else {
            Type gfieldtype = child.getTupleDesc().getFieldType(gfield);
            if (afieldtype == Type.INT_TYPE) {
                agg = new IntegerAggregator(gfield, gfieldtype, afield, aop);
            } else { // == Type.STRING_TYPE
                agg = new StringAggregator(gfield, gfieldtype, afield, aop);
            }
        }
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
        return gbField;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples If not, return
     *         null;
     * */
    public String groupFieldName() {
        return childIter.getTupleDesc().getFieldName(gbField);
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
        return aggField;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
        return childIter.getTupleDesc().getFieldName(aggField);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
        return oper;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
    TransactionAbortedException {
        iter = agg.iterator();
        iter.open();
        super.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate, If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     *
     * Hint: think about how many tuples you have to process from the child operator
     * before this method can return its first tuple.
     * Hint: notice that each Aggregator class has an iterator() method
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if(!allChildDone) {
            childIter.open();
            while(childIter.hasNext()) {
                Tuple childNext = childIter.next();
                agg.mergeTupleIntoGroup(childNext);
            }
            iter = agg.iterator();
            super.open();
            iter.open();
            allChildDone = true;
        }
        if(!iter.hasNext()) {
            return null;
        } else {
            return iter.next();
        }
    }

    public void rewind() throws DbException, TransactionAbortedException {
        iter.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        Type gfieldtype = childIter.getTupleDesc().getFieldType(gbField);
        String gfieldname = childIter.getTupleDesc().getFieldName(gbField);
        String aggfieldname = childIter.getTupleDesc().getFieldName(aggField);

        if (gfieldtype == Type.INT_TYPE) {
            return new TupleDesc(new Type[]{Type.INT_TYPE, Type.INT_TYPE}, new String[]{gfieldname, aggfieldname});
        } else if (gfieldtype == Type.STRING_TYPE) {
            return new TupleDesc(new Type[]{Type.STRING_TYPE, Type.INT_TYPE}, new String[]{gfieldname, aggfieldname});
        } else {
            return new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{aggfieldname});
        }
    }

    public void close() {
        super.close();
        iter.close();
    }

    /**
     * See Operator.java for additional notes
     */
    @Override
    public DbIterator[] getChildren() {
        return new DbIterator[]{childIter};
    }

    /**
     * See Operator.java for additional notes
     */
    @Override
    public void setChildren(DbIterator[] children) {
        childIter = children[0];
    }

}
