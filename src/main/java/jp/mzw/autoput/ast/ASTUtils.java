package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

/**
 * Created by TK on 7/28/17.
 */
public class ASTUtils {
    private static final AST ast = AST.newAST(AST.JLS8);

    public static Modifier getPublicModifier() {
        return ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
    }
    public static Modifier getPrivateModifier() {
        return ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
    }
    public static Modifier getStaticModifier() {
        return ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
    }

    public static SingleMemberAnnotation getRunWithAnnotation() {
        return createSingleMemberAnnotation("RunWith", "Parameterized");
    }
    public static MarkerAnnotation getParametersAnnotation() {
        return createMarkerAnnotation("Parameters");
    }
    public static MarkerAnnotation getTestAnnotation() {
        return createMarkerAnnotation("Test");
    }

    private static SingleMemberAnnotation createSingleMemberAnnotation(String name, String type) {
        SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
        annotation.setTypeName(ast.newName(name));
        TypeLiteral typeLiteral = ast.newTypeLiteral();
        typeLiteral.setType(ast.newSimpleType(ast.newSimpleName(type)));
        annotation.setValue(typeLiteral);
        return annotation;
    }
    private static MarkerAnnotation createMarkerAnnotation(String name) {
        MarkerAnnotation annotation = ast.newMarkerAnnotation();
        annotation.setTypeName(ast.newName(name));
        return annotation;
    }

    public static List<MethodInvocation> getAssertionMethods(ASTNode node) {
        AllAssertionVisitor visitor = new AllAssertionVisitor();
        node.accept(visitor);
        return visitor.getAssertions();
    }

    public static List<SimpleName> getAllSimpleNames(ASTNode node) {
        AllSimpleNameVisitor visitor = new AllSimpleNameVisitor();
        node.accept(visitor);
        return visitor.getSimpleNames();
    }
}
