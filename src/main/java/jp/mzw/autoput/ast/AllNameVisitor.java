package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/28/17.
 */
public class AllNameVisitor extends ASTVisitor {
    private List<Name> names;

    public AllNameVisitor() {
        super();
        names = new ArrayList<>();
    }

    public List<Name> getNames() {
        return this.names;
    }

    @Override
    public boolean visit(SimpleName node) {
        if (!names.contains(node)) {
            names.add(node);
        }
        return super.visit(node);
    }
}
