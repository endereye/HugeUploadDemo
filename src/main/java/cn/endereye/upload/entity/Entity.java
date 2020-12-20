package cn.endereye.upload.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

@DatabaseTable(tableName = "tax")
@Data
public class Entity {
    @DatabaseField(canBeNull = false, generatedId = true)
    private int    uuid;
    @DatabaseField(canBeNull = false, columnDefinition = "TEXT")
    private String json;
}
