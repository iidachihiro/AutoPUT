package jp.mzw.autoput.modifier;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.TestSuite;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

/**
 * Created by TK on 8/3/17.
 */
public class ParamterizedModifier extends AbstractModifier {

    public ParamterizedModifier(TestSuite testSuite) {
        super(testSuite);
    }

    public void paramterized() {
        Map<MethodDeclaration, List<MethodDeclaration>> maps = detect();
        for (MethodDeclaration key : maps.keySet()) {
            if (!key.getName().toString().equals("testParseSimpleWithDecimals")) {
                continue;
            }
            modify(maps.get(key));
        }
    }
    @Override
    public void modify(List<MethodDeclaration> testMethods) {
        MethodDeclaration origin = testMethods.get(0);
        Expression expected = getExpectedInAssertion(origin);
        Expression actual = getActualInAssertion(origin);
        List<VariableDeclarationStatement> statements = new ArrayList<>();
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationStatement node) {
                statements.add(node);
                return super.visit(node);
            }
        };
        origin.accept(visitor);
        System.out.println("getLiteral(expected, statements): " + getLiteral(expected, statements));
        System.out.println("getLiteral(actual, statements): " + getLiteral(actual, statements));
    }

    private Expression getExpectedInAssertion(ASTNode node) {
        List<MethodInvocation> assertions = ASTUtils.getAssertionMethods(node);
        MethodInvocation assertion = assertions.get(0);
        if (assertion.arguments().size() == 2) {
            return (Expression) assertion.arguments().get(0);
        } else {
            return (Expression) assertion.arguments().get(1);
        }
    }

    private Expression getActualInAssertion(ASTNode node) {
        List<MethodInvocation> assertions = ASTUtils.getAssertionMethods(node);
        MethodInvocation assertion = assertions.get(0);
        if (assertion.arguments().size() == 2) {
            return (Expression) assertion.arguments().get(1);
        } else {
            return (Expression) assertion.arguments().get(2);
        }
    }

    private Expression getLiteral(Expression expression, List<VariableDeclarationStatement> statements) {
        if (isLiteral(expression)) {
            return expression;
        }
        for (VariableDeclarationStatement statement : statements) {
            for (Object object : statement.fragments()) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
                if (fragment.getName().toString().equals(expression.toString())) {
                    return _getLiteral(fragment.getInitializer(), statements);
                }
            }
        }
        return null;
    }

    private Expression _getLiteral(Expression expression, List<VariableDeclarationStatement> statements) {
        if (isLiteral(expression) || (expression instanceof InfixExpression)) {
            return expression;
        } else if (expression instanceof MethodInvocation) {
            MethodInvocation methodInvocation = (MethodInvocation) expression;
            return getLiteral((Expression) methodInvocation.arguments().get(0), statements);
        }  else if (expression instanceof ClassInstanceCreation) {
            ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) expression;
            return getLiteral((Expression) classInstanceCreation.arguments().get(0), statements);
        }
        System.out.println(expression);
        System.out.println(expression.getClass());
        return null;
    }

    private boolean isLiteral(Expression expression) {
        return (expression instanceof BooleanLiteral) || (expression instanceof CharacterLiteral)
                || (expression instanceof NullLiteral)  || (expression instanceof NumberLiteral)
                || (expression instanceof StringLiteral);
    }
}
