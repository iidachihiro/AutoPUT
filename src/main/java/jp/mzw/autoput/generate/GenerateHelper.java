package jp.mzw.autoput.generate;

import jp.mzw.autoput.ast.ASTUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.List;

class GenerateHelper {

    protected static List<ASTNode> getParameterNodes(ASTNode src, ASTNode dst) {
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
                if (ASTUtils.canExtract(simpleName1) && ASTUtils.canExtract(simpleName2)
                        && simpleName1.resolveTypeBinding().getName().equals(simpleName2.resolveBinding().getName())
                        && ASTUtils.isConst(simpleName1) && ASTUtils.isConst(simpleName1)) {
                    ret.add(node2);
                }
            }
        }
        return ret;
    }

    protected static boolean isInExpectedDeclaringNode(MethodDeclaration origin, ASTNode node) {
        List<MethodInvocation> assertions = ASTUtils.getAllAssertions(origin);
        for (MethodInvocation assertion : assertions) {
            Expression expected;
            ASTNode expectedDeclaringNode = null;
            if (assertion.arguments().size() == 2) {
                expected = (Expression) assertion.arguments().get(0);
                if (ASTUtils.isLiteralNode(expected) && expected.equals(node)) {
                    return true;
                } else if (expected instanceof Name) {
                    expectedDeclaringNode = ((CompilationUnit) origin.getRoot()).findDeclaringNode(((Name) expected).resolveBinding());
                }
            } else if (assertion.arguments().size() == 3) {
                expected = (Expression) assertion.arguments().get(1);
                if (ASTUtils.isLiteralNode(expected) && expected.equals(node)) {
                    return true;
                } else if (expected instanceof Name) {
                    expectedDeclaringNode = ((CompilationUnit) origin.getRoot()).findDeclaringNode(((Name) expected).resolveBinding());
                }
            }
            while (expectedDeclaringNode != null && node != null) {
                if (node.equals(expectedDeclaringNode)) {
                    return true;
                }
                node = node.getParent();
            }
        }
        return false;
    }

    protected static Name getRootName(Name name) {
        if (!(name.getParent() instanceof Name)) {
            return name;
        } else {
            return getRootName(name);
        }
    }

    protected static boolean isObjectArrayType(Type type) {
        if (!(type instanceof ArrayType)) {
            return false;
        }
        ArrayType arrayType = (ArrayType) type;
        if (!(arrayType.getElementType() instanceof SimpleType)) {
            return false;
        }
        SimpleType simpleType = (SimpleType) arrayType.getElementType();
        return simpleType.getName().toString().equals("Object");
    }

    protected static boolean isDoubleArrayType(Type type) {
        if (!(type instanceof ArrayType)) {
            return false;
        }
        ArrayType arrayType = (ArrayType) type;
        if (!(arrayType.getElementType() instanceof PrimitiveType)) {
            return false;
        }
        PrimitiveType primitiveType = (PrimitiveType) arrayType.getElementType();
        return primitiveType.getPrimitiveTypeCode().equals(PrimitiveType.DOUBLE);
    }

    protected static boolean isBooleanType(ASTNode node) {
        return _isThisType(node, "boolean");
    }
    protected static boolean isCharacterType(ASTNode node) {
        return _isThisType(node, "char");
    }
    protected static boolean isByteType(ASTNode node) {
        return _isThisType(node, "byte");
    }
    protected static boolean isShortType(ASTNode node) {
        return _isThisType(node, "short");
    }
    protected static boolean isIntType(ASTNode node) {
        return _isThisType(node, "int");
    }
    protected static boolean isLongType(ASTNode node) {
        return _isThisType(node, "long");
    }
    protected static boolean isFloatType(ASTNode node) {
        return _isThisType(node, "float");
    }
    protected static boolean isDoubleType(ASTNode node) {
        return _isThisType(node, "double");
    }
    protected static boolean _isThisType(ASTNode node, String type) {
        if (!(node instanceof Expression)) {
            return false;
        }
        Expression expression = (Expression) node;
        ITypeBinding iTypeBinding = expression.resolveTypeBinding();
        if (iTypeBinding == null) {
            return false;
        }
        return iTypeBinding.isPrimitive() && iTypeBinding.toString().equals(type);
    }

    protected static boolean canBeCasted(ASTNode node) {
        if (!(node instanceof Expression)) {
            return false;
        }
        Expression expression = (Expression) node;
        ITypeBinding iTypeBinding = expression.resolveTypeBinding();

        if (iTypeBinding == null) {
            return false;
        }
        return !iTypeBinding.isPrimitive();
    }

    protected static boolean shouldCastDoubleToLong(ASTNode target) {
        if (target.getParent() instanceof MethodInvocation) {
            MethodInvocation methodInvocation = (MethodInvocation) target.getParent();
            List<Expression> args = (List<Expression>) methodInvocation.arguments();
            int targetIndex = -1;
            for (int i = 0; i < args.size(); i++) {
                Expression arg = args.get(i);
                if (arg.equals(target)) {
                    targetIndex = i;
                }
            }
            if (targetIndex < 0) {
                return false;
            }
            IMethodBinding iMethodBinding = methodInvocation.resolveMethodBinding();
            if (iMethodBinding == null) {
                return false;
            }
            ITypeBinding[] iTypeBindings = iMethodBinding.getParameterTypes();
            if (iTypeBindings == null) {
                return false;
            }
            if (iTypeBindings.length - 1 < targetIndex) {
                return false;
            }
            if (iTypeBindings[targetIndex].getName().equals("long")) {
                return true;
            }
        } else if (target.getParent() instanceof ClassInstanceCreation) {
            ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) target.getParent();
            List<Expression> args = (List<Expression>) classInstanceCreation.arguments();
            int targetIndex = -1;
            for (int i = 0; i < args.size(); i++) {
                Expression arg = args.get(i);
                if (arg.equals(target)) {
                    targetIndex = i;
                }
            }
            if (targetIndex < 0) {
                return false;
            }
            IMethodBinding iMethodBinding = classInstanceCreation.resolveConstructorBinding();
            ITypeBinding[] iTypeBindings = iMethodBinding.getParameterTypes();
            if (iTypeBindings.length - 1 < targetIndex) {
                return false;
            }
            if (iTypeBindings[targetIndex].getName().equals("long")) {
                return true;
            }
        }
        return false;
    }

    protected static boolean shouldCastDoubleToInt(ASTNode target) {
        if (target.getParent() instanceof MethodInvocation) {
            MethodInvocation methodInvocation = (MethodInvocation) target.getParent();
            List<Expression> args = (List<Expression>) methodInvocation.arguments();
            int targetIndex = -1;
            for (int i = 0; i < args.size(); i++) {
                Expression arg = args.get(i);
                if (arg.equals(target)) {
                    targetIndex = i;
                }
            }
            IMethodBinding iMethodBinding = methodInvocation.resolveMethodBinding();
            if (iMethodBinding == null) {
                return false;
            }
            ITypeBinding[] iTypeBindings = iMethodBinding.getParameterTypes();
            if (iTypeBindings == null) {
                return false;
            }
            if (iTypeBindings.length - 1 < targetIndex) {
                return false;
            }
            if (iTypeBindings[targetIndex].getName().equals("int")) {
                return true;
            }
        }
        return false;
    }
}
