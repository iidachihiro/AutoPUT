package jp.mzw.autoput.generate;

import jp.mzw.autoput.Main;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestCase;
import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.detect.Detector;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Generator {

    protected static Logger LOGGER = LoggerFactory.getLogger(Generator.class);
    protected Project project;
    protected Detector detector;

    private static final String GENERATE_DIR = "generate";

    public Generator(Project project) {
        this.project = project;
        this.detector = new Detector(project);
    }

    public void generate() {
        // Load detection results. Then, generate PUT candidates.
        List<CSVRecord> records = detector.getDetectResults();
        for (CSVRecord record : records) {
            String packageName = record.get(0);
            String testSuiteName = record.get(1);
            String testCaseName  = record.get(2);


            for (TestSuite testSuite : detector.getTestSuites()) {
                if (!testSuite.getTestClassName().equals(testSuiteName)) {
                    continue;
                }
                if (!testSuite.getCu().getPackage().getName().toString().equals(packageName)) {
                    continue;
                }
                for (TestCase testCase : testSuite.getTestCases()) {
                    if (testCase.getName().equals(testCaseName)) {
                        try {
                            String content = new GenerateEngine(testSuite).generatePUT(testCase.getMethodDeclaration());
                            _outputPUT(packageName, testSuiteName, testCaseName, content);
                        } catch (NullPointerException e) {
                            System.err.println("NullPo At " + testSuiteName + "_" + testCaseName + " when generating PUT");
                            e.printStackTrace();
                            continue;
                        }
                        List<String> similarCUTs = new ArrayList<>();
                        for (int j = 3; j < record.size(); j++) {
                            similarCUTs.add(record.get(j));
                        }
                        try {
                            String content = new GenerateEngine(testSuite).extractOriginalCUTs(testCase.getMethodDeclaration(), similarCUTs);
                            _outputOriginalCUTs(packageName, testSuiteName, testCaseName, content);
                        } catch (NullPointerException e) {
                            System.err.println("NullPo At " + testSuiteName + "_" + testCaseName + " when extracting original CUTs");
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }

    public List<CSVRecord> getDetectResult() {
        return detector.getDetectResults();
    }

    public String getPUT(String packageName, String className, String methodName) {
        try {
            return Files.lines(Paths.get(_getPathToPUT(packageName, className, methodName)))
                    .collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return "";
        }
    }

    public String getOriginalCUTs(String packageName, String className, String methodName) {
        try {
            return Files.lines(Paths.get(_getPathToOriginalCUTs(packageName, className, methodName)))
                    .collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return "";
        }
    }

    private void _outputPUT(String packageName, String className, String methodName, String content) {
        if (!Files.exists(Paths.get(_getPathToPUT(packageName, className, methodName)))) {
            try {
                Files.createDirectories(Paths.get(_getPathToOutputDir(packageName, className, methodName)));
                Files.createFile(Paths.get(_getPathToPUT(packageName, className, methodName)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(_getPathToPUT(packageName, className, methodName)))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void _outputOriginalCUTs(String packageName, String className, String methodName, String content) {
        if (!Files.exists(Paths.get(_getPathToOriginalCUTs(packageName, className, methodName)))) {
            try {
                Files.createDirectories(Paths.get(_getPathToOutputDir(packageName, className, methodName)));
                Files.createFile(Paths.get(_getPathToOriginalCUTs(packageName, className, methodName)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(_getPathToOriginalCUTs(packageName, className, methodName)))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String _getPathToPUT(String packageName, String className, String methodName) {
        return String.join("/", _getPathToOutputDir(packageName, className, methodName), Main.PUT + ".java");
    }
    private String _getPathToOriginalCUTs(String packageName, String className, String methodName) {
        return String.join("/", _getPathToOutputDir(packageName, className, methodName), Main.CUT + ".java");
    }

    private String _getPathToOutputDir(String packageName, String className, String testName) {
        return String.join("/", project.getOutputDir().getName(), project.getProjectId(), GENERATE_DIR, packageName, className, testName);
    }
}
