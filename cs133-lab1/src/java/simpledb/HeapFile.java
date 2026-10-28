package simpledb;

import java.io.*;
import java.util.*;

import simpledb.HeapPage.HeapPageIter;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    public class HeapFileIterator implements DbFileIterator{

        private TransactionId tId;
        private HeapPage currentPage;
        private Iterator<Tuple> currentTuple;
        private int pageNum;
        private int tupleNum;
        private boolean opened;
        private BufferPool bP;

        public HeapFileIterator(TransactionId tid) {
            tId = tid;
            pageNum = 0;
        }

        /**
         * Opens the iterator
         * @throws DbException when there are problems opening/accessing the database.
         */
        public void open() throws DbException, TransactionAbortedException {
            opened = true;

            // set the iterators to the beginning of the file
            bP = Database.getBufferPool();
            PageId pid = new HeapPageId(tableId, pageNum);
            currentPage = (HeapPage) bP.getPage(tId, pid, Permissions.READ_ONLY);
            if (currentPage == null) {
                currentTuple = null;
            } else {
                currentTuple = currentPage.iterator();
            }
        }

        /** @return true if there are more tuples available, false if no more tuples
         * @throws IllegalStateException If the iterator has not been opened 
         */
        public boolean hasNext() 
            throws DbException, TransactionAbortedException, IllegalStateException { 
            if(!opened) {
                throw new IllegalStateException("Iterator has not been opened.");
            } else {
                if (currentTuple != null && currentTuple.hasNext()) {
                    return true;
                } else {
                    pageNum++;
                    while(pageNum < numPages) {
                        PageId nextPid = new HeapPageId(tableId, pageNum);
                        currentPage = (HeapPage) bP.getPage(tId, nextPid, Permissions.READ_ONLY);
                        currentTuple = currentPage.iterator();
                        if (currentTuple.hasNext()) {
                            return true;
                        } else {
                            pageNum++;
                        }
                    }
                    return false;
                }
            }
            
        }

        /**
         * Gets the next tuple from the operator (typically implementing by reading
         * from a child operator or an access method).
         *
         * @return The next tuple in the iterator.
         * @throws IllegalStateException If the iterator has not been opened
         * @throws NoSuchElementException if there are no more tuples
         */
        public Tuple next() 
            throws DbException, TransactionAbortedException, NoSuchElementException {
            
            if (!hasNext()) {
                throw new NoSuchElementException("No more tuples to iterate through");
            } else {
                return currentTuple.next();
            } 
        }

        /**
         * Resets the iterator to the start.
         * @throws DbException When rewind is unsupported.
         */
        public void rewind() throws DbException, TransactionAbortedException {
            pageNum = 0;
            PageId pid = new HeapPageId(tableId, pageNum);
            currentPage = (HeapPage) bP.getPage(tId, pid, Permissions.READ_ONLY);
            if (currentPage == null) {
                currentTuple = null;
            } else {
                currentTuple = currentPage.iterator();
            }
        }

        /**
         * Closes the iterator.
         */
        public void close() {
            opened = false;
        }
    }

    private File file;
    private TupleDesc tupleDesc;
    private int tableId;
    private int pageSize;
    private int numPages;

    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        file = f;
        tupleDesc = td;
        tableId = f.getAbsoluteFile().hashCode();
        pageSize = BufferPool.getPageSize();
        numPages = (int) Math.ceil((double) f.length() / pageSize);
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return tableId;
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return tupleDesc;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) throws IllegalArgumentException {
        int pageNum = pid.getPageNumber();
        long offset = pageNum * pageSize;
        byte[] pageData = new byte[pageSize];
        
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(offset);
            int dataRead = raf.read(pageData);
            return new HeapPage((HeapPageId) pid, pageData);
        } catch(Exception e) {
            throw new IllegalArgumentException("Page could not be read. " +
                "Exception: " + e.toString());
        }
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        return numPages;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
    TransactionAbortedException {
        // TODO: some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        return new HeapFileIterator(tid);
    }

}

