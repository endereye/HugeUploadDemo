package cn.endereye.upload.service.impl;

import cn.endereye.upload.entity.Status;
import cn.endereye.upload.service.UploadService;
import cn.endereye.upload.worker.Master;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Service
public class UploadServiceImpl implements UploadService {
    @Autowired
    private Master master;

    @Override
    public Status upload(String name, InputStream inputStream) throws IOException {
        return master.addWorkbook(name, inputStream);
    }
}
