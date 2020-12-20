package cn.endereye.upload.service;

import cn.endereye.upload.entity.Status;

import java.io.IOException;
import java.io.InputStream;

public interface UploadService {
    Status upload(String name, InputStream inputStream) throws IOException;
}
