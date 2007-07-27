package bitronix.tm;

import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class RestartTest extends TestCase {

    public void testRestart() throws Exception {
        for (int i=0; i<3 ;i++) {
            PoolingDataSource pds = new PoolingDataSource();
            pds.setClassName(MockXADataSource.class.getName());
            pds.setUniqueName("ds");
            pds.setPoolSize(1);
            pds.init();
            
            BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
            tm.shutdown();
            assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());
        }
    }

}
