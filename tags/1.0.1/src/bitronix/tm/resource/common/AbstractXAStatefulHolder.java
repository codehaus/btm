package bitronix.tm.resource.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bitronix.tm.internal.Decoder;

import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of all services required by a {@link XAStatefulHolder}.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public abstract class AbstractXAStatefulHolder implements XAStatefulHolder {

    private final static Logger log = LoggerFactory.getLogger(AbstractXAStatefulHolder.class);

    protected int state = STATE_IN_POOL;
    protected List stateChangeEventListeners = new ArrayList();


    public int getState() {
        return state;
    }

    public void setState(int state) {
        int oldState = this.state;
        if (oldState == state)
            throw new IllegalArgumentException("cannot switch state from " + Decoder.decodeXAStatefulHolderState(oldState) +
                    " to " + Decoder.decodeXAStatefulHolderState(state));

        if (log.isDebugEnabled()) log.debug("state changing from " + Decoder.decodeXAStatefulHolderState(oldState) +
                " to " + Decoder.decodeXAStatefulHolderState(state) + " in " + this);

        this.state = state;
        fireStateChange(oldState, state);
    }

    public void addStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.add(listener);
    }

    public void removeStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.remove(listener);
    }

    private void fireStateChange(int oldState, int newState) {
        if (log.isDebugEnabled()) log.debug("notifying " + stateChangeEventListeners.size() +
                " stateChangeEventListener(s) about state change from " + Decoder.decodeXAStatefulHolderState(oldState) +
                " to " + Decoder.decodeXAStatefulHolderState(newState) + " in " + this);

        for (int i = 0; i < stateChangeEventListeners.size(); i++) {
            StateChangeListener stateChangeListener = (StateChangeListener) stateChangeEventListeners.get(i);
            stateChangeListener.stateChanged(this, oldState, newState);
        }
    }
}
