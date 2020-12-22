package cn.endereye.upload.service.impl;

import cn.endereye.upload.entity.File;
import cn.endereye.upload.service.StatusService;
import cn.endereye.upload.worker.Master;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Service
public class StatusServiceImpl implements StatusService {
    @Autowired
    private Master master;

    @Override
    public File getStatus(int uuid) {
        return master.getUnfinishedFile(uuid);
    }

    @Override
    public int getGlobalFinishCount() {
        return master.getGlobalFile().getFinishCount();
    }

    @Override
    public int getGlobalRemainCount() {
        return master.getGlobalFile().getRemainCount();
    }
}
