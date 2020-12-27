package cn.endereye.upload.controller;

import cn.endereye.upload.entity.Entity;
import cn.endereye.upload.entity.File;
import cn.endereye.upload.service.AccessService;
import cn.endereye.upload.service.StatusService;
import cn.endereye.upload.service.UploadService;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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
                         @RequestParam int limit,
                         @RequestParam String search) throws SQLException {
        final Pair<Integer, List<Entity>> queryResult = accessService.getRecords(page, limit, search);
        final StringBuilder               jsonBuilder = new StringBuilder();

        jsonBuilder.append("{\"rows\":")
                   .append(queryResult.getFirst())
                   .append(",\"data\":[");
        for (final Entity entity : queryResult.getSecond())
            jsonBuilder.append(entity.getJson())
                       .append(',');
        if (jsonBuilder.charAt(jsonBuilder.length() - 1) != '[')
            jsonBuilder.setLength(jsonBuilder.length() - 1); // remove the last comma
        jsonBuilder.append("]}");

        return jsonBuilder.toString();
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String status(@RequestParam(required = false) Integer uuid) {
        final StringBuilder jsonBuilder = new StringBuilder();

        if (uuid != null) {
            final File file = statusService.getStatus(uuid);
            jsonBuilder.append("{\"uuid\":").append(file.getUuid())
                       .append(",\"name\":\"").append(file.getName()).append('"')
                       .append(",\"time\":\"").append(file.getTime()).append('"')
                       .append(",\"finish\":").append(file.getFinishCount())
                       .append(",\"remain\":").append(file.getRemainCount())
                       .append('}');
        } else {
            jsonBuilder.append("{\"finish\":").append(statusService.getGlobalFinishCount())
                       .append(",\"remain\":").append(statusService.getGlobalRemainCount())
                       .append('}');
        }

        return jsonBuilder.toString();
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String upload(@RequestParam MultipartFile file) throws IOException {
        final File fileInfo = uploadService.upload(file.getOriginalFilename(), file.getInputStream());
        return "{\"uuid\":" + fileInfo.getUuid() +
               ",\"name\":\"" + fileInfo.getName() + "\"" +
               ",\"time\":\"" + fileInfo.getTime() + "\"" +
               ",\"size\":" + (fileInfo.getRemainCount() + fileInfo.getFinishCount()) + "}";
    }
}
