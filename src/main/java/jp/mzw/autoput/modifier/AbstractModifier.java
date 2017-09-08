package jp.mzw.autoput.modifier;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestCase;
import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.experiment.ExperimentUtils;
import jp.mzw.autoput.maven.MavenUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jdt.core.dom.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



/**
 * Created by TK on 7/28/17.
 */
public abstract class AbstractModifier {

    protected static Logger LOGGER = LoggerFactory.getLogger(AbstractModifier.class);
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

    abstract public String modify(MethodDeclaration method);

    public void modify() {
        // DETECT_RESULTを読み込む
        List<CSVRecord> records = _getDetectResults();
        // modifyしていく
        for (CSVRecord record : records) {
            String packageName = record.get(0);
            String testSuiteName = record.get(1);
            String testCaseName  = record.get(2);
            if (Files.exists(Paths.get(getConvertResultDir() + "/" + packageName + "/"
                    + testSuiteName + "/" + testCaseName + ".txt"))) {
                continue;
            } else {
                try {
                    Files.createDirectories(Paths.get(getConvertResultDir() + "/" + packageName + "/" + testSuiteName));
                    Files.createFile(Paths.get(getConvertResultDir() + "/" + packageName + "/"
                            + testSuiteName + "/" + testCaseName + ".txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (TestSuite testSuite : getTestSuites()) {
                if (!testSuite.getTestClassName().equals(testSuiteName)) {
                    continue;
                }
                if (!testSuite.getCu().getPackage().getName().toString().equals(packageName)) {
                    continue;
                }
                for (TestCase testCase : testSuite.getTestCases()) {
                    if (testCase.getName().equals(testCaseName)) {
                        try {
                            String content = new ParameterizedModifierBase(project, testSuite).modify(testCase.getMethodDeclaration());
                            _outputConvertResult(packageName, testSuiteName, testCaseName, content);
                        } catch (NullPointerException e) {
                            System.err.println("NullPo At " + testSuiteName + "_" + testCaseName);
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
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


    private void createSubjectFile(Project project, String packageName, String className, String testName, String mode) {
        ExperimentUtils.createSubjectFileDirs(project.getProjectId(), packageName, className, testName, mode);
        for (TestSuite testSuite : getTestSuites()) {
            if (!testSuite.getTestClassName().equals(className)) {
                continue;
            }
            if (!testSuite.getCu().getPackage().getName().toString().equals(packageName)) {
                continue;
            }
            for (TestCase testCase : testSuite.getTestCases()) {
                if (testCase.getName().equals(testName)) {
                    try {
                        String content = new ParameterizedModifierBase(project, testSuite).experimentalModify(testCase.getMethodDeclaration(), mode);
                        ExperimentUtils.outputConvertResult(project.getProjectId(), packageName, className, testName, mode, content);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }


    public void detect() {
        if (!Files.exists(Paths.get(getDetectResultPath()))) {
            try {
                Files.createDirectories(Paths.get(getDetectResultDir()));
                Files.createFile(Paths.get(getDetectResultPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<String> contents = new ArrayList<>();
        for (TestSuite testSuite : getTestSuites()) {
            if (testSuite.getClassDeclaration() == null) {
                continue;
            }
            if (testSuite.alreadyParameterized()) {
                continue;
            }
            Map<MethodDeclaration, List<MethodDeclaration>> detected = detect(testSuite);
            for (MethodDeclaration method : detected.keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.join(",", testSuite.getCu().getPackage().getName().toString(), testSuite.getTestClassName(), method.getName().getIdentifier()));
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
            for (int j = i + 1; j < size; j++) {
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

    public static boolean similarAST(ASTNode src1, ASTNode src2) {
        List<ASTNode> nodes1 = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(src1));
        List<ASTNode> nodes2 = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(src2));
        if (nodes1.size() != nodes2.size()) {
            return false;
        }
        for (int i = 0; i < nodes1.size(); i++) {
            ASTNode node1 = nodes1.get(i);
            ASTNode node2 = nodes2.get(i);
            if (!node1.getClass().equals(node2.getClass())) {
                return false;
            }
            if (node1 instanceof MethodInvocation) {
                MethodInvocation tmp1 = (MethodInvocation) node1;
                MethodInvocation tmp2 = (MethodInvocation) node2;
                if (!tmp1.getName().getIdentifier().equals(tmp2.getName().getIdentifier())) {
                    return false;
                }
            } else if (node1 instanceof InfixExpression) {
                InfixExpression tmp1 = (InfixExpression) node1;
                InfixExpression tmp2 = (InfixExpression) node2;
                if (!tmp1.getOperator().equals(tmp2.getOperator())) {
                    return false;
                }
            } else if (node1 instanceof PrefixExpression) {
                PrefixExpression tmp1 = (PrefixExpression) node1;
                PrefixExpression tmp2 = (PrefixExpression) node2;
                if (!tmp1.getOperator().equals(tmp2.getOperator())) {
                    return false;
                }
            } else if (node1 instanceof PostfixExpression) {
                PostfixExpression tmp1 = (PostfixExpression) node1;
                PostfixExpression tmp2 = (PostfixExpression) node2;
                if (!tmp1.getOperator().equals(tmp2.getOperator())) {
                    return false;
                }
            } else if (node1 instanceof SimpleType) {
                SimpleType tmp1 = (SimpleType) node1;
                SimpleType tmp2 = (SimpleType) node2;
                if (!tmp1.getName().toString().equals(tmp2.getName().toString())) {
                    return false;
                }
            } else if (node1 instanceof ClassInstanceCreation) {
                ClassInstanceCreation tmp1 = (ClassInstanceCreation) node1;
                ClassInstanceCreation tmp2 = (ClassInstanceCreation) node2;
                if (!tmp1.getType().toString().equals(tmp2.getType().toString())) {
                    return false;
                }
            } else if (node1 instanceof SimpleName) {
                SimpleName tmp1 =(SimpleName) node1;
                SimpleName tmp2 =(SimpleName) node2;
                if (tmp1.isDeclaration() && tmp2.isDeclaration()) {
                    continue;
                }
                if (ASTUtils.canExtract(tmp1) && ASTUtils.canExtract(tmp2)
                        && tmp1.resolveTypeBinding().getName().equals(tmp2.resolveTypeBinding().getName())
                        && (ASTUtils.isConst(tmp1) && ASTUtils.isConst(tmp1))) {
                    continue;
                }
                if (!tmp1.getIdentifier().equals(tmp2.getIdentifier())) {
                    return false;
                }
            }
        }
        return true;
    }


    /* ------------------------- For Experiment ------------------------- */

    public void experiment() {
        // DETECT_RESULTを読み込む
        List<CSVRecord> records = _getDetectResults();
        // modifyしていく
        for (CSVRecord record : records) {
            String packageName = record.get(0);
            String className = record.get(1);
            String testName = record.get(2);
            String[] modes = {"AutoPut", "Origin"};
            if (Files.exists(Paths.get(String.join("/", "jacoco", project.getProjectId(), packageName, className, testName, modes[0])))) {
                continue;
            }
            for (String mode : modes) {
                createSubjectFile(project, packageName, className, testName, mode);
                ExperimentUtils.deploy(project.getProjectId(), packageName, className, testName, mode);
                File subject = project.getProjectDir();
                File mavenHome = project.getMavenHome();
                int compile = -1;
                try {
                    System.out.println("Compile: " + className + "_" + testName);
                    List<String> goal = Arrays.asList("test-compile");
                    compile = MavenUtils.maven(project.getProjectId(), subject, goal, mavenHome, packageName + "/" + className + "/" + testName + "/" + mode + "Test.java");
                    if (compile == 0) {
                        System.out.println("Jacoco: " + className + "_" + testName);
                        _deleteJacocoExec(project);
                        goal = Arrays.asList("jacoco:prepare-agent", "test", "-Dtest=" + mode + "Test", "-DfailIfNoTests=false", "jacoco:report");
                        compile = MavenUtils.maven(project.getProjectId(), subject, goal, mavenHome, packageName + "/" + className + "/" + testName + "/" + mode + "Test.java");
                        if (compile == 0) {
                            _copyJacocoHtml(project, packageName, className, testName, mode);
                        } else {
                            System.out.println("Test Failure");
                            LOGGER.warn("Test Failure: {}" + (packageName + "/" + className + "/" + testName + "/" + mode + "Test.java"));
                        }
                    } else {
                        System.out.println("Compile Failure");
                        LOGGER.warn("Compile Failure: {}" + (packageName + "/" + className + "/" + testName + "/" + mode + "Test.java"));
                        continue;
                    }
                } catch (MavenInvocationException e) {
                    e.printStackTrace();
                } finally {
                    ExperimentUtils.delete(project.getProjectId(), packageName, mode);
                }
                ExperimentUtils.delete(project.getProjectId(), packageName, mode);
            }
        }
    }

    public void evaluation() {
        // DETECT_RESULTを読み込む
        List<CSVRecord> records = _getDetectResults();
        int truePositive = 0;
        int allSize = 0;
        int numOfPut = 0;
        int numOfCut = 0;
        for (CSVRecord record : records) {
            String packageName = record.get(0);
            String className = record.get(1);
            String testName = record.get(2);
            String content = _getJacocoHtml(project.getProjectId(), packageName, className, testName, "AutoPut");
            if (content.equals("")) {
                continue;
            }
            Document document = Jsoup.parse(content);
            Element element = document.select("#coveragetable tfoot tr .ctr2").first();
            int coverageAutoPut = Integer.parseInt(element.text().replace("%", ""));

            content = _getJacocoHtml(project.getProjectId(), packageName, className, testName, "Origin");
            if (content.equals("")) {
                continue;
            }
            document = Jsoup.parse(content);
            element = document.select("#coveragetable tfoot tr .ctr2").first();
            int coverageOrigin = Integer.parseInt(element.text().replace("%", ""));

            if (coverageAutoPut >= coverageOrigin) {
                truePositive++;
                numOfPut++;
                numOfCut += record.size() - 2;
            }
            allSize++;
        }
        System.out.println("All Size: " + allSize);
        System.out.println("Precision: " + ((double) truePositive / allSize));
        System.out.println("Average Num Of Cuts Per Put: " + ((double) numOfCut / numOfPut));
    }


    /* -------------------------- Private --------------------------------- */

    private String _getJacocoHtml(String project, String packageName, String className, String testName, String mode) {
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

    protected void _outputConvertResult(String packageName, String className, String methodName, String content) {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getConvertResultDir() + "/"
                + packageName + "/" + className + "/" + methodName + ".txt"))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void _deleteJacocoExec(Project project) {
        try {
            Files.deleteIfExists(Paths.get(
                    String.join("/", project.getProjectDir().getPath(), "target", "jacoco.exec")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void _copyJacocoHtml(Project project, String packageName, String className, String testName, String mode) {
        if (!Files.exists(Paths.get(String.join("/", "jacoco", project.getProjectId(), packageName, className, testName, mode)))) {
            try {
                Files.createDirectories(Paths.get(
                        String.join("/", "jacoco", project.getProjectId(), packageName, className, testName, mode)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.copy(
                    Paths.get(String.join("/", project.getProjectDir().getPath(), "target", "site", "jacoco", "index.html")),
                    Paths.get(String.join("/", "jacoco", project.getProjectId(), packageName, className, testName, mode, "index.html")),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
