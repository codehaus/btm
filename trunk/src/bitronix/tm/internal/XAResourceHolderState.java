package bitronix.tm.internal;

import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.BitronixXid;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

/**
 * {@link XAResourceHolder} state container.
 * Instances are kept in the transaction and bound to / unbound from the {@link XAResourceHolder} as the
 * resource participates in different transactions. A {@link XAResourceHolder} without {@link XAResourceHolderState}
 * is considered to be in local transaction mode.
 * <p>Objects of this class also expose resource specific configuration like the unique resource name.</p>
 * <p>The {@link XAResource} state during a transaction participation is also contained: assigned XID, transaction
 * start / end state...</p>
 * <p>There is exactly one {@link XAResourceHolderState} object per {@link XAResourceHolder} per
 * {@link javax.transaction.Transaction}.</p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @see bitronix.tm.resource.common.ResourceBean
 * @author lorban
 */
public class XAResourceHolderState {

    private final static Logger log = LoggerFactory.getLogger(XAResourceHolderState.class);

    private ResourceBean bean;
    private BitronixXid xid;
    private XAResourceHolder xaResourceHolder;
    private boolean started;
    private boolean ended;
    private boolean suspended;

    public XAResourceHolderState(XAResourceHolder XAResourceHolder, ResourceBean bean) {
        this.bean = bean;
        this.xaResourceHolder = XAResourceHolder;

        started = false;
        ended = false;
        suspended = false;
        xid = null;
    }

    public BitronixXid getXid() {
        return xid;
    }

    public void setXid(BitronixXid xid) throws BitronixSystemException {
        if (log.isDebugEnabled()) log.debug("assigning <" + xid + "> to <" + this + ">");
        if (this.xid != null)
            throw new BitronixSystemException("a XID has already been assigned to " + this);
        this.xid = xid;
    }

    public XAResource getXAResource() {
        return xaResourceHolder.getXAResource();
    }

    public XAResourceHolder getXAResourceHolder() {
        return xaResourceHolder;
    }

    public String getUniqueName() {
        return bean.getUniqueName();
    }

    public boolean getUseTmJoin() {
        return bean.getUseTmJoin();
    }

    public int getCommitOrderingPosition() {
        return bean.getCommitOrderingPosition();
    }

    public boolean isEnded() {
        return ended;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void end(int flags) throws XAException {
        boolean ended = this.ended;
        boolean suspended = this.suspended;

        if (this.ended && (flags == XAResource.TMSUSPEND)) {
            if (log.isDebugEnabled()) log.debug("resource already ended, changing state to suspended: " + this);
            this.suspended = true;
            return;
        }

        if (this.ended)
            throw new BitronixXAException("resource already ended: " + this, XAException.XAER_PROTO);

        if (flags == XAResource.TMSUSPEND) {
            if (!this.started)
                throw new BitronixXAException("resource hasn't been started, cannot suspend it: " + this, XAException.XAER_PROTO);
            if (this.suspended)
                throw new BitronixXAException("resource already suspended: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) log.debug("suspending " + this);
            suspended = true;
        }
        else {
            if (log.isDebugEnabled()) log.debug("ending " + this);
            ended = true;
        }

        getXAResource().end(xid, flags);
        this.suspended = suspended;
        this.ended = ended;
        this.started = false;
    }

    public void start(int flags) throws XAException {
        boolean suspended = this.suspended;
        boolean started = this.started;

        if (this.ended && (flags == XAResource.TMRESUME)) {
            if (log.isDebugEnabled()) log.debug("resource already ended, changing state to resumed: " + this);
            this.suspended = false;
            return;
        }

        if (flags == XAResource.TMRESUME) {
            if (!this.suspended)
                throw new BitronixXAException("resource hasn't been suspended, cannot resume it: " + this, XAException.XAER_PROTO);
            if (!this.started)
                throw new BitronixXAException("resource hasn't been started, cannot resume it: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) log.debug("resuming " + this);
            suspended = false;
        }
        else {
            if (this.started)
                throw new BitronixXAException("resource already started: " + this, XAException.XAER_PROTO);

            if (log.isDebugEnabled()) log.debug("starting " + this);
            started = true;
        }

        getXAResource().start(xid, flags);
        this.suspended = suspended;
        this.started = started;
        this.ended = false;
    }

    public String toString() {
        return "an XAResourceHolderState with uniqueName=" + bean.getUniqueName() +
                " XAResource=" + getXAResource() +
                (started ? " (started)":"") +
                (ended ? " (ended)":"") +
                (suspended ? " (suspended)":"") +
                " with XID " + xid;
    }

}
