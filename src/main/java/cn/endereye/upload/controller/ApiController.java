package cn.endereye.upload.controller;

import cn.endereye.upload.entity.Entity;
import cn.endereye.upload.entity.Status;
import cn.endereye.upload.service.AccessService;
import cn.endereye.upload.service.StatusService;
import cn.endereye.upload.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private AccessService accessService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private UploadService uploadService;

    @RequestMapping(value = "/access", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String access(@RequestParam int page,
                         @RequestParam int limit) throws SQLException {
        final StringBuilder jsonBuilder = new StringBuilder();

        jsonBuilder.append("{\"rows\":")
                   .append(accessService.getRecordCount())
                   .append(",\"data\":[");
        for (final Entity entity : accessService.getRecords(page, limit))
            jsonBuilder.append(entity.getJson())
                       .append(',');
        if (jsonBuilder.charAt(jsonBuilder.length() - 1) != '[')
            jsonBuilder.setLength(jsonBuilder.length() - 1); // remove the last comma
        jsonBuilder.append("]}");

        return jsonBuilder.toString();
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String status() {
        final StringBuilder jsonBuilder = new StringBuilder();
        int                 finish      = 0;
        int                 remain      = 0;

        jsonBuilder.append("{\"data\":[");
        for (final Status status : statusService.getStatuses()) {
            jsonBuilder.append("{\"uuid\":").append(status.getUuid())
                       .append(",\"name\":\"").append(status.getName()).append('"')
                       .append(",\"time\":\"").append(status.getTime()).append('"')
                       .append(",\"finish\":").append(status.getFinishCount())
                       .append(",\"remain\":").append(status.getRemainCount())
                       .append("},");
            finish += status.getFinishCount();
            remain += status.getRemainCount();
        }
        if (jsonBuilder.charAt(jsonBuilder.length() - 1) != '[')
            jsonBuilder.setLength(jsonBuilder.length() - 1); // remove the last comma
        jsonBuilder.append("],\"finish\":")
                   .append(finish)
                   .append(",\"remain\":")
                   .append(remain)
                   .append('}');

        return jsonBuilder.toString();
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String upload(@RequestParam MultipartFile file) throws IOException {
        final Status status = uploadService.upload(file.getOriginalFilename(), file.getInputStream());
        return "{\"uuid\":" + status.getUuid() +
               ",\"name\":\"" + status.getName() + "\"" +
               ",\"time\":\"" + status.getTime() + "\"" +
               ",\"size\":" + (status.getRemainCount() + status.getFinishCount()) + "}";
    }
}
