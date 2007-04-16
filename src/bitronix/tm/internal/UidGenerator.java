package bitronix.tm.internal;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;

/**
 * Helper that offers UID generation (GTRID, XID, sequences) needed by the transaction manager.
 * <p>Generated UIDs are at most 64 bytes long and are made of 3 subparts: the current time in milliseconds since
 * Epoch, a JVM transient atomic sequence number and the configured <code>bitronix.tm.serverId</code>.</p>
 * <p>The reliance on the system clock is critical to the uniqueness of the UID in the network so you have to make sure
 * all servers of the network running this transaction manager have their clock reasonably in sync. An order of 1
 * second synchronicity is generally fine.</p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class UidGenerator {

    /**
     * Maximum serverId length.
     */
    public final static int MAX_SERVER_ID_LENGTH = 51;

    private static int sequenceNumber = 0;
    private static byte[] serverId;

    static {
        serverId = TransactionManagerServices.getConfiguration().buildServerIdArray();
        if (serverId.length > MAX_SERVER_ID_LENGTH) {
            byte[] truncatedServerId = new byte[MAX_SERVER_ID_LENGTH];
            System.arraycopy(serverId, 0, truncatedServerId, 0, MAX_SERVER_ID_LENGTH);
            serverId = truncatedServerId;
        }
    }

    /**
     * Generate a UID, globally unique. This method relies on the configured serverId for network uniqueness.
     * @return the generated UID.
     */
    public static Uid generateUid() {
        byte[] timestamp = Encoder.longToBytes(System.currentTimeMillis());
        byte[] sequence = Encoder.intToBytes(getNextSequenceNumber());

        int uidLength = serverId.length + timestamp.length + sequence.length;
        byte[] uidArray = new byte[uidLength];

        System.arraycopy(serverId, 0, uidArray, 0, serverId.length);
        System.arraycopy(timestamp, 0, uidArray, serverId.length, timestamp.length);
        System.arraycopy(sequence, 0, uidArray, serverId.length + timestamp.length, sequence.length);

        return new Uid(uidArray);
    }

    /**
     * Atomically generate general-purpose sequence numbers starting at 0. The counter is reset at every
     * JVM startup.
     * @return a sequence number unique for the lifespan of this JVM.
     */
    public static synchronized int getNextSequenceNumber() {
        return sequenceNumber++;
    }

    /**
     * Generate a XID with the specified globalTransactionId.
     * @param gtrid the GTRID to use to generate the Xid.
     * @return the generated Xid.
     */
    public static BitronixXid generateXid(Uid gtrid) {
        return new BitronixXid(gtrid, generateUid());
    }

}
