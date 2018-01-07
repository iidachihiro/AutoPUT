package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class AllTypeDeclarationVisitor extends ASTVisitor {
    private List<TypeDeclaration> typeDeclarationList;

    public AllTypeDeclarationVisitor() {
        super();
        typeDeclarationList = new ArrayList<>();
    }

    public List<TypeDeclaration> getTypeDeclarationList() {
        return typeDeclarationList;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        typeDeclarationList.add(node);
        return super.visit(node);
    }
}
