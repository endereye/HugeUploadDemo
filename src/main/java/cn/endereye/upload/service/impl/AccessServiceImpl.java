package cn.endereye.upload.service.impl;

import cn.endereye.upload.entity.Entity;
import cn.endereye.upload.service.AccessService;
import cn.endereye.upload.util.Database;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.QueryBuilder;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class AccessServiceImpl implements AccessService {
    @Override
    public Pair<Integer, List<Entity>> getRecords(int page, int limit, String search) throws SQLException {
        return Database.exec(connectionSource -> {
            final Dao<Entity, Integer>          dao   = DaoManager.createDao(connectionSource, Entity.class);
            final QueryBuilder<Entity, Integer> query = dao.queryBuilder();

            if (!search.isEmpty())
                query.where().like("json", "%" + search + "%");

            final int          fst = (int) query.countOf();
            final List<Entity> sec = dao.query(query.offset((long) (page - 1) * limit).limit((long) limit).prepare());

            return new Pair<>(fst, sec);
        });
    }
}
