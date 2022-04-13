package org.magicat.intuition.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;

@EqualsAndHashCode
@ToString
public class ResourceModel {

    private String memInfo;
    private String osInfo;
    private String diskInfo;
    private String jobsRunning;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime startupTime;

    public String getMemInfo() {
        return memInfo;
    }

    public void setMemInfo(String memInfo) {
        this.memInfo = memInfo;
    }

    public String getOsInfo() {
        return osInfo;
    }

    public void setOsInfo(String osInfo) {
        this.osInfo = osInfo;
    }

    public String getDiskInfo() {
        return diskInfo;
    }

    public void setDiskInfo(String diskInfo) {
        this.diskInfo = diskInfo;
    }

    public String getJobsRunning() {
        return jobsRunning;
    }

    public void setJobsRunning(String jobsRunning) {
        this.jobsRunning = jobsRunning;
    }

    public DateTime getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(DateTime startupTime) {
        this.startupTime = startupTime;
    }
}
