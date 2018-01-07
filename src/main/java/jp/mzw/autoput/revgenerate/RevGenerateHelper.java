package jp.mzw.autoput.revgenerate;

import jp.mzw.autoput.ast.ASTUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;

public class RevGenerateHelper {

    protected static MethodDeclaration getTheory(CompilationUnit cu) {
        List<MethodDeclaration> methods = ASTUtils.getAllMethods(cu);

        for (MethodDeclaration method : methods) {
            if (method.modifiers().isEmpty()) {
                continue;
            }
            List<IExtendedModifier> modifiers = method.modifiers();
            for (IExtendedModifier modifier : modifiers) {
                if ((modifier instanceof Annotation) && ASTUtils.isTheoryAnnotation((Annotation) modifier)) {
                    return method;
                }
            }
        }
        return null;
    }

    protected static List<Pair<Expression, Expression>> getDataPoints(CompilationUnit cu) {
        List<Pair<Expression, Expression>> ret = new ArrayList<>();

        List<Expression> inputExpressions = new ArrayList<>();
        List<Expression> expectedExpressions = new ArrayList<>();
        List<FieldDeclaration> fieldDeclaratoins = ASTUtils.getAllFieldDeclaratoins(cu);
        for (FieldDeclaration fieldDeclaration : fieldDeclaratoins) {
            for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) fieldDeclaration.fragments()) {
                if (fragment.getName().getIdentifier().startsWith("INPUT")) {
                    inputExpressions.add(fragment.getInitializer());
                } else if (fragment.getName().getIdentifier().startsWith("EXPECTED")) {
                    expectedExpressions.add(fragment.getInitializer());
                }
            }
        }
        for (int i = 0; i < inputExpressions.size(); i++) {
            Expression inputExpression  = inputExpressions.get(i);
            Expression expectedExpression  = expectedExpressions.get(i);
            ret.add(new ImmutablePair<>(inputExpression, expectedExpression));
        }

        return ret;
    }
}
