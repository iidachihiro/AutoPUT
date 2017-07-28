package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.*;

/**
* Created by TK on 7/25/17.
*/
public class JdtVisitor extends AbstractJdtVisitor {

    boolean lessLabel;

    public JdtVisitor() {
        super();
        this.lessLabel = false;
    }

    public static JdtVisitor createJdtVisitorWithLessLabel() {
        return new JdtVisitor(true);
    }
    public JdtVisitor(boolean lessLabel) {
        super();
        this.lessLabel = lessLabel;
    }
    @Override
    public void preVisit(ASTNode n) {
        if (lessLabel) {
            pushNode(n, getLessLabel(n));
        } else {
            pushNode(n, getLabel(n));
        }
    }

    protected String getLabel(ASTNode n) {
        if (n instanceof Name) return ((Name) n).getFullyQualifiedName();
        if (n instanceof Type) return n.toString();
        if (n instanceof Modifier) return n.toString();
        if (n instanceof StringLiteral) return ((StringLiteral) n).getEscapedValue();
        if (n instanceof NumberLiteral) return ((NumberLiteral) n).getToken();
        if (n instanceof CharacterLiteral) return ((CharacterLiteral) n).getEscapedValue();
        if (n instanceof BooleanLiteral) return ((BooleanLiteral) n).toString();
        if (n instanceof InfixExpression) return ((InfixExpression) n).getOperator().toString();
        if (n instanceof PrefixExpression) return ((PrefixExpression) n).getOperator().toString();
        if (n instanceof PostfixExpression) return ((PostfixExpression) n).getOperator().toString();
        if (n instanceof Assignment) return ((Assignment) n).getOperator().toString();
        if (n instanceof TextElement) return n.toString();
        if (n instanceof TagElement) return ((TagElement) n).getTagName();
        return "";
    }
    protected String getLessLabel(ASTNode n) {
//        if (n instanceof Name) return ((Name) n).getFullyQualifiedName();
        if (n instanceof Type) return n.toString();
        if (n instanceof Modifier) return n.toString();
//        if (n instanceof StringLiteral) return ((StringLiteral) n).getEscapedValue();
//        if (n instanceof NumberLiteral) return ((NumberLiteral) n).getToken();
//        if (n instanceof CharacterLiteral) return ((CharacterLiteral) n).getEscapedValue();
//        if (n instanceof BooleanLiteral) return ((BooleanLiteral) n).toString();
        if (n instanceof InfixExpression) return ((InfixExpression) n).getOperator().toString();
        if (n instanceof PrefixExpression) return ((PrefixExpression) n).getOperator().toString();
        if (n instanceof PostfixExpression) return ((PostfixExpression) n).getOperator().toString();
        if (n instanceof Assignment) return ((Assignment) n).getOperator().toString();
        if (n instanceof TextElement) return n.toString();
        if (n instanceof TagElement) return ((TagElement) n).getTagName();
        return "";
    }
    @Override
    public boolean visit(TagElement e) {
        return true;
    }
    @Override
    public boolean visit(QualifiedName name) {
        return false;
    }
    @Override
    public void postVisit(ASTNode n) {
        popNode();
    }
}
