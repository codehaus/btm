package bitronix.tm;

import bitronix.tm.drivers.*;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * (c) Bitronix, 20-oct.-2005
 *
 * @author lorban
 */
public class JdbcOnlyTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(JdbcOnlyTest.class);

    private String query;
    private PoolingDataSource poolingDataSource1;
    private PoolingDataSource poolingDataSource2;

    protected void setUp() throws Exception {
        // change transactionRetryInterval to 1 second
        Field field = TransactionManagerServices.getConfiguration().getClass().getDeclaredField("transactionRetryInterval");
        field.setAccessible(true);
        field.set(TransactionManagerServices.getConfiguration(), new Integer(1));

        setUpJdbc_FB();
    }

    private void setUpJdbc_FB() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = FbTest.getPoolingDataSource1();
        poolingDataSource2 = FbTest.getPoolingDataSource2();
    }

    private void setUpJdbc_HSQL() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = HsqldbTest.getPoolingDataSource1();
        poolingDataSource2 = FbTest.getPoolingDataSource2();
    }

    private void setUpJdbc_Ingres() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = IngresTest.getPoolingDataSource1();
        poolingDataSource2 = IngresTest.getPoolingDataSource2();
    }

    private void setUpJdbc_Derby() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = DerbyTest.getPoolingDataSource1();
        poolingDataSource2 = DerbyTest.getPoolingDataSource2();
    }

    private void setUpJdbc_Mysql() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (name) values (?)";

        poolingDataSource1 = MysqlTest.getPoolingDataSource1();
        poolingDataSource2 = MysqlTest.getPoolingDataSource2();
    }

    private void setUpJdbc_ORA() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (id, name) values (seq_users.nextval, ?)";

        poolingDataSource1 = OracleTest.getPoolingDataSource1();
        poolingDataSource2 = OracleTest.getPoolingDataSource2();
    }

    private void setUpJdbc_ASE() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (name) values (?)";

        poolingDataSource1 = SybaseTest.getPoolingDataSource1();
        poolingDataSource2 = SybaseTest.getPoolingDataSource2();
    }

    private void setUpJdbc_FSQL() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = FirstsqlTest.getPoolingDataSource1();
        poolingDataSource2 = FirstsqlTest.getPoolingDataSource2();
    }

    private void setUpJdbc_PB() throws Exception {
        if (poolingDataSource1 != null  &&  poolingDataSource2 != null)
            return;

        query = "insert into users (name) values (?)";

        poolingDataSource1 = PointbaseTest.getPoolingDataSource1();
        poolingDataSource2 = PointbaseTest.getPoolingDataSource2();
    }


    public void testAutoEnlistment() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug(" ****** about to begin");
        tm.begin();
        tm.setTransactionTimeout(5);
        if (log.isDebugEnabled()) log.debug(" ****** TX is: " + tm.getTransaction());

        Connection connection1 = poolingDataSource1.getConnection();
        Connection connection2 = poolingDataSource2.getConnection();

        if (log.isDebugEnabled()) log.debug(" ****** cleaning up DB");
        dropAll(connection1, connection2);

        if (log.isDebugEnabled()) log.debug(" ****** doing some read-write SQL");
        PreparedStatement ps = connection1.prepareStatement(query);
        ps.setString(1, "testAutoEnlistment(" + getUniqueNumber() + ")");
        ps.executeUpdate();
        ps.close();
        if (log.isDebugEnabled()) log.debug(" ****** executed update 1");

        if (log.isDebugEnabled()) log.debug(" ****** preparing update 2");
        ps = connection2.prepareStatement(query);
        ps.setString(1, "testAutoEnlistment(" + getUniqueNumber() + ")");
        if (log.isDebugEnabled()) log.debug(" ****** executing update 2");
        ps.executeUpdate();
        ps.close();
        if (log.isDebugEnabled()) log.debug(" ****** executed update 2");

        if (log.isDebugEnabled()) log.debug(" ****** about to close connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug(" ****** about to close connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug(" ****** about to commit");
        tm.commit();
    }

    static int cpt = 0;
    private long getUniqueNumber() {
        synchronized (getClass()) {
            return cpt++;
        }
    }

    private long genPk() {
        return System.currentTimeMillis() + getUniqueNumber();
    }

    public void dropAll(Connection connection1, Connection connection2) throws Exception {
        Statement statement = connection1.createStatement();
        statement.executeUpdate("delete from users");

        statement = connection2.createStatement();
        statement.executeUpdate("delete from users");
    }

}
