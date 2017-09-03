package jp.mzw.autoput;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestCase;
import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.modifier.AbstractModifier;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 8/24/17.
 */
public class Evaluation {
    private static Logger LOGGER = LoggerFactory.getLogger(Evaluation.class);

    public static final String CONFIG_FILENAME = "config.properties";

    protected static final String DETECT_RESULT = "detect_result.csv";

    protected static final String HEADERS = "headers.file";
    protected static final String SOUCERERCC_RESULT = "tokensclones_index_WITH_FILTER.txt";
    protected static final String CONVERTIBLE_TEST_CASES = "convertible_test_cases.csv";
    protected static final String CONVERTER_RESULTS = "converter_results.csv";
    protected static final String[] PROJECTS =
//            {"commons-codec"};
            {"commons-codec", "commons-collections", "commons-math", "joda-time", "jdom"};
    
    protected static final String[] THRESHOLDS =
        {"6.0"};
//        {"6.0", "7.0", "8.0", "9.0"};

    protected static Project project;

    public static void main(String[] args) {
        String command = args[0];
        if (command.equals("detect")) {
            try {
                detectEvaluation();
            } catch (IOException e) {
                LOGGER.error("Error: {} in detect evaluation.", e.getClass());
            }
        } else if (command.equals("convert")) {
            convertEvaluation();
        } else {
            System.out.println("Illegal usage.");
        }

    }

    private static void detectEvaluation() throws IOException {
        for (String projectId : PROJECTS) {
            // set a subject project
            project = new Project(projectId).setConfig(CONFIG_FILENAME);
            LOGGER.debug("getHeaders()");
            List<CSVRecord> headers = getHeaders();
            LOGGER.debug("getTestSuites()");
            List<TestSuite> testSuites = project.getTestSuites();
            // テストメソッドのMapを作る．
            LOGGER.debug("テストメソッドのMapを作る．");
            Map<MethodDeclaration, String> methodIds = new HashMap<>();
            for (CSVRecord record : headers) {
                String[] pathsToFile = record.get(1).split("/");
                String className = pathsToFile[pathsToFile.length - 1];
                className = className.substring(0, className.length() - ".java".length());
                for (TestSuite testSuite : testSuites) {
                    if (!testSuite.getTestClassName().equals(className)) {
                        continue;
                    }
                    int startLine = Integer.valueOf(record.get(2));
                    CompilationUnit cu = testSuite.getCu();
                    for (MethodDeclaration method : ASTUtils.getAllMethods(cu)) {
                        if (startLine == cu.getLineNumber(method.getStartPosition()) + 1
                                || startLine == cu.getLineNumber(method.getStartPosition())) {
                            methodIds.put(method, record.get(0));
                        }
                    }
                }
            }
            // Answer Results
            LOGGER.debug("getAnswers()");
            List<Pair<String, String>> answers = getAnswers();
            System.out.println("Answer Size: " + answers.size());
            // AutoPUT Results
            LOGGER.debug("AutoPUT Results");
            int truePositive = 0;
            List<Pair<String, String>> detected = getAutoPutDetectResults();
            for (Pair<String, String> answer : answers) {
                String answer1 = answer.getLeft();
                String answer2 = answer.getRight();
                for (Pair<String, String> autoPutResult : detected) {
                    String id1 = autoPutResult.getLeft();
                    String id2 = autoPutResult.getRight();
                    if ((answer1.equals(id1) && answer2.equals(id2))
                            || (answer1.equals(id2) && answer2.equals(id1))) {
                        truePositive++;
                        break;
                    }
                }
            }
            System.out.println("Project: " + projectId);
            System.out.println("AutoPUT");
            System.out.println("Detected Size: " + detected.size());
            System.out.println("Recall: " + ((double) truePositive / answers.size()) * 100);
            System.out.println("Precision: " + ((double) truePositive / detected.size() * 100));

            // To evaluate sourcererCC, get answers' ids.
            LOGGER.debug("To evaluate sourcererCC, get answers' ids.");
            List<Pair<String, String>> answerIds = new ArrayList<>();
            for (Pair<String, String> pair : answers) {
                String leftId = null;
                String rightId = null;
                for (MethodDeclaration method : methodIds.keySet()) {
                    if (method.getName().getIdentifier().equals(pair.getLeft())) {
                        leftId = methodIds.get(method);
                    } else if (method.getName().getIdentifier().equals(pair.getRight())) {
                        rightId = methodIds.get(method);
                    }
                    if (leftId != null && rightId != null) {
                        break;
                    }
                }
                answerIds.add(new ImmutablePair<>(leftId, rightId));
            }
            // SourcererCC Results
            LOGGER.debug("SourcererCC Results");
            System.out.println("SourcererCC");
            for (String threshold : THRESHOLDS) {
                truePositive = 0;
                for (Pair<String, String> answer : answerIds) {
                    String answer1 = answer.getLeft();
                    String answer2 = answer.getRight();
                    List<CSVRecord> truePositives = new ArrayList<>();
                    for (CSVRecord record : getSourcererCCResults(threshold)) {
                        String id1 = record.get(0);
                        String id2 = record.get(1);
                        if (answer1 == null || answer2 == null) {
                            continue;
                        }
                        if (id1 == null || id2 == null) {
                            System.out.println(record);
                            continue;
                        }
                        if ((answer1.equals(id1) && answer2.equals(id2))
                                || (answer1.equals(id2) && answer2.equals(id1))) {
                            truePositive++;
                            truePositives.add(record);
                            break;
                        }
                    }
                }
                System.out.println("Threshold: " + threshold);
                System.out.println("Recall: " + ((double) truePositive / answerIds.size()  * 100));
                System.out.println("Precision: " + ((double) truePositive / getSourcererCCResults(threshold).size()  * 100));
            }
            System.out.println();
        }
    }

