package bitronix.tm.timer;

import bitronix.tm.BitronixTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This task is used to mark a transaction as timed-out.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class TransactionTimeoutTask extends Task {

    private final static Logger log = LoggerFactory.getLogger(TransactionTimeoutTask.class);

    private BitronixTransaction transaction;

    public TransactionTimeoutTask(BitronixTransaction transaction, Date executionTime, TaskScheduler scheduler) {
        super(executionTime, scheduler);
        this.transaction = transaction;
    }

    public Object getObject() {
        return transaction;
    }

    public void execute() throws TaskException {
        if (log.isDebugEnabled()) log.debug("marking " + transaction + " as timed out");
        transaction.timeout();
    }

    public String toString() {
        return "a TransactionTimeoutTask on " + transaction + " scheduled for " + getExecutionTime();
    }

}
