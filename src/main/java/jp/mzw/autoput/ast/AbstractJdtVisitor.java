package jp.mzw.autoput.ast;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TK on 7/25/17.
 */
public class AbstractJdtVisitor extends ASTVisitor {
    protected TreeContext context = new TreeContext();
    private Deque<ITree> trees = new ArrayDeque<>();
    private Map<ITree, ASTNode> astNodeMap;
    public AbstractJdtVisitor() {
        super(true);
        astNodeMap = new HashMap<>();
    }
    public TreeContext getTreeContext() {
        return context;
    }

    public Map<ITree, ASTNode> getAstNodeMap() {
        return astNodeMap;
    }
    protected void pushNode(ASTNode n, String label) {
        int type = n.getNodeType();
        String typeName = n.getClass().getSimpleName();
        push(type, typeName, label, n.getStartPosition(), n.getLength(), n);
    }
    private void push(int type, String typeName, String label, int startPosition, int length, ASTNode n) {
        ITree t = context.createTree(type, label, typeName);
        t.setPos(startPosition);
        t.setLength(length);
        if (trees.isEmpty())
            context.setRoot(t);
        else {
            ITree parent = trees.peek();
            t.setParentAndUpdateChildren(parent);
        }
        astNodeMap.put(t, n);
        trees.push(t);
    }
    protected ITree getCurrentParent() {
        return trees.peek();
    }
    protected void popNode() {
        trees.pop();
    }
}
