package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/19/17.
 */
public class AllMethodFindVisitor extends ASTVisitor {
    private List<MethodDeclaration> methods;
    public AllMethodFindVisitor() {
        super();
        this.methods = new ArrayList<>();
    }
    @Override
    public boolean visit(MethodDeclaration node) {
        if(!this.methods.contains(node)) {
            this.methods.add(node);
        }
        return false;
    }
    public List<MethodDeclaration> getFoundMethods() {
        return this.methods;
    }
}
