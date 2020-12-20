package cn.endereye.upload.worker;

import cn.endereye.upload.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.list.UnmodifiableList;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Component
public class Master implements ApplicationListener<ContextRefreshedEvent> {
    public static final int BATCHES = 100;
    public static final int WORKERS = 5;

    @Data
    @AllArgsConstructor
    public static class Single {
        private Status status;
        private Object object;
    }

    private final LinkedList<Thread> threads  = new LinkedList<>();
    private final LinkedList<Worker> workers  = new LinkedList<>();
    private final LinkedList<Single> singles  = new LinkedList<>();
    private final LinkedList<Status> statuses = new LinkedList<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        for (int i = 0; i < WORKERS; i++) {
            final Worker worker = new Worker(singles);
            final Thread thread = new Thread(worker);
            thread.start();
            workers.addLast(worker);
            threads.addLast(thread);
        }
    }

    public Status addWorkbook(String name, InputStream inputStream) throws IOException {
        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (Exception ignored) {
            workbook = new HSSFWorkbook(inputStream);
        }
        final Sheet spreadsheet = workbook.getSheetAt(0);

        final boolean notify = singles.isEmpty();
        final Status  status = new Status();

        status.setUuid(System.identityHashCode(status));
        status.setName(name);
        status.setTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        status.setFinishCount(0);
        status.setRemainCount(spreadsheet.getLastRowNum() - spreadsheet.getFirstRowNum());

        synchronized (singles) {
            final Iterator<Row> iter = spreadsheet.rowIterator();
            iter.next();
            iter.forEachRemaining(row -> singles.addLast(new Single(status, row)));
            if (notify)
                singles.notifyAll();
        }
        synchronized (statuses) {
            statuses.addLast(status);
        }

        return status;
    }

    public List<Status> getUnfinished() {
        // Enumerate every status, if its finished, simply drop it; otherwise, move it to the end of the list;
        synchronized (statuses) {
            final int size = statuses.size();
            for (int i = 0; i < size; i++) {
                final Status status = statuses.removeFirst();
                if (status.getRemainCount() > 0)
                    statuses.addLast(status);
            }
            return UnmodifiableList.unmodifiableList(statuses);
        }
    }

    public void waitUntilFinish() throws InterruptedException {
        for (final Worker worker : workers)
            worker.setExit(true);
        synchronized (singles) {
            singles.notifyAll();
        }
        for (final Thread thread : threads)
            thread.join();
    }
}
