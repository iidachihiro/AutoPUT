package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

/**
 * Created by TK on 7/28/17.
 */
public class ASTUtils {

    public static Modifier getPublicModifier(AST ast) {
        return ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
    }
    public static Modifier getPrivateModifier(AST ast) {
        return ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
    }
    public static Modifier getProtectedModifier(AST ast) {
        return ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
    }
    public static Modifier getStaticModifier(AST ast) {
        return ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
    }

    public static SingleMemberAnnotation getRunWithAnnotation(AST ast) {
        return createSingleMemberAnnotation(ast, "RunWith", "Parameterized");
    }
    public static MarkerAnnotation getParametersAnnotation(AST ast) {
        return createMarkerAnnotation(ast, "Parameters");
    }
    public static MarkerAnnotation getTestAnnotation(AST ast) {
        return createMarkerAnnotation(ast, "Test");
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

    public static Statement getParentStatement(ASTNode node) {
        if (node instanceof Statement) {
            return (Statement) node;
        }
        while (node.getParent() != null) {
            node = node.getParent();
            if (node instanceof Statement) {
                return (Statement) node;
            }
        }
        return null;
    }





    /*
    =========================== private ==================================
     */

    private static SingleMemberAnnotation createSingleMemberAnnotation(AST ast, String name, String type) {
        SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
        annotation.setTypeName(ast.newName(name));
        TypeLiteral typeLiteral = ast.newTypeLiteral();
        typeLiteral.setType(ast.newSimpleType(ast.newSimpleName(type)));
        annotation.setValue(typeLiteral);
        return annotation;
    }
    private static MarkerAnnotation createMarkerAnnotation(AST ast, String name) {
        MarkerAnnotation annotation = ast.newMarkerAnnotation();
        annotation.setTypeName(ast.newName(name));
        return annotation;
    }
}
