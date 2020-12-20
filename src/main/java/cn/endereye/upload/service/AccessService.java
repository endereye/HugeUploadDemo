package cn.endereye.upload.service;

import cn.endereye.upload.entity.Entity;

import java.sql.SQLException;
import java.util.List;

public interface AccessService {
    int getRecordCount() throws SQLException;

    List<Entity> getRecords(int page, int limit) throws SQLException;
}
