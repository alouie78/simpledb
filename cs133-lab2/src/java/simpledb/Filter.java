package simpledb;

import java.util.*;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {

    private static final long serialVersionUID = 1L;
    private TupleDesc td;
    private Predicate pred;
    private DbIterator childIter;

    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     * 
     * @param p
     *            The predicate to filter tuples with
     * @param child
     *            The child operator
     */
    public Filter(Predicate p, DbIterator child) {
        td = child.getTupleDesc();
        pred = p;
        childIter = child;
    }

    public Predicate getPredicate() {
        return pred;
    }

    public TupleDesc getTupleDesc() {
        return td;
    }

    public void open() throws DbException, NoSuchElementException,
    TransactionAbortedException {
        childIter.open();
        super.open();
    }

    public void close() {
        super.close();
        childIter.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        childIter.rewind();
    }

    /**
     * The Filter operator iterates through the tuples from its child, 
     * applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     * This method returns the next tuple.
     * 
     * @return The next tuple that passes the filter, or null if there are no
     *         more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
    TransactionAbortedException, DbException {
        while (childIter.hasNext()) {
            Tuple t = childIter.next();
            if(pred.filter(t)) {
                return t;
            }
        }
        return null;
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
