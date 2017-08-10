package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/28/17.
 */
public class AllAssertionVisitor extends ASTVisitor {
    private List<MethodInvocation> assertions;
    public AllAssertionVisitor() {
        super();
        assertions = new ArrayList<>();
    }

    public List<MethodInvocation> getAssertions() {
        return this.assertions;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (ASTUtils.isAssetionMethod(node) && !assertions.contains(node)) {
            assertions.add(node);
        }
        return super.visit(node);
    }
}
