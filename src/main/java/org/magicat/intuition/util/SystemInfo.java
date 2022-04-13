package org.magicat.intuition.util;
import java.io.File;
import java.text.NumberFormat;

public class SystemInfo {

    private final Runtime runtime = Runtime.getRuntime();

    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.osInfo());
        sb.append(this.memInfo());
        sb.append(this.diskInfo());
        return sb.toString();
    }

    public String osName() {
        return System.getProperty("os.name");
    }

    public String osVersion() {
        return System.getProperty("os.version");
    }

    public String osArch() {
        return System.getProperty("os.arch");
    }

    public long totalMem() {
        return Runtime.getRuntime().totalMemory();
    }

    public long usedMem() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public String memInfo() {
        NumberFormat format = NumberFormat.getInstance();
        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        sb.append("Free memory (MB): ");
        sb.append(format.format(freeMemory / 1024 / 1024));
        sb.append("<br/>");
        sb.append("Allocated memory (MB): ");
        sb.append(format.format(allocatedMemory / 1024 / 1024));
        sb.append("<br/>");
        sb.append("Max memory (MB): ");
        sb.append(format.format(maxMemory / 1024 / 1024));
        sb.append("<br/>");
        sb.append("Total free memory (MB): ");
        sb.append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024 / 1024));
        sb.append("<br/>");
        sb.append("Percentage used (%): ");
        sb.append(format.format(100.0*(double)(maxMemory - (freeMemory + (maxMemory - allocatedMemory)))/(double)maxMemory));
        sb.append("<br/>");
        return sb.toString();

    }

    public String osInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS: ");
        sb.append(this.osName());
        sb.append("<br/>");
        sb.append("Version: ");
        sb.append(this.osVersion());
        sb.append(" (");
        sb.append(this.osArch());
        sb.append(")<br/>");
        sb.append("Available processors (cores): ");
        sb.append(runtime.availableProcessors());
        sb.append("<br/>");
        return sb.toString();
    }

    public String diskInfo() {
        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();
        StringBuilder sb = new StringBuilder();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            sb.append("File system root: ");
            sb.append(root.getAbsolutePath());
            sb.append("<br/>");
            sb.append("Total space (bytes): ");
            sb.append(root.getTotalSpace());
            sb.append("<br/>");
            sb.append("Free space (bytes): ");
            sb.append(root.getFreeSpace());
            sb.append("<br/>");
            sb.append("Usable space (bytes): ");
            sb.append(root.getUsableSpace());
            sb.append("<br/>");
        }
        return sb.toString();
    }
}
