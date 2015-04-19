/**
 * Created by Maxim Zhukov <crmaxx@ya.ru> on 2015-03-03.
 */

package ru.gravenet.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.codepoetics.protonpack.StreamUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {
    public static void main(String[] args) {
        long startTime = System.nanoTime();

        Config config = new Config();

        JCommander commander = new JCommander(config);
        commander.setProgramName("java-console-downloader-all-0.1-alpha.jar");

        if(ArrayUtils.isEmpty(args)) {
            commander.usage();
            return;
        }

        if(args.length == 1) {
            args = ArrayUtils.add(args, "--help");
        }

        try {
            commander.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            return;
        }

        String outputDir = getOutputDir(config.getOutputDir());
        Map<String, String> downloadList = parseDownloadList(config.getFileList());
        Integer numberOfThreads = config.getNumberOfThreads();
        Integer limitRate = parseRateLimit(config.getLimitRate());

        final ActorSystem system = ActorSystem.create("downloader");
        final ActorRef manager = system.actorOf(Props.create(DownloadManager.class), "manager");

        System.out.println(config.getFileList());
        System.out.println(config.getLimitRate());
        System.out.println(config.getNumberOfThreads());
        System.out.println(config.getOutputDir());

        system.awaitTermination();
        double elapsedTime = (System.nanoTime() - startTime) / 1000000000.0;
        System.out.println("Elapsed time " + elapsedTime + " s");
    }

    private static void createDirectoryIfNeeded(File directory) {
        if (!directory.exists()) {
            System.out.println("Creating directory: " + directory.getPath());
            try {
                directory.mkdir();
            } catch (Throwable e) {
                System.out.println("Error on create new directory: " + e.getMessage());
            }
        }
    }

    private static List<String> readFileAsList(String filePatch) {
        Path path = Paths.get(filePatch);
        List<String> lines = new ArrayList<>();
        try {
            lines = Files.lines(path).collect(Collectors.toList());
            lines.forEach(s -> System.out.println(s));
        } catch (IOException e) {
            System.out.println("Error on read download list file: " + e.getMessage());
        }

        return lines.stream().distinct().filter(p -> p != null).collect(Collectors.toList());
    }

    private static void quitIfEmpty(List<String> downloadList) {
        if (downloadList.isEmpty()) {
            System.out.println("No links for download.");
            System.exit(1);
        }
    }

    private static Map<String, String> convertListToMap(List<String> downloadList) {
        List<String> urls = downloadList.stream()
                                        .map(w -> w.split(" "))
                                        .flatMap(Arrays::stream)
                                        .collect(Collectors.toList());
        
        List<String> files = downloadList.stream()
                                         .map(w -> w.split(" "))
                                         .flatMap(Arrays::stream)
                                         .collect(Collectors.toList());

        List<String> zipped = StreamUtils.zip(urls.stream(), files.stream(), (a, b) -> a)
                                         .collect(Collectors.toList());

        return zipped.stream().collect(Collectors.toMap(x -> x, x -> x));
    }

    private static Map<String, String> parseDownloadList(String filePatch) {
        List<String> downloadList = readFileAsList(filePatch);
        quitIfEmpty(downloadList);
        return convertListToMap(downloadList);
    }

    private static String getOutputDir(String output) {
        File outputDir = new File(output);
        createDirectoryIfNeeded(outputDir);
        return outputDir.getAbsolutePath();
    }

    private static Integer parseRateLimit(String rateLimit) {
        if (rateLimit.isEmpty()) {
            return -1;
        }

        val (value, postfix) = rateLimit.splitAt(rateLimit.length - 1)

        try {
            postfix match {
                case "k" => Some(1024 * value.toInt)
                case "m" => Some(1024 * 1024 * value.toInt)
                case _ => Some(rateLimit.toInt)
            }
        } catch {
            case e : Throwable =>
                Logger.printError("Error on parse rate limit: %s".format(e.getMessage))
                None
        }
    }
}
