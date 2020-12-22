package cn.endereye.upload.worker;

import cn.endereye.upload.entity.File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

@Component
public class Master implements ApplicationListener<ContextRefreshedEvent> {
    public static final int BATCHES = 100;
    public static final int WORKERS = 5;

    @Data
    @AllArgsConstructor
    public static class Task {
        private File   upload;
        private Object object;
    }

    private final LinkedList<Thread> threads = new LinkedList<>();
    private final LinkedList<Worker> workers = new LinkedList<>();

    private final LinkedList<Task>       tasks = new LinkedList<>();
    private final HashMap<Integer, File> files = new HashMap<>();

    @Getter
    private final File globalFile = new File();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        for (int i = 0; i < WORKERS; i++) {
            final Worker worker = new Worker(tasks, globalFile);
            final Thread thread = new Thread(worker);
            thread.start();
            workers.addLast(worker);
            threads.addLast(thread);
        }
    }

    public File addWorkbook(String name, InputStream inputStream) throws IOException {
        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (Exception ignored) {
            workbook = new HSSFWorkbook(inputStream);
        }
        final Sheet spreadsheet = workbook.getSheetAt(0);

        final boolean notify = tasks.isEmpty();
        final File    file   = new File();

        file.setUuid(System.identityHashCode(file));
        file.setName(name);
        file.setTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        file.setFinishCount(0);
        file.setRemainCount(spreadsheet.getLastRowNum() - spreadsheet.getFirstRowNum());

        if (notify) {
            boolean clear = true;
            for (final Worker worker : workers) {
                if (!worker.isSleep()) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                globalFile.setFinishCount(0);
                globalFile.setRemainCount(0);
            }
        }

        globalFile.setRemainCount(globalFile.getRemainCount() + file.getRemainCount());

        synchronized (tasks) {
            final Iterator<Row> iter = spreadsheet.rowIterator();
            iter.next();
            iter.forEachRemaining(row -> tasks.addLast(new Task(file, row)));
            if (notify)
                tasks.notifyAll();
        }
        synchronized (files) {
            files.put(file.getUuid(), file);
        }

        return file;
    }

    public File getUnfinishedFile(int uuid) {
        final File file = files.get(uuid);
        if (file.getRemainCount() == 0)
            files.remove(uuid);
        return file;
    }

    public void waitUntilFinish() throws InterruptedException {
        for (final Worker worker : workers)
            worker.setExit(true);
        synchronized (tasks) {
            tasks.notifyAll();
        }
        for (final Thread thread : threads)
            thread.join();
    }
}
