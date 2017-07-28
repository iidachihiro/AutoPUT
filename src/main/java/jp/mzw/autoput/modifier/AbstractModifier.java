package jp.mzw.autoput.modifier;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.heuristic.LcsMatcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import jp.mzw.autoput.ast.JdtVisitor;
import jp.mzw.autoput.core.TestCase;
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

    abstract public void modify(List<MethodDeclaration> detected);

    public Map<MethodDeclaration, List<MethodDeclaration>> detect() {
        int size = testSuite.getTestCases().size();
        Map<MethodDeclaration, List<MethodDeclaration>> detected = new HashMap<>();
        boolean[] registered = new boolean[size];
        for (int i = 0; i < size; i++) {
            if (registered[i]) {
                continue;
            }
            ITree src = getITreeWithLessLabel(testSuite.getTestCases().get(i).getMethodDeclaration());
            for (int j = size - 1; i < j; j--) {
                if (registered[j]) {
                    continue;
                }
                ITree dst = getITreeWithLessLabel(testSuite.getTestCases().get(j).getMethodDeclaration());
                List<Action> actions = getActions(src, dst);
                if (actions.isEmpty()) {
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

    protected List<Action> getActions(ITree src, ITree dst) {
        LcsMatcher m = new LcsMatcher(src, dst, new MappingStore());
        m.match();
        ActionGenerator ag = new ActionGenerator(src, dst, m.getMappings());
        ag.generate();
        return ag.getActions();
    }

    private ITree getITreeWithLessLabel(ASTNode method) {
        JdtVisitor visitor = JdtVisitor.createJdtVisitorWithLessLabel();
        method.accept(visitor);
        ITree tree = visitor.getTreeContext().getRoot();
        tree.refresh();
        TreeUtils.postOrderNumbering(tree);
        return tree;
    }
}