    private static List<Pair<String, String>> getAnswers() {
        List<CSVRecord> records = new ArrayList<>();
        Path path = Paths.get(String.join("/", "experiment", "answer", project.getProjectId(), CONVERTIBLE_TEST_CASES));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Pair<String, String>> ret = new ArrayList<>();
        for (CSVRecord record : records) {
            for (int i = 2; i < record.size(); i++) {
                if (record.get(i).equals("")) {
                    break;
                }
                for (int j = i + 1; j < record.size(); j++) {
                    if (record.get(j).equals("")) {
                        break;
                    }
                    ret.add(new ImmutablePair<>(record.get(i), record.get(j)));
                }
            }
        }
        return ret;
    }

    private static List<CSVRecord> getHeaders() {
        List<CSVRecord> ret = new ArrayList<>();
        Path path = Paths.get(String.join("/", "experiment", "SourcererCC", "data",
                project.getProjectId(), "bookkeping", HEADERS));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            ret = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static List<CSVRecord> getSourcererCCResults(String threshold) {
        List<CSVRecord> ret = new ArrayList<>();
        Path path = Paths.get(String.join("/", "experiment", "SourcererCC", "data",
                project.getProjectId(), "output" + threshold, SOUCERERCC_RESULT));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            ret = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<Pair<String, String>> getAutoPutDetectResults() {
        List<CSVRecord> records = new ArrayList<>();
        Path path = Paths.get(String.join("/", "output", project.getProjectId(), "detect", DETECT_RESULT));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Pair<String, String>> ret = new ArrayList<>();
        for (CSVRecord record : records) {
            for (int i = 1; i < record.size(); i++) {
                for (int j = i + 1; j < record.size(); j++) {
                    ret.add(new ImmutablePair<>(record.get(i), record.get(j)));
                }
            }
        }
        return ret;
    }


    private static void convertEvaluation() {
        for (String project : PROJECTS) {
            List<CSVRecord> records = getConverterAnswers(project);
            List<CSVRecord> correctPuts = new ArrayList<>();
            for (CSVRecord record : records) {
                if (record.get(2).equals("1") && record.get(3).equals("1")) {
                    correctPuts.add(record);
                }
            }
            System.out.println("Project: " + project);
            System.out.println("Precision: " + ((double) correctPuts.size() / records.size()));
            System.out.println();
        }

    }

    private static List<CSVRecord> getConverterAnswers(String project) {
        List<CSVRecord> ret = new ArrayList<>();
        Path path = Paths.get(String.join("/", "experiment", "answer", project, CONVERTER_RESULTS));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            ret = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

}
