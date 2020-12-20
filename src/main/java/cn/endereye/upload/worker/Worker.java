package cn.endereye.upload.worker;

import cn.endereye.upload.entity.Entity;
import cn.endereye.upload.util.Database;
import com.google.gson.Gson;
import com.j256.ormlite.dao.DaoManager;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class Worker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    private final LinkedList<Master.Single> tasks;
    private final Gson                      gson = new Gson();
    @Setter
    private       boolean                   exit = false;

    public Worker(LinkedList<Master.Single> tasks) {
        this.tasks = tasks;
    }

    @SneakyThrows
    @Override
    public void run() {
        while (!exit || !tasks.isEmpty()) {
            if (tasks.isEmpty()) {
                logger.info("Worker goes to sleep because nothing to do");
                synchronized (tasks) {
                    tasks.wait();
                }
                logger.info("Worker gets back to work");
            }
            if (!tasks.isEmpty())
                doBatch();
        }
    }

    private void doBatch() {
        final Master.Single[] rows;
        final int             size;
        synchronized (tasks) {
            size = Math.min(Master.BATCHES, tasks.size());
            rows = new Master.Single[size];
            for (int i = 0; i < size; i++)
                rows[i] = tasks.removeFirst();
            logger.info(String.format("%d rows retrieved, %d remaining", size, tasks.size()));
        }

        if (size > 0) {
            final Entity[] entities = new Entity[size];
            for (int i = 0; i < size; i++)
                entities[i] = doSingle(rows[i]);
            try {
                Database.exec(connectionSource -> {
                    DaoManager.createDao(connectionSource, Entity.class).create(Arrays.asList(entities));
                });
            } catch (SQLException e) {
                e.printStackTrace();
                logger.error("Worker failed due to internal exception");
            }
            for (final Master.Single single : rows)
                single.getStatus().completeOne();
        }
    }

    private Entity doSingle(Master.Single single) {
        final Row current = (Row) single.getObject();
        final Row columns = current.getSheet().getRow(0);

        final Iterator<Cell>          iterNow    = current.cellIterator();
        final HashMap<String, Object> properties = new HashMap<>();

        while (iterNow.hasNext()) {
            final Cell cellNow = iterNow.next();
            final Cell cellTop = columns.getCell(cellNow.getColumnIndex());

            final String rawColumnName = cellTop.getStringCellValue();
            final String proColumnName;

            if (rawColumnName.startsWith("CHECK"))
                continue;

            final int l = Math.max(rawColumnName.indexOf('('), rawColumnName.indexOf('（'));
            final int r = Math.max(rawColumnName.indexOf(')'), rawColumnName.indexOf('）'));
            if (l != -1 && r != -1)
                proColumnName = rawColumnName.substring(0, l) + rawColumnName.substring(r + 1);
            else
                proColumnName = rawColumnName;

            switch (cellNow.getCellType()) {
                case NUMERIC:
                    properties.put(proColumnName, cellNow.getNumericCellValue());
                    break;
                case STRING:
                    final String s = cellNow.getStringCellValue();
                    if ("是".equals(s) || "否".equals(s))
                        properties.put(proColumnName, "是".equals(s));
                    else
                        properties.put(proColumnName, s);
                    break;
                case BOOLEAN:
                    properties.put(proColumnName, cellNow.getBooleanCellValue());
                    break;
            }
        }

        final Entity entity = new Entity();
        entity.setJson(gson.toJson(properties));
        return entity;
    }
}
