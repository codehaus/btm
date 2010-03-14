package bitronix.tm.mock.events;

/**
 * (c) Bitronix, 19-d�c.-2005
 *
 * @author lorban
 */
public class LocalRollbackEvent extends Event {

    public LocalRollbackEvent(Object source, Exception ex) {
        super(source, ex);
    }

    public String toString() {
        return "LocalRollbackEvent at " + getTimestamp();
    }

}