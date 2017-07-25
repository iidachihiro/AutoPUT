package jp.mzw.tri.modifier;

import jp.mzw.tri.ast.AllElementsFindVisitor;
import jp.mzw.tri.core.TestCase;
import jp.mzw.tri.core.TestSuite;
import jp.mzw.tri.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/24/17.
 */
public class UseParametersAnnotation {
    protected static Logger LOGGER = LoggerFactory.getLogger(UseParametersAnnotation.class);

    protected TestSuite testSuite;

    public UseParametersAnnotation(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public void useParametersAnnotation() {
        for (TestCase testCase : testSuite.getTestCases()) {
            if (!testCase.getName().equals("testLargeArguments")) {
                continue;
            }
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (detect(method)) {
                modify(method);
            }
        }
    }

    private void modify(MethodDeclaration method, ASTNode target) {
        final CompilationUnit cu = testSuite.getCu();
        final AST ast = cu.getAST();
        final ASTRewrite rewrite = ASTRewrite.create(ast);


        SingleMemberAnnotation annotation = getRunWithAnnotation(ast);
        TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
        typeDeclaration.modifiers().add(annotation);
        typeDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        typeDeclaration.setName(ast.newSimpleName(StringUtils.capitalize(method.getName().toString())));


        rewrite.replace(testSuite.getClassDeclaration(), typeDeclaration, null);
        System.out.println("CompilationUnit " + cu);
    }

    private boolean detect(MethodDeclaration methodDeclaration) {
        List<MethodInvocation> assertions = getAssertionMethods(methodDeclaration);
        if (assertions.isEmpty()) {
            return false;
        }
        return detect(assertions);
    }

    private boolean detect(List<MethodInvocation> assertions) {
        for (int i = 0; i < assertions.size(); i++) {
            for (int j = assertions.size() - 1; i < j; j--) {
                if (Utils.compareAssetionMethod(assertions.get(i), assertions.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }


    private ASTNode canUseParametersAnnotation2(MethodDeclaration method) {
        AllElementsFindVisitor visitor = new AllElementsFindVisitor();
        method.accept(visitor);
        List<ASTNode> nodes = visitor.getNodes();
        List<Name> arrayVarNames = new ArrayList<>();
        for (ASTNode node : nodes) {
            if (isArrayVarInit(node)) {
                arrayVarNames.add(((VariableDeclarationFragment)node).getName());
            }
        }
        for (ASTNode node : nodes) {
            if (!(node instanceof SimpleName)) {
                continue;
            }
            SimpleName name = (SimpleName) node;
            if (isArrayVar(name, arrayVarNames) && usedInAssertion(name) && usedInForStatement(name)) {
                return targetForStatement(name);
            }
        }
        return null;
    }

    private boolean isArrayVar(Name name, List<Name> arrayVarNames) {
        if (arrayVarNames == null || arrayVarNames.isEmpty()) {
            return false;
        }
        for (Name arrayVar : arrayVarNames) {
            if (arrayVar.toString().equals(name.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean usedInForStatement(Name name) {
        ASTNode node = name;
        while (node.getParent() != null) {
            node = node.getParent();
            if (node instanceof ForStatement || node instanceof EnhancedForStatement) {
                return true;
            }
        }
        return false;
    }

    private ASTNode targetForStatement(Name name) {
        ASTNode node = name;
        while (node.getParent() != null) {
            node = node.getParent();
            if (node instanceof ForStatement || node instanceof EnhancedForStatement) {
                return node;
            }
        }
        return null;
    }

    private boolean usedInAssertion(Name name) {
        ASTNode node = name;
        while (node.getParent() != null) {
            node = node.getParent();
            if (isAssertMethod(node)) {
                return true;
            }
        }
        return false;
    }

    private boolean isArrayVarInit(ASTNode node) {
        if (!(node instanceof VariableDeclarationFragment)) {
            return false;
        }
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) node;
        return fragment.getInitializer() instanceof ArrayInitializer;
    }
    private List<MethodInvocation> getAssertionMethods(MethodDeclaration methodDeclaration) {
        AllElementsFindVisitor visitor = new AllElementsFindVisitor();
        methodDeclaration.accept(visitor);
        List<ASTNode> nodes = visitor.getNodes();
        List<MethodInvocation> assertions = new ArrayList<>();
        for (ASTNode node : nodes) {
            if (isAssertMethod(node)) {
                assertions.add((MethodInvocation) node);
            }
        }
        return assertions;
    }
    private boolean isAssertMethod(ASTNode node) {
        if (!(node instanceof MethodInvocation)) {
            return false;
        }
        MethodInvocation method = (MethodInvocation) node;
        return method.getName().toString().startsWith("assert");
    }

    private SingleMemberAnnotation getRunWithAnnotation(AST ast) {
        SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
        annotation.setTypeName(ast.newName("RunWith"));
        TypeLiteral typeLiteral = ast.newTypeLiteral();
        typeLiteral.setType(ast.newSimpleType(ast.newName("Parameterized")));
        annotation.setValue(typeLiteral);
        return annotation;
    }

    private MethodDeclaration getDataMethod(AST ast) {
        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
        annotation.setTypeName(ast.newName("Parameters"));
        methodDeclaration.modifiers().add(annotation);
        methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));

        return methodDeclaration;
    }
}
