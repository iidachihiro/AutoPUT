package jp.mzw.autoput.experiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ExperimentUtils {

    public static Path getSubjectFilePath(String project, String packageName, String className, String testName, String mode) {
        return Paths.get(String.join("/", "experiment", project ,packageName, className, testName, mode, mode + "Test.java"));
    }
    public static void createSubjectFileDirs(String project, String packageName, String className, String testName, String mode) {
        if (Files.exists(getSubjectFilePath(project, packageName, className, testName, mode))) {
            return;
        }
        try {
            Files.createDirectories(Paths.get(String.join("/", "experiment", project ,packageName, className, testName, mode)));
            Files.createFile(getSubjectFilePath(project, packageName, className, testName, mode));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Path getDeployPath(String project, String packageName, String mode) {

        String testPath = "src/test/java/";
        if (project.equals("commons-chain")) {
            testPath = "base/src/test/java/";
        } else if (project.equals("commons-digester")) {
            testPath = "core/src/test/java/";
        }
        return Paths.get(String.join("/", "subjects", project, testPath, packageName.replace(".", "/"), mode + "Test.java"));
    }

    public static void outputConvertResult(String project, String packageName, String className, String testName, String mode, String content) {
        try (BufferedWriter bw = Files.newBufferedWriter(getSubjectFilePath(project, packageName, className, testName, mode))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deploy(String project, String packageName, String className, String testName, String mode) {
        if (!Files.exists(getDeployPath(project, packageName, mode))) {
            try {
                Files.createFile(getDeployPath(project, packageName, mode));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getDeployPath(project, packageName, mode))){
            String content = getModifiedContent(project, packageName, className, testName, mode);
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delete(String project, String packageName, String mode) {
        try {
            Files.deleteIfExists(getDeployPath(project, packageName, mode));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getModifiedContent(String project, String packageName, String className, String testName, String mode) throws IOException {
        return Files.lines(getSubjectFilePath(project, packageName, className, testName, mode), Charset.forName("UTF-8"))
                .collect(Collectors.joining(System.getProperty("line.separator")));
    }

    public String getJacocoHtml(String project, String packageName, String className, String testName, String mode) {
        String content = "";
        try {
            content = Files.lines(
                    Paths.get(
                            String.join("/", "jacoco", project, packageName, className, testName, mode, "index.html")),
                    Charset.forName("UTF-8")
            ).collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            // do nothing
        }
        return content;
    }
}
