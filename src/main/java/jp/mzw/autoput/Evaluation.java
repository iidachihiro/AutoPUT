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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

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
    public static final String CONFIG_FILENAME = "config.properties";

    protected static final String HEADERS = "headers.file";
    protected static final String RESULT = "tokensclones_index_WITH_FILTER.txt";
    protected static final String[] PROJECTS =
            {"commons-codec"};
//            {"commons-codec", "commons-collections", "commons-math", "joda-time", "jdom"};

    protected static Project project;

    public static void main(String[] args) throws IOException {
        for (String projectId : PROJECTS) {
            // set a subject project
            project = new Project(projectId).setConfig(CONFIG_FILENAME);
            List<CSVRecord> headers = getHeaders();
            List<TestSuite> testSuites = project.getTestSuites();

            // テストメソッドのMapを作る．
            Map<MethodDeclaration, String> methods = new HashMap<>();
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
                            methods.put(method, record.get(0));
                        }
                    }
                }
            }
            // Detection Results
            List<Pair<String, String>> autoPutResults = new ArrayList<>();
            for (Pair<MethodDeclaration, MethodDeclaration> pair : detect()) {
                String id1 = methods.get(pair.getLeft());
                String id2 = methods.get(pair.getRight());
                autoPutResults.add(new ImmutablePair<>(id1, id2));
            }
            int truePositive = 0;
            for (CSVRecord record : getDetectionResults()) {
                for (Pair<String, String> pair : autoPutResults) {
                    String id1 = pair.getLeft();
                    String id2 = pair.getRight();
                    if ((record.get(0).equals(id1) && record.get(1).equals(id2))
                            || (record.get(1).equals(id1) && record.get(0).equals(id2))) {
                        truePositive++;
                    }
                }
            }
            System.out.println("Recall: " + ((double) truePositive / autoPutResults.size()));
        }
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

    private static List<CSVRecord> getDetectionResults() {
        List<CSVRecord> ret = new ArrayList<>();
        Path path = Paths.get(String.join("/", "experiment", "SourcererCC", "data",
                project.getProjectId(), "output", RESULT));
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            ret = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<Pair<MethodDeclaration, MethodDeclaration>> detect() {
        List<Pair<MethodDeclaration, MethodDeclaration>> detected = new ArrayList<>();
        List<TestSuite> testSuites = project.getTestSuites();
        for (int i = 0; i < testSuites.size(); i++) {
            for (int j = i + 1; j < testSuites.size(); j++) {
                TestSuite testSuite1 = testSuites.get(i);
                TestSuite testSuite2 = testSuites.get(j);
                for (int k = 0; k < testSuite1.getTestCases().size(); k++) {
                    for (int l = 0; l < testSuite2.getTestCases().size(); l++) {
                        TestCase testCase1 = testSuite1.getTestCases().get(k);
                        MethodDeclaration method1 = testCase1.getMethodDeclaration();
                        TestCase testCase2 = testSuite2.getTestCases().get(l);
                        MethodDeclaration method2 = testCase2.getMethodDeclaration();
                        if (AbstractModifier.similarAST(method1, method2)) {
                            detected.add(new ImmutablePair<>(method1, method2));
                        }
                    }
                }

            }
        }
        return detected;
    }
}
