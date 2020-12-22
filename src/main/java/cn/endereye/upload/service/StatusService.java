package cn.endereye.upload.service;

import cn.endereye.upload.entity.File;

public interface StatusService {
    File getStatus(int uuid);

    int getGlobalFinishCount();

    int getGlobalRemainCount();
}
