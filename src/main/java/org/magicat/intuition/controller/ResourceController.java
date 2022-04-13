package org.mskcc.knowledge.controller;

import org.mapdb.Atomic;
import org.mskcc.knowledge.Startup;
import org.mskcc.knowledge.model.ResourceModel;
import org.mskcc.knowledge.model.User;
import org.mskcc.knowledge.util.SystemInfo;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResourceController {

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping(value = "/resourceUsage")
    public ResourceModel getResourceUsage() {
        SystemInfo system = new SystemInfo();
        ResourceModel result = new ResourceModel();
        result.setMemInfo(system.memInfo());
        result.setOsInfo(system.osInfo());
        result.setStartupTime(Startup.startTime);
        Map<User, Map<String, Object>> userCache = ArticleController.getUserCache();
        StringBuilder sb = new StringBuilder();
        if (!userCache.isEmpty()) {
            for (User u : userCache.keySet()) {
                if (!userCache.get(u).isEmpty())
                    for (String key: userCache.get(u).keySet()) {
                        if (key.contains("running")) sb.append(u.getEmailAddress()).append(": <b>").append(key).append("</b><br/>");
                    }
            }
        }
        result.setJobsRunning(sb.toString());
        return result;
    }



}
