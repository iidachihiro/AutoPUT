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

    public static boolean isTestMethod(MethodDeclaration method) {
        if (ASTUtils.hasTestAnnotation(method.modifiers())) {
            return true;
        }
        if (ASTUtils.hasPublicModifier(method.modifiers())
                && ASTUtils.isVoidReturn(method)
                && (method.getName().getIdentifier().startsWith("test") || method.getName().getIdentifier().endsWith("test"))
                && method.parameters().size() == 0) {
            return true;
        }
        return false;
    }

    public static boolean isVoidReturn(MethodDeclaration method) {
        if (method.getReturnType2() == null) {
            return false;
        }
        if (!method.getReturnType2().isPrimitiveType()) {
            return false;
        }
        PrimitiveType primitiveType = (PrimitiveType) method.getReturnType2();
        return primitiveType.getPrimitiveTypeCode().equals(PrimitiveType.VOID);
    }

    public static boolean canExtract(Expression expression) {
        if (expression instanceof BooleanLiteral) return true;
        if (expression instanceof CharacterLiteral) return true;
        if (expression instanceof NumberLiteral) return true;
        if (expression instanceof StringLiteral) return true;

        ITypeBinding iTypeBinding = expression.resolveTypeBinding();
        if (iTypeBinding == null) return false;

        String type = iTypeBinding.getName();
        if (type.equals("boolean") || type.equals("char") || type.equals("byte")
                || type.equals("short") || type.equals("int") || type.equals("long")
                || type.equals("float") || type.equals("double") || type.equals("String")) {
            return true;
        }
        return false;
    }

    public static SingleMemberAnnotation getRunWithAnnotation(AST ast) {
        return createSingleMemberAnnotation(ast, "RunWith", "Theories");
    }
    public static MarkerAnnotation getParametersAnnotation(AST ast) {
        return createMarkerAnnotation(ast, "Parameters");
    }
    public static MarkerAnnotation getDataPointsAnnotation(AST ast) {
        return createMarkerAnnotation(ast, "DataPoints");
    }
    public static MarkerAnnotation getThoryAnnotation(AST ast) {
        return createMarkerAnnotation(ast, "Theory");
    }

    public static List<MethodInvocation> getAssertionMethods(ASTNode node) {
        AllAssertionVisitor visitor = new AllAssertionVisitor();
        node.accept(visitor);
        return visitor.getAssertions();
    }

    public static List<Annotation> getAnnotations(ASTNode node) {
        AnnotationVisitor visitor = new AnnotationVisitor();
        node.accept(visitor);
        return visitor.getAnnotations();
    }

    public static List<Name> getAllNames(ASTNode node) {
        AllNameVisitor visitor = new AllNameVisitor();
        node.accept(visitor);
        return visitor.getNames();
    }
    public static List<ASTNode> getAllNodes(ASTNode node) {
        AllElementsFindVisitor visitor = new AllElementsFindVisitor();
        node.accept(visitor);
        return visitor.getNodes();
    }
    public static List<MethodDeclaration> getAllMethods(ASTNode node) {
        AllMethodFindVisitor visitor = new AllMethodFindVisitor();
        node.accept(visitor);
        return visitor.getFoundMethods();
    }
    public static List<ASTNode> getDifferentNodes(ASTNode src, ASTNode dst) {
        List<ASTNode> ret = new ArrayList<>();
        List<ASTNode> nodes1 = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(src));
        List<ASTNode> nodes2 = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(dst));
        if (nodes1.size() != nodes2.size()) {
            return ret;
        }
        for (int i = 0; i < nodes1.size(); i++) {
            ASTNode node1 = nodes1.get(i);
            ASTNode node2 = nodes2.get(i);
            if (node1 instanceof BooleanLiteral) {
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
            } else if (node1 instanceof SimpleName) {
                SimpleName simpleName1 = (SimpleName) node1;
                SimpleName simpleName2 = (SimpleName) node2;
                if (canExtract(simpleName1) && canExtract(simpleName2)
                        && simpleName1.resolveTypeBinding().getName().equals(simpleName2.resolveBinding().getName())
                        && ASTUtils.isConst(simpleName1) && ASTUtils.isConst(simpleName1)) {
                    ret.add(node2);
                }
            }
        }
        return ret;
    }

    public static boolean isConst(Name name) {
        if (name.resolveBinding() == null || !(name.resolveBinding() instanceof IVariableBinding)) {
            return false;
        }
        IVariableBinding iVariableBinding = (IVariableBinding) name.resolveBinding();
        return iVariableBinding.getConstantValue() != null;
    }


    public static List<MethodInvocation> getAllAssertions(ASTNode node) {
        AllAssertionVisitor visitor = new AllAssertionVisitor();
        node.accept(visitor);
        return visitor.getAssertions();
    }

    public static boolean allStringType(List<ASTNode> nodes) {
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


    public static boolean allByteType(List<ASTNode> nodes) {
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
                if (type.equals("byte")) {
                    continue;
                }
                return false;
            } else {
                String type = ((NumberLiteral) node).resolveTypeBinding().getName();
                if (type.equals("byte")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }
    public static boolean allShortType(List<ASTNode> nodes) {
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
                if (type.equals("byte") || type.equals("short")) {
                    continue;
                }
                return false;
            } else {
                String type = ((NumberLiteral) node).resolveTypeBinding().getName();
                if (type.equals("byte") || type.equals("short")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }
    public static boolean allIntType(List<ASTNode> nodes) {
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
                if (type.equals("byte") || type.equals("short")|| type.equals("int")) {
                    continue;
                }
                return false;
            } else {
                String type = ((NumberLiteral) node).resolveTypeBinding().getName();
                if (type.equals("byte") || type.equals("short") || type.equals("int")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }
    public static boolean allLongType(List<ASTNode> nodes) {
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
                if (type.equals("byte") || type.equals("short") || type.equals("int")|| type.equals("long")) {
                    continue;
                }
                return false;
            } else {
                String type = ((NumberLiteral) node).resolveTypeBinding().getName();
                if (type.equals("byte") || type.equals("short") || type.equals("int")|| type.equals("long")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }
    public static boolean allFloatType(List<ASTNode> nodes) {
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
                if (type.equals("byte") || type.equals("short") || type.equals("int")
                        || type.equals("long") || type.equals("float")) {
                    continue;
                }
                return false;
            } else {
                String type = ((NumberLiteral) node).resolveTypeBinding().getName();
                if (type.equals("byte") || type.equals("short") || type.equals("int")
                        || type.equals("long")|| type.equals("float")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }
    public static boolean allDoubleType(List<ASTNode> nodes) {
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
                if (type.equals("byte") || type.equals("short") || type.equals("int")
                        || type.equals("long")|| type.equals("float") || type.equals("double")) {
                    continue;
                }
                return false;
            } else {
                String type = ((NumberLiteral) node).resolveTypeBinding().getName();
                if (type.equals("byte") || type.equals("short") || type.equals("int")
                        || type.equals("long")|| type.equals("float") || type.equals("double")) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }


    public static boolean allCharacterType(List<ASTNode> nodes) {
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

    public static boolean allBooleanType(List<ASTNode> nodes) {
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

    public static boolean isAssertionMethod(MethodInvocation method) {
        if (method == null) {
            return false;
        }
        boolean ret;
        if (method.resolveTypeBinding() == null) {
            ret = method.getName().toString().startsWith("assert");
        } else {
            ret = method.resolveTypeBinding().getName().equals("Assert");
        }
        return ret;
    }

    public static boolean isLiteralNode(ASTNode node) {
        return (node instanceof BooleanLiteral) || (node instanceof CharacterLiteral)
                || (node instanceof NumberLiteral) || (node instanceof StringLiteral) || (node instanceof NullLiteral);
    }

    public static boolean hasStaticModifier(List<IExtendedModifier> modifiers) {
        for (IExtendedModifier iExtendedModifier : modifiers) {
            if (iExtendedModifier.isModifier()) {
                Modifier modifier = (Modifier) iExtendedModifier;
                if (modifier.isStatic()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasFinalModifier(List<IExtendedModifier> modifiers) {
        for (IExtendedModifier iExtendedModifier : modifiers) {
            if (iExtendedModifier.isModifier()) {
                Modifier modifier = (Modifier) iExtendedModifier;
                if (modifier.isFinal()) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean hasPrivateModifier(List<IExtendedModifier> modifiers) {
        for (IExtendedModifier iExtendedModifier : modifiers) {
            if (iExtendedModifier.isModifier()) {
                Modifier modifier = (Modifier) iExtendedModifier;
                if (modifier.isPrivate()) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean hasPublicModifier(List<IExtendedModifier> modifiers) {
        for (IExtendedModifier iExtendedModifier : modifiers) {
            if (iExtendedModifier.isModifier()) {
                Modifier modifier = (Modifier) iExtendedModifier;
                if (modifier.isPublic()) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean hasTestAnnotation(List<IExtendedModifier> modifiers) {
        for (IExtendedModifier iExtendedModifier : modifiers) {
            if (iExtendedModifier.isAnnotation()) {
                Annotation annotation = (Annotation) iExtendedModifier;
                if (annotation.getTypeName().toString().equals("Test")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean expectException(MethodDeclaration method) {
        for (IExtendedModifier iExtendedModifier : (List<IExtendedModifier>) method.modifiers()) {
            if (iExtendedModifier.isAnnotation()) {
                Annotation annotation = (Annotation) iExtendedModifier;
                if (!(annotation instanceof SingleMemberAnnotation)) {
                    continue;
                }
                SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) annotation;
                if (!(singleMemberAnnotation.getValue() instanceof Assignment)) {
                    continue;
                }
                Assignment assignment = (Assignment) singleMemberAnnotation.getValue();
                if (!assignment.getOperator().equals(Assignment.Operator.ASSIGN)) {
                    continue;
                }
                if (!(assignment.getLeftHandSide() instanceof SimpleName)) {
                    continue;
                }
                if (!(assignment.getRightHandSide() instanceof TypeLiteral)) {
                    continue;
                }
                TypeLiteral typeLiteral = (TypeLiteral) assignment.getRightHandSide();
                if (typeLiteral.getType().toString().contains("Exception")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<ASTNode> flattenMinusNumberLiteral(List<ASTNode> nodes) {
        List<ASTNode> ret = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            ASTNode node = nodes.get(i);
            if (node instanceof PrefixExpression) {
                PrefixExpression prefixExpression = (PrefixExpression) node;
                if ((prefixExpression.getOperator().equals(PrefixExpression.Operator.MINUS))
                        && (prefixExpression.getOperand() instanceof NumberLiteral)) {
                    NumberLiteral numberLiteral = (NumberLiteral) prefixExpression.getOperand();
                    if (!numberLiteral.getToken().contains("-")) {
                        numberLiteral.setToken("-" + numberLiteral.getToken());
                    }
                    ret.add(numberLiteral);
                    i++;
                    continue;
                }
            }
            ret.add(node);
        }
        return ret;
    }

    public static boolean isNumberLiteralWithPrefixedMinus(ASTNode node) {
        if (!(node instanceof NumberLiteral)) {
            return false;
        }
        if (!(node.getParent() instanceof PrefixExpression)) {
            return false;
        }
        NumberLiteral numberLiteral = (NumberLiteral) node;
        PrefixExpression parent = (PrefixExpression) node.getParent();
        return parent.getOperator().equals(PrefixExpression.Operator.MINUS);
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

    private static boolean _sameType(ITypeBinding binding1, ITypeBinding binding2) {
        if (binding1 == null || binding2 == null) {
            return false;
        }
        return binding1.getName().equals(binding2.getName());
    }

    private Name _getRootName(Name name) {
        if (!(name.getParent() instanceof Name)) {
            return name;
        }
        return _getRootName((Name) name.getParent());
    }
}
