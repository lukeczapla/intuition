package org.magicat.intuition.controller;

import org.mapdb.Atomic;
import org.magicat.intuition.Startup;
import org.magicat.intuition.model.ResourceModel;
import org.magicat.intuition.model.User;
import org.magicat.intuition.util.SystemInfo;
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
