package cc.concurrent.mango;

import cc.concurrent.mango.support.Man;
import cc.concurrent.mango.support.ManDao;
import cc.concurrent.mango.util.logging.InternalLoggerFactory;
import cc.concurrent.mango.util.logging.Slf4JLoggerFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author ash
 */
public class MangoCacheTest {

    private static ManDao dao;
    private static DataCache dataCache;

    @BeforeClass
    public static void beforeClass() throws Exception {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:hsqldb:mem:test", "sa", "");
        createTable(ds);
        dataCache = new DataCacheImpl();
        dao = new Mango(ds, dataCache).create(ManDao.class);
    }

    // TODO 便利的debug log
    @Test
    public void testOne() throws Exception {
        Man man = new Man("ash", 26, true, 10086, new Date());
        int id = dao.insert(man);
        String key = getKey(id);
        man.setId(id);
        assertThat(dataCache.get(key), nullValue());
        Man man2 = dao.select(id);
        assertThat(man2, equalTo(man));
        Man man3 = (Man) dataCache.get(key);
        assertThat(man3, equalTo(man));
        Man man4 = dao.select(id);
        assertThat(man4, equalTo(man));

        man.setName("lulu");
        dao.update(man);
        assertThat(dataCache.get(key), nullValue());
        Man man5 = dao.select(id);
        assertThat(man5, equalTo(man));
        Man man6 = (Man) dataCache.get(key);
        assertThat(man6, equalTo(man));
        Man man7 = dao.select(id);
        assertThat(man7, equalTo(man));

        dao.delete(id);
        assertThat(dataCache.get(key), nullValue());
        Man man8 = dao.select(id);
        assertThat(man8, nullValue());
        assertThat(dataCache.get(key), nullValue());
        Man man9 = dao.select(id);
        assertThat(man9, nullValue());
    }

    /**
     * 创建表
     * @throws java.sql.SQLException
     */
    private static void createTable(DataSource ds) throws SQLException {
        Connection conn = ds.getConnection();
        Statement stat = conn.createStatement();
        String table = fileToString("/man.sql");
        stat.execute(table);
        stat.close();
        conn.close();
    }

    /**
     * 从文本文件中获得建表语句
     * @param name
     * @return
     */
    private static String fileToString(String name) {
        InputStream is = MangoTest.class.getResourceAsStream(name);
        Scanner s = new Scanner(is);
        StringBuffer sb = new StringBuffer();
        while (s.hasNextLine()) {
            sb.append(s.nextLine()).append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }

    private String getKey(int id) {
        return "man_" + id;
    }


    private static class DataCacheImpl implements DataCache {

        private Map<String, Object> cache = new HashMap<String, Object>();

        @Override
        public Object get(String key) {
            return cache.get(key);
        }

        @Override
        public Map<String, Object> getBulk(Set<String> keys) {
            Map<String, Object> map = new HashMap<String, Object>();
            for (String key : keys) {
                map.put(key, cache.get(key));
            }
            return map;
        }

        @Override
        public void set(String key, Object value) {
            cache.put(key, value);
        }

        @Override
        public void delete(Set<String> keys) {
            for (String key : keys) {
                cache.remove(key);
            }
        }
    }

}
