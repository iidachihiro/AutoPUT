package jp.mzw.autoput.experiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ExperimentUtils {

    public static Path getSubjectFilePath(String project, String subjectId, String testId, String mode) {
        return Paths.get(String.join("/", "experiment", "subject", project ,subjectId, getSubjectName(subjectId, testId, mode) + ".java"));
    }
    public static void createSubjectFileDirs(String project, String subjectId, String testId, String mode) {
        if (Files.exists(getSubjectFilePath(project, subjectId, testId, mode))) {
            return;
        }
        try {
            Files.createDirectories(Paths.get(String.join("/", "experiment", "subject", project ,subjectId)));
            Files.createFile(getSubjectFilePath(project, subjectId, testId, mode));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getSubjectName(String subjectId, String testId, String mode) {
        return String.join("_", mode, "Subject" + subjectId, testId);
    }
    public static Path getDeployPath(String project, String subjectId, String testId, String packageName, String mode) {
        String testPath;
        if (project.equals("jdom")) {
            testPath = "test/src/java";
        } else {
            testPath = "src/test/java/";
        }
        return Paths.get(String.join("/", "usersubjects", project, mode,
                subjectId, project, testPath, packageName.replace(".", "/"), getSubjectName(subjectId, testId, mode) + ".java"));
    }

    public static Path getWrongDeployPath(String project, String subjectId, String testId, String packageName, String mode) {
        String testPath;
        if (project.equals("jdom")) {
            testPath = "test/src/java";
        } else {
            testPath = "src/test/java/";
        }
        return Paths.get(String.join("/", "usersubjects", project, mode,
                subjectId, project, testPath, packageName.replace(".", "/")) + getSubjectName(subjectId, testId, mode) + ".java");
    }

    public static void outputConvertResult(String project, String subjectId, String testId, String mode, String content) {
        try (BufferedWriter bw = Files.newBufferedWriter(getSubjectFilePath(project, subjectId, testId, mode))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deploy(String project, String subjectId, String testId, String packageName, String mode) {
        try (BufferedWriter bw = Files.newBufferedWriter(getDeployPath(project, subjectId, testId, packageName, mode))){
            Files.deleteIfExists(getWrongDeployPath(project, subjectId, testId, packageName, mode));
            String content = getModifiedContent(project, subjectId, testId, mode);
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delete(String project, String subjectId, String testId, String packageName, String mode) {
        try {
            Files.deleteIfExists(getDeployPath(project, subjectId, testId, packageName, mode));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getModifiedContent(String project, String subjectId, String testId, String mode) throws IOException {
        return Files.lines(getSubjectFilePath(project, subjectId, testId, mode), Charset.forName("UTF-8"))
                .collect(Collectors.joining(System.getProperty("line.separator")));
    }
}
