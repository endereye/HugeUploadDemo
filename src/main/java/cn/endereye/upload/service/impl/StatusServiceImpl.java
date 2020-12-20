package cn.endereye.upload.service.impl;

import cn.endereye.upload.entity.Status;
import cn.endereye.upload.service.StatusService;
import cn.endereye.upload.worker.Master;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Service
public class StatusServiceImpl implements StatusService {
    @Autowired
    private Master master;

    @Override
    public List<Status> getStatuses() {
        return master.getUnfinished();
    }
}
