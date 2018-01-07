package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.QualifiedName;

import java.util.ArrayList;
import java.util.List;

public class AllQualifiedNameVisitor extends ASTVisitor{
    private List<QualifiedName> qualifiedNames;

    public AllQualifiedNameVisitor() {
        super();
        qualifiedNames = new ArrayList<>();
    }

    public List<QualifiedName> getQualifiedNames() {
        return this.qualifiedNames;
    }

    @Override
    public boolean visit(QualifiedName node) {
        if (!qualifiedNames.contains(node)) {
            qualifiedNames.add(node);
        }
        return super.visit(node);
    }
}
