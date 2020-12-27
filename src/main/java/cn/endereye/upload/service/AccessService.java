package cn.endereye.upload.service;

import cn.endereye.upload.entity.Entity;
import org.apache.commons.math3.util.Pair;

import java.sql.SQLException;
import java.util.List;

public interface AccessService {
    Pair<Integer, List<Entity>> getRecords(int page, int limit, String search) throws SQLException;
}
