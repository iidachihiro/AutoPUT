package jp.mzw.tri.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

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
