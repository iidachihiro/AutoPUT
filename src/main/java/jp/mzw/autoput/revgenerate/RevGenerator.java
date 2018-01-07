package jp.mzw.autoput.revgenerate;

import jp.mzw.autoput.Main;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.generate.Generator;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class RevGenerator {

    protected static Logger LOGGER = LoggerFactory.getLogger(RevGenerator.class);
    protected Project project;
    protected Generator generator;

    private static final String REVGENERATE_DIR = "revgenerate";

    public RevGenerator(Project project) {
        this.project = project;
        this.generator = new Generator(project);
    }

    public void revgenerate() {
        // Load detection results. Then, generate PUT candidates.
        List<CSVRecord> records = generator.getDetectResult();
        for (CSVRecord record : records) {
            String packageName = record.get(0);
            String testSuiteName = record.get(1);
            String testCaseName  = record.get(2);
            String put = generator.getPUT(packageName, testSuiteName, testCaseName);

            String modified = new RevGenerateEngine(put).revgenerate();
            _outputPUT(packageName, testSuiteName, testCaseName, modified);
        }
    }

    private void _outputPUT(String packageName, String className, String methodName, String content) {
        if (!Files.exists(Paths.get(_getPathToCUT(packageName, className, methodName)))) {
            try {
                Files.createDirectories(Paths.get(_getPathToOutputDir(packageName, className, methodName)));
                Files.createFile(Paths.get(_getPathToCUT(packageName, className, methodName)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(_getPathToCUT(packageName, className, methodName)))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String _getPathToCUT(String packageName, String className, String methodName) {
        return String.join("/", _getPathToOutputDir(packageName, className, methodName), Main.CUT + ".java");
    }
    private String _getPathToOutputDir(String packageName, String className, String testName) {
        return String.join("/", project.getOutputDir().getName(), project.getProjectId(), REVGENERATE_DIR, packageName, className, testName);
    }
}
