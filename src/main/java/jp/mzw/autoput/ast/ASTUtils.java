package jp.mzw.autoput.ast;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
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
    public static Modifier getStaticModifier(AST ast) {
        return ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
    }

    public static SingleMemberAnnotation getRunWithAnnotation(AST ast) {
        return createSingleMemberAnnotation(ast, "RunWith", "Parameterized");
    }
    public static MarkerAnnotation getParametersAnnotation(AST ast) {
        return createMarkerAnnotation(ast, "Parameters");
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
    public static List<ASTNode> getAllNodes(ASTNode node) {
        AllElementsFindVisitor visitor = new AllElementsFindVisitor();
        node.accept(visitor);
        return visitor.getNodes();
    }
    public static List<ASTNode> getDifferentNodes(ASTNode src, ASTNode dst) {
        List<ASTNode> ret = new ArrayList<>();
        List<ASTNode> nodes1 = ASTUtils.getAllNodes(src);
        List<ASTNode> nodes2 = ASTUtils.getAllNodes(dst);
        if (nodes1.size() != nodes2.size()) {
            return ret;
        }
        for (int i = 0; i < nodes1.size(); i++) {
            ASTNode node1 = nodes1.get(i);
            ASTNode node2 = nodes2.get(i);
            if (!node1.getClass().equals(node2.getClass())) {
                ret.add(node2);
            } else if (node1 instanceof BooleanLiteral) {
                BooleanLiteral booleanLiteral1 = (BooleanLiteral) node1;
                BooleanLiteral booleanLiteral2 = (BooleanLiteral) node2;
                if (booleanLiteral1.booleanValue() != booleanLiteral2.booleanValue()) {
                    ret.add(node2);
                }
            } else if (node1 instanceof CharacterLiteral) {
                CharacterLiteral characterLiteral1 = (CharacterLiteral) node1;
                CharacterLiteral characterLiteral2 = (CharacterLiteral) node2;
                if (characterLiteral1.charValue() != characterLiteral2.charValue()) {
                    ret.add(node2);
                }
            } else if (node1 instanceof NumberLiteral) {
                NumberLiteral numberLiteral1 = (NumberLiteral) node1;
                NumberLiteral numberLiteral2 = (NumberLiteral) node2;
                if (!numberLiteral1.getToken().equals(numberLiteral2.getToken())) {
                    ret.add(node2);
                }
            } else if (node1 instanceof StringLiteral) {
                StringLiteral stringLiteral1 = (StringLiteral) node1;
                StringLiteral stringLiteral2 = (StringLiteral) node2;
                if (!stringLiteral1.getLiteralValue().equals(stringLiteral2.getLiteralValue())) {
                    ret.add(node2);
                }
            } else if (node1 instanceof QualifiedName) {
                QualifiedName qualifiedName1 = (QualifiedName) node1;
                QualifiedName qualifiedName2 = (QualifiedName) node2;
                if (qualifiedName1.getQualifier().toString().equals(qualifiedName2.getQualifier().toString())
                        && !qualifiedName1.getName().toString().equals(qualifiedName2.getName().toString())) {
                    ret.add(qualifiedName2);
                }
            }
        }
        return ret;
    }

    public static List<MethodInvocation> getAllAssertions(ASTNode node) {
        AllAssertionVisitor visitor = new AllAssertionVisitor();
        node.accept(visitor);
        return visitor.getAssertions();
    }

    public static Expression getExpectedInAssertion(MethodInvocation assertion) {
        if (assertion.arguments().size() == 2) {
            return (Expression) assertion.arguments().get(0);
        } else if (assertion.arguments().size() == 3) {
            return (Expression) assertion.arguments().get(1);
        } else {
            return null;
        }
    }

    public static Expression getActualInAssertion(MethodInvocation assertion) {
        if (assertion.arguments().size() == 2) {
            return (Expression) assertion.arguments().get(1);
        } else if (assertion.arguments().size() == 3) {
            return (Expression) assertion.arguments().get(2);
        } else {
            return null;
        }
    }


    public static boolean allStringLiteral(List<ASTNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        for (ASTNode node : nodes) {
            if (!(node instanceof StringLiteral)) {
                if (!(node instanceof Expression)) {
                    return false;
                }
                Expression expression = (Expression) node;
                if (expression.resolveTypeBinding() == null) {
                    return false;
                }
                if (expression.resolveTypeBinding().getName().equals("String")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    public static boolean allNumberLiteral(List<ASTNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        for (ASTNode node : nodes) {
            if (!(node instanceof NumberLiteral)) {
                if (!(node instanceof Expression)) {
                    return false;
                }
                Expression expression = (Expression) node;
                if (expression.resolveTypeBinding() == null) {
                    return false;
                }
                String type = expression.resolveTypeBinding().getName();
                if (type.equals("int") || type.equals("double")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    public static boolean allCharacterLiteral(List<ASTNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        for (ASTNode node : nodes) {
            if (!(node instanceof CharacterLiteral)) {
                if (!(node instanceof Expression)) {
                    return false;
                }
                Expression expression = (Expression) node;
                if (expression.resolveTypeBinding() == null) {
                    return false;
                }
                if (expression.resolveTypeBinding().getName().equals("char")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    public static boolean allBooleanLiteral(List<ASTNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        for (ASTNode node : nodes) {
            if (!(node instanceof BooleanLiteral)) {
                if (!(node instanceof Expression)) {
                    return false;
                }
                Expression expression = (Expression) node;
                if (expression.resolveTypeBinding() == null) {
                    return false;
                }
                if (expression.resolveTypeBinding().getName().equals("boolean")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    public static boolean isAssetionMethod(MethodInvocation method) {
        if (method == null) {
            return false;
        }
        if (!method.getName().toString().startsWith("assert")) {
            return false;
        }
        IMethodBinding methodBinding = method.resolveMethodBinding();
        if (methodBinding == null) {
            return false;
        }
        return method.resolveTypeBinding().getName().equals("Assert");
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
