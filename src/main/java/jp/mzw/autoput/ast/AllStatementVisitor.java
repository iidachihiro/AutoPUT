package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.ArrayList;
import java.util.List;

public class AllStatementVisitor extends ASTVisitor {
    private List<Statement> statements;
    public AllStatementVisitor() {
        super();
        statements = new ArrayList<>();
    }

    public List<Statement> getStatments() {
        return this.statements;
    }

    @Override
    public boolean visit(AssertStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(Block node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(BreakStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(ConstructorInvocation node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(ContinueStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(DoStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(EmptyStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(EnhancedForStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(ExpressionStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(ForStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(IfStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(LabeledStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(ReturnStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(SuperConstructorInvocation node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(SwitchCase node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(SwitchStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(SynchronizedStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(ThrowStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(TryStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(TypeDeclarationStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(VariableDeclarationStatement node) {
        statements.add(node);
        return super.visit(node);
    }
    @Override
    public boolean visit(WhileStatement node) {
        statements.add(node);
        return super.visit(node);
    }
}
