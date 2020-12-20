package cn.endereye.upload;

import cn.endereye.upload.worker.Master;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UploadApplicationTests {
    @Autowired
    Master master;

    @Test
    void small() {
        Assertions.assertDoesNotThrow(() -> {
            Thread.sleep(1000);
            master.addWorkbook("", UploadApplicationTests.class.getResourceAsStream("/small.xlsx"));
            Thread.sleep(1000);
            master.addWorkbook("", UploadApplicationTests.class.getResourceAsStream("/small.xlsx"));
            master.waitUntilFinish();
        });
    }

    @Test
    void large() {
        Assertions.assertDoesNotThrow(() -> {
            master.addWorkbook("", UploadApplicationTests.class.getResourceAsStream("/large.xlsx"));
            master.waitUntilFinish();
        });
    }
}
