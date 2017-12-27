package jp.mzw.autoput.detect;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestSuite;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class Detector {
    protected static Logger LOGGER = LoggerFactory.getLogger(Detector.class);
    protected Project project;

    private static final String DETECT_RESULT = "detect_result.csv";
    private static final String DETECT_DIR = "detect";

    public Detector(Project project) {
        this.project = project;
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
            Map<MethodDeclaration, List<MethodDeclaration>> detected = _detect(testSuite);
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

    public List<CSVRecord> getDetectResults() {
        List<CSVRecord> ret = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(getDetectResultPath()), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            ret = parser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public List<TestSuite> getTestSuites() {
        project.prepare();
        return project.getTestSuites();
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

    private String getDetectResultDir() {
        return String.join("/", project.getOutputDir().getName(), project.getProjectId(), DETECT_DIR);
    }
    private String getDetectResultPath() {
        return String.join("/", getDetectResultDir(), DETECT_RESULT);
    }

    private Map<MethodDeclaration, List<MethodDeclaration>> _detect(TestSuite testSuite) {
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
}
