package jp.mzw.autoput.modifier;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestCase;
import jp.mzw.autoput.core.TestSuite;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 7/28/17.
 */
public abstract class AbstractModifier {
    protected Project project;
    protected TestSuite testSuite;

    private static final String DETECT_RESULT = "detect_result.csv";
    private static final String DETECT_DIR = "detect";
    private static final String CONVERT_DIR = "convert";
    protected static final String OUTPUT_PATH = "output";

    public AbstractModifier(Project project) {
        this.project = project;
    }
    public AbstractModifier(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public List<TestSuite> getTestSuites() {
        project.prepare();
        return project.getTestSuites();
    }

    protected String getDetectResultDir() {
        return String.join("/", OUTPUT_PATH, project.getProjectId(), DETECT_DIR);
    }
    protected String getDetectResultPath() {
        return String.join("/", OUTPUT_PATH, project.getProjectId(), DETECT_DIR, DETECT_RESULT);
    }

    protected String getConvertResultDir() {
        return String.join("/", OUTPUT_PATH, project.getProjectId(), CONVERT_DIR);
    }

    abstract public void modify(MethodDeclaration method);

    public void modify() {
        // DETECT_RESULTを読み込む
        List<CSVRecord> records = _getDetectResults();
        // modifyしていく
        for (CSVRecord record : records) {
            String testSuiteName = record.get(0);
            String testCaseName  = record.get(1);
            if (Files.exists(Paths.get(getConvertResultDir() + "/"
                    + testSuiteName + "_" + testCaseName + ".txt"))) {
                continue;
            } else {
                try {
                    Files.createDirectories(Paths.get(getConvertResultDir()));
                    Files.createFile(Paths.get(getConvertResultDir() + "/"
                            + testSuiteName + "_" + testCaseName + ".txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (TestSuite testSuite : getTestSuites()) {
                if (!testSuite.getTestClassName().equals(testSuiteName)) {
                    continue;
                }
                for (TestCase testCase : testSuite.getTestCases()) {
                    if (testCase.getName().equals(testCaseName)) {
                        new ParameterizedModifierBase(project, testSuite).modify(testCase.getMethodDeclaration());
                        break;
                    }
                }
            }
        }
    }

    private List<CSVRecord> _getDetectResults() {
        List<CSVRecord> ret = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(getDetectResultPath()), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            ret = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }



    public void detect() {
        if (Files.exists(Paths.get(getDetectResultPath()))) {
            System.out.println("DetectResult already exists!");
            return;
        } else {
            try {
                Files.createDirectories(Paths.get(getDetectResultDir()));
                Files.createFile(Paths.get(getDetectResultPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<String> contents = new ArrayList<>();
        for (TestSuite testSuite : getTestSuites()) {
            if (testSuite.alreadyParameterized()) {
                continue;
            }
            Map<MethodDeclaration, List<MethodDeclaration>> detected = detect(testSuite);
            for (MethodDeclaration method : detected.keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.join(",", testSuite.getTestClassName(), method.getName().getIdentifier()));
                for (MethodDeclaration similarMethod : detected.get(method)) {
                    sb.append(",").append(similarMethod.getName().getIdentifier());
                }
                sb.append("\n");
                contents.add(sb.toString());
            }
        }
        _outputDetectResults(contents);
    }

    private void _outputDetectResults(List<String> contents) {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getDetectResultPath()), StandardCharsets.UTF_8)) {
            for (String content : contents) {
                bw.write(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<MethodDeclaration, List<MethodDeclaration>> detect(TestSuite testSuite) {
        int size = testSuite.getTestCases().size();
        Map<MethodDeclaration, List<MethodDeclaration>> detected = new HashMap<>();
        boolean[] registered = new boolean[size];
        for (int i = 0; i < size; i++) {
            if (registered[i]) {
                continue;
            }
            for (int j = size - 1; i < j; j--) {
                if (registered[j]) {
                    continue;
                }
                MethodDeclaration src = testSuite.getTestCases().get(i).getMethodDeclaration();
                MethodDeclaration dst = testSuite.getTestCases().get(j).getMethodDeclaration();
                // Compare the ASTs
                if (similarAST(src, dst)) {
                    MethodDeclaration key = testSuite.getTestCases().get(i).getMethodDeclaration();
                    List<MethodDeclaration> similarMethods = detected.get(key);
                    if (similarMethods == null) {
                        similarMethods = new ArrayList<>();
                        registered[i] = true;
                        detected.put(key, similarMethods);
                    }
                    similarMethods.add(testSuite.getTestCases().get(j).getMethodDeclaration());
                    registered[j] = true;
                }
            }
        }
        return detected;
    }

    protected boolean similarAST(ASTNode node1, ASTNode node2) {
        List<ASTNode> nodes1 = ASTUtils.getAllNodes(node1);
        List<ASTNode> nodes2 = ASTUtils.getAllNodes(node2);
        if (nodes1.size() != nodes2.size()) {
            return false;
        }
        for (int i = 0; i < nodes1.size(); i++) {
            ASTNode tmp1 = nodes1.get(i);
            ASTNode tmp2 = nodes2.get(i);
            if (!tmp1.getClass().equals(tmp2.getClass())) {
                return false;
            }
            if (tmp1 instanceof MethodInvocation) {
                MethodInvocation method1 = (MethodInvocation) tmp1;
                MethodInvocation method2 = (MethodInvocation) tmp2;
                if (!method1.getName().getIdentifier().equals(method2.getName().getIdentifier())) {
                    return false;
                }
            }
        }
        return true;
    }
}
