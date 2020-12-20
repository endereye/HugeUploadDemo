package cn.endereye.upload.util;

import cn.endereye.upload.entity.Entity;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class Database implements ApplicationListener<ContextRefreshedEvent> {
    @FunctionalInterface
    public interface JobV {
        void doJob(ConnectionSource connectionSource) throws SQLException;
    }

    @FunctionalInterface
    public interface JobR<R> {
        R doJob(ConnectionSource connectionSource) throws SQLException;
    }

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private static final DataSourceConnectionSource connectionSource = new DataSourceConnectionSource();

    public static synchronized void exec(JobV job) throws SQLException {
        job.doJob(connectionSource);
    }

    public static synchronized <R> R exec(JobR<R> job) throws SQLException {
        return job.doJob(connectionSource);
    }

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            connectionSource.setDataSource(jdbcTemplate.getDataSource());
            connectionSource.setDatabaseUrl(jdbcTemplate.getDataSource().getConnection().getMetaData().getURL());
            connectionSource.initialize();

            TableUtils.createTableIfNotExists(connectionSource, Entity.class);
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            logger.error("Failed when initializing ORMLite datasource");
            System.exit(-1);
        }
    }
}
