package bitronix.tm.internal;

import bitronix.tm.utils.Decoder;

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import java.util.ArrayList;
import java.util.List;

/**
 * Subclass of {@link javax.transaction.SystemException} supporting nested {@link Throwable}s.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixMultiSystemException extends BitronixSystemException {

    private List exceptions = new ArrayList();
    private List resourceStates = new ArrayList();

    public BitronixMultiSystemException(String string, List exceptions, List resourceStates) {
        super(string);
        this.exceptions = exceptions;
        this.resourceStates = resourceStates;
    }

    public String getMessage() {
        StringBuffer errorMessage = new StringBuffer();
        errorMessage.append("collected ");
        errorMessage.append(exceptions.size());
        errorMessage.append(" exception(s):");
        for (int i = 0; i < exceptions.size(); i++) {
            errorMessage.append(System.getProperty("line.separator"));
            Throwable throwable = (Throwable) exceptions.get(i);
            String message = throwable.getMessage();
            XAResourceHolderState holderState = (XAResourceHolderState) resourceStates.get(i);

            if (holderState != null) {
                errorMessage.append(" [");
                errorMessage.append(holderState.getUniqueName());
                errorMessage.append(" - ");
            }
            errorMessage.append(throwable.getClass().getName());
            if (throwable instanceof XAException) {
                XAException xaEx = (XAException) throwable;
                errorMessage.append("(");
                errorMessage.append(Decoder.decodeXAExceptionErrorCode(xaEx));
                errorMessage.append(")");
            }
            errorMessage.append(" - ");
            errorMessage.append(message);
            errorMessage.append("]");
        }

        return errorMessage.toString();
    }

    public boolean isUnilateralRollback() {
        for (int i = 0; i < exceptions.size(); i++) {
            Throwable throwable = (Throwable) exceptions.get(i);
            if (!(throwable instanceof BitronixRollbackSystemException))
                return false;
        }
        return true;
    }

    /**
     * Get the list of exceptions that have been thrown during execution.
     * @return the list of exceptions that have been thrown during execution.
     */
    public List getExceptions() {
        return exceptions;
    }

    /**
     * Get the list of XAResourceHolderStates which threw an exception during execution.
     * This list always contains exactly one resource per exception present in {@link #getExceptions} list.
     * Indices of both list always match a resource against the exception it threw.
     * @return the list of resource which threw an exception during execution.
     */
    public List getResourceStates() {
        return resourceStates;
    }

}
