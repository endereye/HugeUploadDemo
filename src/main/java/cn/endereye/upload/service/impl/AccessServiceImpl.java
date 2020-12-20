package cn.endereye.upload.service.impl;

import cn.endereye.upload.entity.Entity;
import cn.endereye.upload.service.AccessService;
import cn.endereye.upload.util.Database;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.QueryBuilder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class AccessServiceImpl implements AccessService {
    @Override
    public int getRecordCount() throws SQLException {
        return Database.exec(connectionSource -> {
            final Dao<Entity, Integer> dao = DaoManager.createDao(connectionSource, Entity.class);
            return (int) dao.countOf();
        });
    }

    @Override
    public List<Entity> getRecords(int page, int limit) throws SQLException {
        return Database.exec(connectionSource -> {
            final Dao<Entity, Integer>          dao   = DaoManager.createDao(connectionSource, Entity.class);
            final QueryBuilder<Entity, Integer> query = dao.queryBuilder();

            query.offset((long) (page - 1) * limit).limit((long) limit);

            return dao.query(query.prepare());
        });
    }
}
