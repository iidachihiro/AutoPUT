package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Created by TK on 7/21/17.
 */
public class ClassDeclarationFindVisitor extends ASTVisitor {
    private TypeDeclaration classDeclaration;

    public ClassDeclarationFindVisitor() {
        super();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface()) {
            classDeclaration = node;
        }
        return super.visit(node);
    }

    public TypeDeclaration getClassDeclaration() {
        return this.classDeclaration;
    }
}
