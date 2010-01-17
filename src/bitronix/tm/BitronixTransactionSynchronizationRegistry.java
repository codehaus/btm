package bitronix.tm;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.utils.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of JTA 1.1 {@link TransactionSynchronizationRegistry}.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry, Referenceable {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionSynchronizationRegistry.class);

    private static BitronixTransactionManager transactionManager;
    private final static ThreadLocal resourcesTl = new ThreadLocal();


    public BitronixTransactionSynchronizationRegistry() {
        transactionManager = TransactionManagerServices.getTransactionManager();
        resourcesTl.set(new HashMap());
    }

    public Object getResource(Object key) {
        try {
            if (key == null)
                throw new NullPointerException("key cannot be null");
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on curent thread");

            return getResources().get(key);
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    public boolean getRollbackOnly() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on curent thread");

            return currentTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    public Object getTransactionKey() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                return null;

            return currentTransaction().getGtrid();
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    public int getTransactionStatus() {
        try {
            if (currentTransaction() == null)
                return Status.STATUS_NO_TRANSACTION;

            return currentTransaction().getStatus();
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    public void putResource(Object key, Object value) {
        try {
            if (key == null)
                throw new NullPointerException("key cannot be null");
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on curent thread");

            getResources().put(key, value);

            if (getResources().size() == 1) {
                if (log.isDebugEnabled()) log.debug("first resource put in synchronization registry, registering a ClearRegistryResourcesSynchronization");
                Synchronization synchronization = new ClearRegistryResourcesSynchronization();
                currentTransaction().getSynchronizationScheduler().add(synchronization, Scheduler.ALWAYS_LAST_POSITION);
            }
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    public void registerInterposedSynchronization(Synchronization synchronization) {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on curent thread");
            if (    currentTransaction().getStatus() == Status.STATUS_PREPARING ||
                    currentTransaction().getStatus() == Status.STATUS_PREPARED ||
                    currentTransaction().getStatus() == Status.STATUS_COMMITTING ||
                    currentTransaction().getStatus() == Status.STATUS_COMMITTED ||
                    currentTransaction().getStatus() == Status.STATUS_ROLLING_BACK ||
                    currentTransaction().getStatus() == Status.STATUS_ROLLEDBACK
                    )
                throw new IllegalStateException("transaction is done, cannot register an interposed synchronization");

            currentTransaction().getSynchronizationScheduler().add(synchronization, Scheduler.DEFAULT_POSITION +1);
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get current transaction status");
        }
    }

    public void setRollbackOnly() {
        try {
            if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
                throw new IllegalStateException("no transaction started on curent thread");

            currentTransaction().setStatus(Status.STATUS_MARKED_ROLLBACK);
        } catch (SystemException e) {
            throw new BitronixRuntimeException("cannot get or set current transaction status");
        }
    }

    private Map getResources() {
        return ((Map) resourcesTl.get());
    }

    private BitronixTransaction currentTransaction() {
        return transactionManager.getCurrentTransaction();
    }

    public Reference getReference() throws NamingException {
        return new Reference(
                BitronixTransactionManager.class.getName(),
                new StringRefAddr("TransactionSynchronizationRegistry", "BitronixTransactionSynchronizationRegistry"),
                BitronixTransactionSynchronizationRegistryObjectFactory.class.getName(),
                null
        );
    }

    private class ClearRegistryResourcesSynchronization implements Synchronization {
        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            if (log.isDebugEnabled()) log.debug("clearing resources");
            getResources().clear();
        }
    }

}
