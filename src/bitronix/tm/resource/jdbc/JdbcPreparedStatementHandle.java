package bitronix.tm.resource.jdbc;

import java.sql.*;
import java.util.Arrays;

/**
 * Caching {@link PreparedStatement} wrapper.
 * <p/>
 * This class is a proxy handler for a PreparedStatement.  It does not
 * implement the PreparedStatement interface or extend a class directly,
 * but you methods implemented here will override those of the
 * underlying delegate.  Simply implement a method with the same
 * signature, and the local method will be called rather than the delegate.
 * <p/>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban, brettw
 */
public class JdbcPreparedStatementHandle extends BaseProxyHandlerClass { // implements PreparedStatement

    private PreparedStatement delegate;
    private boolean pretendClosed = false;

    // The 'parent' connection. Used to return the connection to the pool upon
    // close().
    private JdbcPooledConnection parentConnection;

    // Brett Wooldridge: the following must be taken into account when caching a
    // prepared statement. Defaults are per JDBC-specification.
    //
    // All of these attributes must match a proposed statement before the
    // statement can be considered "the same" and delivered from the cache.
    private String sql;
    private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;
    private int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
    private Integer resultSetHoldability;
    private Integer autoGeneratedKeys;
    private int[] columnIndexes;
    private String[] columnNames;

    /*
      * PreparedStatement Constructors
      */

    public JdbcPreparedStatementHandle(String sql) {
        this.sql = sql;
    }

    public JdbcPreparedStatementHandle(String sql, int autoGeneratedKeys) {
        this.sql = sql;
        this.autoGeneratedKeys = new Integer(autoGeneratedKeys);
    }

    public JdbcPreparedStatementHandle(String sql, int resultSetType, int resultSetConcurrency) {
        this.sql = sql;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
    }

    public JdbcPreparedStatementHandle(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        this.sql = sql;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = new Integer(resultSetHoldability);
    }

    public JdbcPreparedStatementHandle(String sql, int[] columnIndexes) {
        this.sql = sql;
        this.columnIndexes = columnIndexes;
    }

    public JdbcPreparedStatementHandle(String sql, String[] columnNames) {
        this.sql = sql;
        this.columnNames = columnNames;
    }

    /*
      * Internal methods
      */

    /**
     * Set the parent connection that created this statement. We need this to
     * return the PreparedStatement to the pool.
     *
     * @param pooledConnection the parent JdbcPooledConnection
     */
    protected void setPooledConnection(JdbcPooledConnection pooledConnection) {
        this.parentConnection = pooledConnection;
    }

    protected JdbcPooledConnection getPooledConnection() {
        return parentConnection;
    }

    private PreparedStatement getDelegate() throws SQLException {
        if (pretendClosed)
            throw new SQLException("prepared statement closed");
        return delegate;
    }

    protected PreparedStatement getDelegateUnchecked() {
        return delegate;
    }

    protected void setDelegate(PreparedStatement delegate) {
        this.delegate = delegate;
    }

    public Object getProxiedDelegate() throws Exception {
        return getDelegate();
    }

    /*
      * Overridden Object methods
      */

    /**
     * Overridden equals() that takes all PreparedStatement attributes into
     * account.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof JdbcPreparedStatementHandle)) {
            return false;
        }

        JdbcPreparedStatementHandle otherStmt = (JdbcPreparedStatementHandle) obj;
        if (!sql.equals(otherStmt.sql)) {
            return false;
        } else if (resultSetType != otherStmt.resultSetType) {
            return false;
        } else if (resultSetConcurrency != otherStmt.resultSetConcurrency) {
            return false;
        } else if (!Arrays.equals(columnIndexes, otherStmt.columnIndexes)) {
            return false;
        } else if (!Arrays.equals(columnNames, otherStmt.columnNames)) {
            return false;
        } else if ((autoGeneratedKeys == null && otherStmt.autoGeneratedKeys != null) ||
                (autoGeneratedKeys != null && !autoGeneratedKeys.equals(otherStmt.autoGeneratedKeys))) {
            return false;
        } else if ((resultSetHoldability == null && otherStmt.resultSetHoldability != null) ||
                (resultSetHoldability != null && !resultSetHoldability.equals(otherStmt.resultSetHoldability))) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return sql != null ? sql.hashCode() : System.identityHashCode(this);
    }

    public String toString() {
        return sql;
    }

    /**
     * Overridden methods of PreparedStatement.
     */

    public void close() throws SQLException {
        if (!pretendClosed) {
            // Clear the parameters so the next use of this cached statement
            // doesn't pick up unexpected values.
            delegate.clearParameters();
            // Return to cache so the usage count can be updated
            parentConnection.putCachedStatement(this);
        }

        pretendClosed = true;
    }

    public boolean isClosed() throws SQLException {
        return pretendClosed;
	}
}
