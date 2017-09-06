package jp.mzw.autoput.experiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExperimentUtils {

    public static Path getSubjectFilePath(String project, String subjectId, String testId, String mode) {
        return Paths.get(String.join("/", "experiment", "subject", project ,subjectId, getSubjectName(subjectId, testId, mode) + ".java"));
    }
    public static void createSubjectFileDirs(String project, String subjectId, String testId, String mode) {
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

    public static void outputConvertResult(String project, String subjectId, String testId, String mode, String content) {
        try (BufferedWriter bw = Files.newBufferedWriter(getSubjectFilePath(project, subjectId, testId, mode))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
