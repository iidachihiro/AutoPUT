package jp.mzw.tri.ast;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/20/17.
 */
public class AllNameFindVisitor extends ASTVisitor {
    private List<Name> names;
    public AllNameFindVisitor() {
        this.names = new ArrayList<>();
    }
    @Override
    public boolean visit(QualifiedName node) {
        if(!this.names.contains(node)) {
            this.names.add(node);
        }
        return false;
    }
    @Override
    public boolean visit(SimpleName node) {
        if(!this.names.contains(node)) {
            this.names.add(node);
        }
        return false;
    }
    public List<Name> getFoundNames() {
        return this.names;
    }
}
