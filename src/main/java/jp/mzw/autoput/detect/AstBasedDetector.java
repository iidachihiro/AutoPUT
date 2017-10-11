package jp.mzw.autoput.detect;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestSuite;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AstBasedDetector extends DetectorBase {
    public AstBasedDetector() {
        super();
    }

    @Override
    protected List<String> _detect(Project project) {
        List<String> detectedResults = new ArrayList<>();
        for (TestSuite testSuite : project.getParsedTestSuites()) {
            if (testSuite.getClassDeclaration() == null) {
                continue;
            }
            if (testSuite.alreadyParameterized()) {
                continue;
            }
            Map<MethodDeclaration, List<MethodDeclaration>> detected = _astBasedDetect(testSuite);
            for (MethodDeclaration method : detected.keySet()) {
                StringBuilder sb = new StringBuilder();
                // package,class name,method name(cut0), cut1, cut2, ...
                sb.append(String.join(",", testSuite.getCu().getPackage().getName().toString(), testSuite.getTestClassName(), method.getName().getIdentifier()));
                for (MethodDeclaration similarMethod : detected.get(method)) {
                    sb.append(",").append(similarMethod.getName().getIdentifier());
                }
                sb.append("\n");
                detectedResults.add(sb.toString());
            }
        }
        return detectedResults;
    }

    private Map<MethodDeclaration, List<MethodDeclaration>> _astBasedDetect(TestSuite testSuite) {
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
                // Compare both ASTs
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

    private static boolean similarAST(ASTNode src1, ASTNode src2) {
        // flatten minus number literals
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
                // if method nodes, we consider the names
                MethodInvocation tmp1 = (MethodInvocation) node1;
                MethodInvocation tmp2 = (MethodInvocation) node2;
                if (!tmp1.getName().getIdentifier().equals(tmp2.getName().getIdentifier())) {
                    return false;
                }
            }
            if (node1 instanceof InfixExpression) {
                // if infix expression, we consider the operators
                InfixExpression tmp1 = (InfixExpression) node1;
                InfixExpression tmp2 = (InfixExpression) node2;
                if (!tmp1.getOperator().equals(tmp2.getOperator())) {
                    return false;
                }
            }
            if (node1 instanceof PrefixExpression) {
                // if prefix expression, we consider the operators
                PrefixExpression tmp1 = (PrefixExpression) node1;
                PrefixExpression tmp2 = (PrefixExpression) node2;
                if (!tmp1.getOperator().equals(tmp2.getOperator())) {
                    return false;
                }
            }
            if (node1 instanceof PostfixExpression) {
                // if postfix expression, we consider the operators
                PostfixExpression tmp1 = (PostfixExpression) node1;
                PostfixExpression tmp2 = (PostfixExpression) node2;
                if (!tmp1.getOperator().equals(tmp2.getOperator())) {
                    return false;
                }
            }
            if (node1 instanceof SimpleType) {
                // if simple type, we consider the names
                SimpleType tmp1 = (SimpleType) node1;
                SimpleType tmp2 = (SimpleType) node2;
                if (!tmp1.getName().toString().equals(tmp2.getName().toString())) {
                    return false;
                }
            }
            if (node1 instanceof ClassInstanceCreation) {
                // if class instance creation, we consider the names
                ClassInstanceCreation tmp1 = (ClassInstanceCreation) node1;
                ClassInstanceCreation tmp2 = (ClassInstanceCreation) node2;
                if (!tmp1.getType().toString().equals(tmp2.getType().toString())) {
                    return false;
                }
            }
            if (node1 instanceof SimpleName) {
                SimpleName tmp1 =(SimpleName) node1;
                SimpleName tmp2 =(SimpleName) node2;

                if (tmp1.isDeclaration() && tmp2.isDeclaration()) {
                    // ignore simple names declaring in this method
                    continue;
                }
                if (ASTUtils.canExtract(tmp1) && ASTUtils.canExtract(tmp2)
                        && tmp1.resolveTypeBinding().getName().equals(tmp2.resolveTypeBinding().getName())
                        && (ASTUtils.isConst(tmp1) && ASTUtils.isConst(tmp1))) {
                    // ignore literal nodes and const variable nodes
                    continue;
                }
                if (!tmp1.getIdentifier().equals(tmp2.getIdentifier())) {
                    // if simple name, we consider the names
                    return false;
                }
            }
        }
        return true;
    }
}
