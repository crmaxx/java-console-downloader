/**
 * Created by Maxim Zhukov <crmaxx@ya.ru> on 2015-03-03.
 */

package ru.gravenet.downloader;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;


public class Config {
    @Parameter
    private List<String> parameters = new ArrayList<String>();

    @Parameter(names = { "-f", "--file-list" },
            required = true,
            description = "download URLs found in local FILE")
    private String fileList;

    @Parameter(names = {"-l", "--limit-rate"},
            required = true,
            description = "limit download rate to RATE")
    private String limitRate;

    @Parameter(names = {"-n", "--number-of-threads"},
            required = true,
            description = "count of concurrent downloads")
    private Integer numberOfThreads = 2;

    @Parameter(names = {"-o", "--output-dir"},
            required = true,
            description = "save files to DIR")
    private String outputDir;

    public String getFileList() {
        return fileList;
    }

    public String getLimitRate() {
        return limitRate;
    }

    public Integer getNumberOfThreads() {
        return numberOfThreads;
    }

    public String getOutputDir() {
        return outputDir;
    }
}
