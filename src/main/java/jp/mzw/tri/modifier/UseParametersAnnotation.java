package jp.mzw.tri.modifier;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.heuristic.LcsMatcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import jp.mzw.tri.ast.JdtVisitor;
import jp.mzw.tri.core.TestCase;
import jp.mzw.tri.core.TestSuite;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 7/24/17.
 */
public class UseParametersAnnotation {
    protected static Logger LOGGER = LoggerFactory.getLogger(UseParametersAnnotation.class);

    private final static String RUN_WITH = "RunWith";
    private final static String PARAMETERS = "Parameters";
    private final static String INPUT_NAME = "input";
    private final static String EXPECTED_NAME = "expected";

    protected TestSuite testSuite;
    private MethodDeclaration key;
    private List<MethodDeclaration> targets;

    public UseParametersAnnotation(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public void useParametersAnnotation() {
        int size = testSuite.getTestCases().size();
        Map<MethodDeclaration, List<MethodDeclaration>> targetMap = new HashMap<>();
        boolean[] registered = new boolean[size];
        for (int i = 0; i < size; i++) {
            if (registered[i]) {
                continue;
            }
            for (int j = size - 1; i < j; j--) {
                if (registered[j]) {
                    continue;
                }
                TestCase src = testSuite.getTestCases().get(i);
                TestCase dst = testSuite.getTestCases().get(j);
                List<Action> actions = getActions(src.getMethodDeclaration(), dst.getMethodDeclaration(), true);
                if (actions.isEmpty()) {
                    MethodDeclaration key = src.getMethodDeclaration();
                    List<MethodDeclaration> targetList = targetMap.get(key);
                    if (targetList == null) {
                        targetList = new ArrayList<>();
                        targetList.add(src.getMethodDeclaration());
                        registered[i] = true;
                        targetMap.put(key, targetList);
                    }
                    targetList.add(dst.getMethodDeclaration());
                    registered[j] = true;
                }
            }
        }

        for (MethodDeclaration key : targetMap.keySet()) {
            List<MethodDeclaration> targets = targetMap.get(key);
//            System.out.println("====================");
//            for (MethodDeclaration method : targets) {
//                System.out.println(method);
//            }
//            System.out.println("====================");
            this.key = key;
            this.targets = targets;
            createPUT(key, targets);
        }
    }

    private void createPUT(MethodDeclaration key, List<MethodDeclaration> similarMethods) {
        // prepare
        final AST ast = key.getRoot().getAST();
        final ASTRewrite rewrite = ASTRewrite.create(ast);
        CompilationUnit cu = (CompilationUnit) ASTNode.copySubtree(ast, key.getRoot());
        TypeDeclaration replace = createNewTypeDeclaration(ast);
        replace.bodyDeclarations().addAll(createFieldDeclaration(ast));
        replace.bodyDeclarations().addAll(createMethodDeclaration(ast));
    }

    private TypeDeclaration createNewTypeDeclaration(AST ast) {
        TypeDeclaration ret = ast.newTypeDeclaration();
        Annotation annotation = createRunWithAnnotation(ast);
        ret.modifiers().add(annotation);
        ret.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        return ret;
    }

    private Annotation createRunWithAnnotation(AST ast) {
        SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
        annotation.setTypeName(ast.newName(RUN_WITH));
        TypeLiteral type = ast.newTypeLiteral();
        type.setType(ast.newSimpleType(ast.newSimpleName(key.getName().toString())));
        annotation.setValue(type);
        return annotation;
    }

    private List<FieldDeclaration> createFieldDeclaration(AST ast) {
        List<FieldDeclaration> fields = new ArrayList<>();
        // create input
        VariableDeclarationFragment inputFragment = ast.newVariableDeclarationFragment();
        inputFragment.setName(ast.newSimpleName(INPUT_NAME));
        FieldDeclaration input = ast.newFieldDeclaration(inputFragment);
        // TODO 変数の型を設定
        // input.setType();
        input.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
        fields.add(input);

        // create expected results
        VariableDeclarationFragment expectedFragment = ast.newVariableDeclarationFragment();
        expectedFragment.setName(ast.newSimpleName(EXPECTED_NAME));
        FieldDeclaration expected = ast.newFieldDeclaration(expectedFragment);
        // TODO 変数の型を設定
        // input.setType();
        input.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
        fields.add(expected);

        return fields;
    }

    private List<MethodDeclaration> createMethodDeclaration(AST ast) {
        List<MethodDeclaration> methods = new ArrayList<>();
        MethodDeclaration constructor = createConstructor(ast);
        MethodDeclaration data = createDataMethod(ast);
        MethodDeclaration test = createTestMethod(ast);

        methods.add(constructor);
        methods.add(data);
        methods.add(test);
        return methods;
    }

    private MethodDeclaration createTestMethod(AST ast) {
        MethodDeclaration testMethod = ast.newMethodDeclaration();

        return testMethod;
    }

    private MethodDeclaration createDataMethod(AST ast) {
        MethodDeclaration method = ast.newMethodDeclaration();
        method.setConstructor(false);
        method.setName(ast.newSimpleName("data"));
        SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
        annotation.setTypeName(ast.newName(PARAMETERS));
        method.modifiers().add(annotation);
        method.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        method.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
        method.setReturnType2(createDataReturnType(ast));
        method.setBody(createDataContent(ast));
        return method;
    }

    private Block createDataContent(AST ast) {
        Block block = ast.newBlock();
        ReturnStatement returnStatement = ast.newReturnStatement();
        MethodInvocation content = ast.newMethodInvocation();
        content.setExpression(ast.newSimpleName("Arrays"));
        content.setName(ast.newSimpleName("asList"));
        // asListの引数を作る
        ArrayType arrayType = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        // two dimensions
        arrayType.dimensions().add(ast.newDimension());
        arrayType.dimensions().add(ast.newDimension());
        ArrayCreation arrayCreation = ast.newArrayCreation();
        arrayCreation.setType(arrayType);
        ArrayInitializer arrayInitializer = ast.newArrayInitializer();
        // TODO arrayInitializerを作る
        arrayCreation.setInitializer(arrayInitializer);
        content.setExpression(arrayCreation);
        returnStatement.setExpression(content);
        block.statements().add(returnStatement);
        return block;
    }

    private Type createDataReturnType(AST ast) {
        ArrayType objectArray = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        objectArray.dimensions().add(ast.newDimension());
        Type ret = ast.newParameterizedType(ast.newSimpleType(ast.newName("Collection")));
        return ret;
    }

    private MethodDeclaration createConstructor(AST ast) {
        MethodDeclaration constructor = ast.newMethodDeclaration();
        constructor.setConstructor(true);
        constructor.setName(ast.newSimpleName(key.getName().toString()));
        constructor.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        // TODO 引数の設定
        // constructor.setReceiverType();
        constructor.setBody(createConstructorBlock(ast));
        return constructor;
    }
    private Block createConstructorBlock(AST ast) {
        Block block = ast.newBlock();
        // create "this.input = input;"
        Assignment inputAssignment = ast.newAssignment();
        inputAssignment.setOperator(Assignment.Operator.ASSIGN);
        FieldAccess leftOperand = ast.newFieldAccess();
        leftOperand.setExpression(ast.newThisExpression());
        leftOperand.setName(ast.newSimpleName(INPUT_NAME));
        inputAssignment.setLeftHandSide(leftOperand);
        inputAssignment.setRightHandSide(ast.newSimpleName(INPUT_NAME));
        block.statements().add(inputAssignment);

        // create "this.expected = expected;"
        Assignment expectedAssignment = ast.newAssignment();
        expectedAssignment.setOperator(Assignment.Operator.ASSIGN);
        leftOperand = ast.newFieldAccess();
        leftOperand.setExpression(ast.newThisExpression());
        leftOperand.setName(ast.newSimpleName(EXPECTED_NAME));
        expectedAssignment.setLeftHandSide(leftOperand);
        expectedAssignment.setRightHandSide(ast.newSimpleName(EXPECTED_NAME));
        block.statements().add(expectedAssignment);

        return block;
    }

    private List<Action> getActions(ASTNode src, ASTNode dst, boolean lessLabel) {
        ITree srcTree = getITree(src,lessLabel);
        ITree dstTree = getITree(dst,lessLabel);
        LcsMatcher m = new LcsMatcher(srcTree, dstTree, new MappingStore());
        m.match();
        ActionGenerator ag = new ActionGenerator(srcTree, dstTree, m.getMappings());
        ag.generate();
        return ag.getActions();
    }

    private ITree getITree(ASTNode method, boolean lessLabel) {
        JdtVisitor visitor = new JdtVisitor(lessLabel);
        method.accept(visitor);
        ITree tree = visitor.getTreeContext().getRoot();
        tree.refresh();
        TreeUtils.postOrderNumbering(tree);
        return tree;
    }
}
