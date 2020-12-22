package cn.endereye.upload.service;

import cn.endereye.upload.entity.File;

import java.io.IOException;
import java.io.InputStream;

public interface UploadService {
    File upload(String name, InputStream inputStream) throws IOException;
}
