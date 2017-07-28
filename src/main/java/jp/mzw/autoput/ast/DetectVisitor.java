package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/26/17.
 */
public class DetectVisitor extends ASTVisitor {
    private List<MethodInvocation> assertions;
    private List<VariableDeclarationFragment> variableDeclarationFragments;
    private List<SimpleName> simpleNames;

    public DetectVisitor() {
        super();
        assertions = new ArrayList<>();
        variableDeclarationFragments = new ArrayList<>();
        simpleNames = new ArrayList<>();
    }

    public List<MethodInvocation> getAssertions() {
        return this.assertions;
    }
    public List<VariableDeclarationFragment> getVariableDeclarationFragments() {
        return this.variableDeclarationFragments;
    }
    public List<SimpleName> getSimpleNames() {
        return this.simpleNames;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (node.getName().toString().startsWith("assert")
                && !assertions.contains(node)) {
            assertions.add(node);
        }
        return super.visit(node);
    }
    @Override
    public boolean visit(VariableDeclarationFragment node) {
        if (!variableDeclarationFragments.contains(node)) {
            variableDeclarationFragments.add(node);
        }
        return super.visit(node);
    }
    @Override
    public boolean visit(SimpleName node) {
        if (!simpleNames.contains(node)) {
            simpleNames.add(node);
        }
        return super.visit(node);
    }
}
