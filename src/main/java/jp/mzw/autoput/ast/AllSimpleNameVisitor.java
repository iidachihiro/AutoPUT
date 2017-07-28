package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/28/17.
 */
public class AllSimpleNameVisitor extends ASTVisitor {
    private List<SimpleName> simpleNames;

    public AllSimpleNameVisitor() {
        super();
        simpleNames = new ArrayList<>();
    }

    public List<SimpleName> getSimpleNames() {
        return this.simpleNames;
    }

    @Override
    public boolean visit(SimpleName node) {
        if (!simpleNames.contains(node)) {
            simpleNames.add(node);
        }
        return super.visit(node);
    }
}
