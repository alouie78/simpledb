package simpledb;

import java.io.IOException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId transId;
    private int tableId;
    private DbIterator childIter;
    private boolean nextCalled;
    private TupleDesc tdFinal;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     * 
     * @param t
     *            The transaction this delete runs in
     * @param child
     *            The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, DbIterator child) {
        transId = t;
        childIter = child;
        nextCalled = false;

        Type[] typeAr = new Type[]{Type.INT_TYPE};
        tdFinal = new TupleDesc(typeAr);
    }

    public TupleDesc getTupleDesc() {
        return tdFinal;
    }

    public void open() throws DbException, TransactionAbortedException {
        childIter.open();
        super.open();
    }

    public void close() {
        super.close();
        childIter.close();
    }
    /**
     * You can just close and then open the child
     */
    public void rewind() throws DbException, TransactionAbortedException {
        childIter.rewind();
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method. You can pass along the TransactionId from the constructor.
     * This operator should keep track of whether its fetchNext() method has been called already. 
     * 
     * @return A 1-field tuple containing the number of deleted records (even if there are 0)
     *          or null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
       if(nextCalled) {
            return null;
        } else {
            BufferPool bp = Database.getBufferPool();
            int counter = 0;
                
            while(childIter.hasNext()) {
                try {
                    bp.deleteTuple(transId, childIter.next());
                    counter++;
                } catch(Exception e) {
                    throw new DbException("Tuples could not be deleted");
                }
            }
            
            nextCalled = true;

            Tuple newTuple = new Tuple(tdFinal);
            newTuple.setField(0, new IntField(counter));
            return newTuple;
        }
    }

    @Override
    public DbIterator[] getChildren() {
        return new DbIterator[]{childIter};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        childIter = children[0];
    }

}
