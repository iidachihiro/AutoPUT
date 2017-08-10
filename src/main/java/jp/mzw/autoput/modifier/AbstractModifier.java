package jp.mzw.autoput.modifier;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.TestSuite;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 7/28/17.
 */
public abstract class AbstractModifier {
    protected TestSuite testSuite;

    public AbstractModifier(TestSuite testSuite) {
        this.testSuite = testSuite;

    }

    public CompilationUnit getCompilationUnit() {
        return this.testSuite.getCu();
    }

    abstract public void modify(MethodDeclaration origin);

    public Map<MethodDeclaration, List<MethodDeclaration>> detect() {
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
        }
        return true;
    }
}
