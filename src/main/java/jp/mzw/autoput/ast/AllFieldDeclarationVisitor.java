package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;

import java.util.ArrayList;
import java.util.List;

public class AllFieldDeclarationVisitor extends ASTVisitor {
    private List<FieldDeclaration> fieldDeclarations;

    public AllFieldDeclarationVisitor() {
        super();
        fieldDeclarations = new ArrayList<>();
    }

    public List<FieldDeclaration> getFieldDeclarations() {
        return this.fieldDeclarations;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        if (!fieldDeclarations.contains(node)) {
            fieldDeclarations.add(node);
        }
        return super.visit(node);
    }
}
