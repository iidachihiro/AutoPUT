package jp.mzw.tri.modifier;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.heuristic.LcsMatcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import jp.mzw.tri.ast.*;
import jp.mzw.tri.core.TestCase;
import jp.mzw.tri.core.TestSuite;
import jp.mzw.tri.util.Utils;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    private List<Expression> inputs = new ArrayList<>();
    private List<Expression> expecteds = new ArrayList<>();
    private Map<String, Expression> targets = new HashMap<>();
    private Expression input;
    private Expression expected;
    private SimpleType inputType;
    private SimpleType expectedType;
    private Statement inputStatement;
    private Statement expectedStatement;

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
            MethodDeclaration dst = targets.get(1);
            if (!key.getName().toString().equals("testParseSimpleWithDecimals")) {
                continue;
            }
            if (!dst.getName().toString().equals("testParseSimpleWithDecimalsTrunc")) {
                continue;
            }
            detect(key, dst);
            this.key = key;
            createPUT(key);
        }
    }

    private void createPUT(MethodDeclaration key) {
        // prepare
        final AST ast = key.getRoot().getAST();
        TypeDeclaration replace = createNewTypeDeclaration(ast);
        replace.bodyDeclarations().addAll(createFieldDeclaration(ast));
        replace.bodyDeclarations().addAll(createMethodDeclaration(ast));
        System.out.println("PUT: " + replace);
    }

    private TypeDeclaration createNewTypeDeclaration(AST ast) {
        TypeDeclaration ret = ast.newTypeDeclaration();
        Annotation annotation = createRunWithAnnotation(ast);
        ret.modifiers().add(annotation);
        ret.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        ret.setName(ast.newSimpleName("AutoPUT"));
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
        input.setType(ast.newSimpleType(ast.newName(this.inputType.getName().toString())));
        input.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
        fields.add(input);

        // create expected results
        VariableDeclarationFragment expectedFragment = ast.newVariableDeclarationFragment();
        expectedFragment.setName(ast.newSimpleName(EXPECTED_NAME));
        FieldDeclaration expected = ast.newFieldDeclaration(expectedFragment);
        expected.setType(ast.newSimpleType(ast.newName(this.expectedType.getName().toString())));
        expected.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
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
        MarkerAnnotation annotation = ast.newMarkerAnnotation();
        annotation.setTypeName(ast.newName("Test"));
        testMethod.modifiers().add(annotation);
        testMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        testMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
        testMethod.setName(ast.newSimpleName("parameterizedTest"));
        // 中身をinput, expected関連のstatementのみ変更する
        Block originBody = this.key.getBody();
        Block copyBody = ast.newBlock();
        for (Object obj : originBody.statements()) {
            Statement statement = (Statement) obj;
            if (inputStatement.equals(statement) && expectedStatement.equals(statement)) {
                // Assertionの書き換え
                Expression expression = ((ExpressionStatement) statement).getExpression();
                MethodInvocation assertion = (MethodInvocation) expression;
                MethodInvocation replaceAssertion = (MethodInvocation) ASTNode.copySubtree(ast, assertion);
                if (assertion.arguments().size() == 2) {
                    replaceAssertion.arguments().clear();
                    replaceAssertion.arguments().add(ast.newSimpleName(EXPECTED_NAME));
                    replaceAssertion.arguments().add(ast.newSimpleName(INPUT_NAME));
                } else if (assertion.arguments().size() == 3) {
                    Expression firstArg = (Expression) replaceAssertion.arguments().get(0);
                    replaceAssertion.arguments().clear();
                    replaceAssertion.arguments().add(firstArg);
                    replaceAssertion.arguments().add(ast.newSimpleName(EXPECTED_NAME));
                    replaceAssertion.arguments().add(ast.newSimpleName(INPUT_NAME));
                }
                ExpressionStatement replace = ast.newExpressionStatement(replaceAssertion);
                copyBody.statements().add(replace);
            } else if (inputStatement.equals(statement)) {
                // inputの代入文の書き換え
                VariableDeclarationStatement vStatement = (VariableDeclarationStatement) statement;
                VariableDeclarationStatement replace = (VariableDeclarationStatement) ASTNode.copySubtree(ast, vStatement);
                replace.fragments().clear();
                for (Object object : vStatement.fragments()) {
                    VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
                    if (fragment.getInitializer().equals(this.input)) {
                        VariableDeclarationFragment replaceFragment =
                                (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);
                        replaceFragment.setInitializer(ast.newSimpleName(INPUT_NAME));
                        replace.fragments().add(replaceFragment);
                    } else {
                        VariableDeclarationFragment copy =
                                (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);
                        replace.fragments().add(copy);
                    }
                }
                copyBody.statements().add(replace);
            } else if (expectedStatement.equals(statement)) {
                // expectedの代入文の書き換え
                VariableDeclarationStatement vStatement = (VariableDeclarationStatement) statement;
                VariableDeclarationStatement replace = (VariableDeclarationStatement) ASTNode.copySubtree(ast, vStatement);
                replace.fragments().clear();
                for (Object object : vStatement.fragments()) {
                    VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
                    if (fragment.getInitializer().equals(this.expected)) {
                        VariableDeclarationFragment replaceFragment =
                                (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);
                        replaceFragment.setInitializer(ast.newSimpleName(EXPECTED_NAME));
                        replace.fragments().add(replaceFragment);
                    } else {
                        VariableDeclarationFragment copy =
                                (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);
                        replace.fragments().add(copy);
                    }
                }
                copyBody.statements().add(replace);
            } else {
                Statement copy = (Statement) ASTNode.copySubtree(ast, statement);
                copyBody.statements().add(copy);
            }
        }
        testMethod.setBody(copyBody);
        return testMethod;
    }

    private MethodDeclaration createDataMethod(AST ast) {
        MethodDeclaration method = ast.newMethodDeclaration();
        method.setConstructor(false);
        method.setName(ast.newSimpleName("data"));
        MarkerAnnotation annotation = ast.newMarkerAnnotation();
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
        ArrayCreation arrayCreation = ast.newArrayCreation();
        arrayCreation.setType(arrayType);
        ArrayInitializer arrayInitializer = ast.newArrayInitializer();
        for (int i = 0; i < this.inputs.size(); i++) {
            ArrayInitializer pair = ast.newArrayInitializer();
            Expression originInput = this.inputs.get(0);
            Expression replaceInput = (Expression) ASTNode.copySubtree(ast, originInput);
            Expression originExpected = this.expecteds.get(0);
            Expression replaceExpected = (Expression) ASTNode.copySubtree(ast, originExpected);
            pair.expressions().add(replaceInput);
            pair.expressions().add(replaceExpected);
            arrayInitializer.expressions().add(pair);
        }
        arrayCreation.setInitializer(arrayInitializer);
        content.arguments().add(arrayCreation);
        returnStatement.setExpression(content);
        block.statements().add(returnStatement);
        return block;
    }

    private Type createDataReturnType(AST ast) {
        ArrayType objectArray = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        ParameterizedType ret = ast.newParameterizedType(ast.newSimpleType(ast.newName("Collection")));
        ret.typeArguments().add(objectArray);
        return ret;
    }

    private MethodDeclaration createConstructor(AST ast) {
        MethodDeclaration constructor = ast.newMethodDeclaration();
        constructor.setConstructor(true);
        // 名前を設定
        constructor.setName(ast.newSimpleName("AutoPUT"));
        constructor.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        // 引数を設定
        SingleVariableDeclaration inputDecralation = ast.newSingleVariableDeclaration();
        inputDecralation.setType(ast.newSimpleType(ast.newName(this.inputType.toString())));
        inputDecralation.setName(ast.newSimpleName(INPUT_NAME));
        constructor.parameters().add(inputDecralation);
        SingleVariableDeclaration expectedDecralation = ast.newSingleVariableDeclaration();
        expectedDecralation.setType(ast.newSimpleType(ast.newName(this.expectedType.getName().toString())));
        expectedDecralation.setName(ast.newSimpleName(EXPECTED_NAME));
        constructor.parameters().add(expectedDecralation);
        // bodyを設定
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
        ExpressionStatement inputStatement = ast.newExpressionStatement(inputAssignment);
        block.statements().add(inputStatement);

        // create "this.expected = expected;"
        Assignment expectedAssignment = ast.newAssignment();
        expectedAssignment.setOperator(Assignment.Operator.ASSIGN);
        leftOperand = ast.newFieldAccess();
        leftOperand.setExpression(ast.newThisExpression());
        leftOperand.setName(ast.newSimpleName(EXPECTED_NAME));
        expectedAssignment.setLeftHandSide(leftOperand);
        expectedAssignment.setRightHandSide(ast.newSimpleName(EXPECTED_NAME));
        ExpressionStatement expectedStatement = ast.newExpressionStatement(expectedAssignment);
        block.statements().add(expectedStatement);

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

    private void detect(MethodDeclaration src, MethodDeclaration dst) {
        DetectVisitor visitor = new DetectVisitor();
        src.accept(visitor);
        List<Statement> statements = getTargetStatements(src, dst);
        getInputAndExpected(statements);
    }

    private List<Statement> getTargetStatements(MethodDeclaration src, MethodDeclaration dst) {
        JdtVisitor srcVisitor = new JdtVisitor();
        src.accept(srcVisitor);
        ITree srcTree = srcVisitor.getTreeContext().getRoot();
        srcTree.refresh();
        TreeUtils.preOrderNumbering(srcTree);

        JdtVisitor dstVisitor = new JdtVisitor();
        dst.accept(dstVisitor);
        ITree dstTree = dstVisitor.getTreeContext().getRoot();
        dstTree.refresh();
        TreeUtils.preOrderNumbering(dstTree);

        LcsMatcher m = new LcsMatcher(srcTree, dstTree, new MappingStore());
        m.match();
        ActionGenerator ag = new ActionGenerator(srcTree, dstTree, m.getMappings());
        ag.generate();

        List<Action> actions = ag.getActions();
        Map<ITree, ASTNode> srcAstNodeMap = srcVisitor.getAstNodeMap();
        List<Statement> targets = new ArrayList<>();
        for (Action action : actions) {
            ITree tree = action.getNode();
            for (ITree key : srcAstNodeMap.keySet()) {
                if (key.equals(tree)) {
                    Statement target = getTargetStatement(srcAstNodeMap.get(key));
                    if (target != null && !targets.contains(target)) {
                        targets.add(target);
                    }
                }
            }
        }

        List<Statement> statements = new ArrayList<>();
        Map<ITree, ASTNode> dstAstNodeMap = dstVisitor.getAstNodeMap();
        for (Action action : actions) {
            ITree tree = action.getNode();
            for (ITree key : srcAstNodeMap.keySet()) {
                if (key.equals(tree)) {
                    Statement target = getTargetStatement(srcAstNodeMap.get(key));
                    if (target != null && !statements.contains(target)) {
                        statements.add(target);
                    }
                }
            }
            for (ITree key : dstAstNodeMap.keySet()) {
                if (key.equals(tree)) {
                    Statement target = getTargetStatement(dstAstNodeMap.get(key));
                    if (target != null && !statements.contains(target)) {
                        statements.add(target);
                    }
                }
            }
        }
        createInputsAndExpecteds(statements);
        return targets;
    }
    private Statement getTargetStatement(ASTNode node) {
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

    private void createInputsAndExpecteds(List<Statement> statements) {
        System.out.println(statements);
        System.out.println(statements.size());
        if (statements.size() % 2 == 0) {
            for (int i = 0; i + 1 < statements.size(); i += 2) {
                Statement statement1 = statements.get(i);
                Statement statement2 = statements.get(i + 1);
                if (!(statement1 instanceof VariableDeclarationStatement
                        && statement1 instanceof VariableDeclarationStatement)) {
                    System.out.println("Limitation");
                    System.out.println("Statement1: " + statement1 + " " + statement1.getClass());
                    System.out.println("Statement2: " + statement2 + " " + statement2.getClass());
                } else {
                    VariableDeclarationStatement vStatement1 = (VariableDeclarationStatement) statement1;
                    VariableDeclarationStatement vStatement2 = (VariableDeclarationStatement) statement2;
                    if (isInputDeclarationStatement(vStatement1) || isExpectedDeclarationStatement(vStatement2)) {
                        Expression input = getInputExpression(vStatement1);
                        Expression expected = getExpectedExpression(vStatement2);
                        this.inputs.add(input);
                        this.expecteds.add(expected);
                    } else if (isExpectedDeclarationStatement(vStatement1) || isInputDeclarationStatement(vStatement2)) {
                        Expression input = getInputExpression(vStatement1);
                        Expression expected = getExpectedExpression(vStatement2);
                        this.inputs.add(input);
                        this.expecteds.add(expected);
                    } else {
                        System.out.println("NotFoundInputAndExpected!");
                        System.out.println("Statement1: " + vStatement1);
                        System.out.println("Statement2: " + vStatement2);
                    }
                }
            }
        }

    }

    private void getInputAndExpected(List<Statement> statements) {
        if (statements.size() == 1) {
            Statement statement = statements.get(0);
            if (!(statement instanceof ExpressionStatement)) {
                System.out.println("NotExpressionStatment! " + statement);
            }
            Expression expression = ((ExpressionStatement) statement).getExpression();
            if (!Utils.isAssertionMethod(expression)) {
                System.out.println("NotAssertionMethod!" + expression);
            }
            MethodInvocation assertion = (MethodInvocation) expression;
            if (assertion.arguments().size() == 2) {
                this.expected = (Expression) assertion.arguments().get(0);
                this.input = (Expression) assertion.arguments().get(1);
                this.targets.put(EXPECTED_NAME, this.expected);
                this.targets.put(INPUT_NAME, this.input);
                this.inputStatement = statement;
                this.expectedStatement = statement;
            } else if (assertion.arguments().size() == 3) {
                this.expected = (Expression) assertion.arguments().get(1);
                this.input = (Expression) assertion.arguments().get(2);
                this.targets.put(EXPECTED_NAME, this.expected);
                this.targets.put(INPUT_NAME, this.input);
            } else {
                System.out.println("Too many arguments for assertion! " + assertion);
            }
        } else if (statements.size() == 2) {
            Statement statement1 = statements.get(0);
            Statement statement2 = statements.get(1);
            if (!(statement1 instanceof VariableDeclarationStatement
                    && statement1 instanceof VariableDeclarationStatement)) {
                System.out.println("Limitation");
                System.out.println("Statement1: " + statement1 + " " + statement1.getClass());
                System.out.println("Statement2: " + statement2 + " " + statement2.getClass());
            } else {
                VariableDeclarationStatement vStatement1 = (VariableDeclarationStatement) statement1;
                VariableDeclarationStatement vStatement2 = (VariableDeclarationStatement) statement2;
                if (isInputDeclarationStatement(vStatement1) || isExpectedDeclarationStatement(vStatement2)) {
                    this.input = getInputExpression(vStatement1);
                    this.expected = getExpectedExpression(vStatement2);
                    this.inputType = (SimpleType) vStatement1.getType();
                    this.expectedType = (SimpleType) vStatement2.getType();
                    this.targets.put(INPUT_NAME, this.input);
                    this.targets.put(EXPECTED_NAME, this.expected);
                    this.inputStatement = vStatement1;
                    this.expectedStatement = vStatement2;
                } else if (isExpectedDeclarationStatement(vStatement1) || isInputDeclarationStatement(vStatement2)) {
                    this.expected = getInputExpression(vStatement1);
                    this.input = getExpectedExpression(vStatement2);
                    this.expectedType = (SimpleType) vStatement1.getType();
                    this.inputType = (SimpleType) vStatement2.getType();
                    this.targets.put(INPUT_NAME, this.input);
                    this.targets.put(EXPECTED_NAME, this.expected);
                    this.expectedStatement = vStatement1;
                    this.inputStatement = vStatement2;
                } else {
                    System.out.println("NotFoundInputAndExpected!");
                    System.out.println("Statement1: " + vStatement1);
                    System.out.println("Statement2: " + vStatement2);
                }
            }
        } else {
            System.out.println("Limitation");
            System.out.println("===========");
            System.out.println(statements);
            System.out.println("===========");
        }
    }

    private boolean isInputDeclarationStatement(VariableDeclarationStatement statement) {
        for (Object object : statement.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
            if (fragment.getName().toString().equals("actual")
                    || fragment.getName().toString().equals("input")) {
                return true;
            }
        }
        return false;
    }

    private boolean isExpectedDeclarationStatement(VariableDeclarationStatement statement) {
        for (Object object : statement.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
            if (fragment.getName().toString().equals("expected")) {
                return true;
            }
        }
        return false;
    }

    private Expression getInputExpression(VariableDeclarationStatement variableDeclaration) {
        if (variableDeclaration.fragments().size() == 1) {
            VariableDeclarationFragment fragment =
                    (VariableDeclarationFragment) variableDeclaration.fragments().get(0);
            return fragment.getInitializer();
        }
        for (Object obj : variableDeclaration.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
            if (fragment.getName().toString().equals("actual") ||
                    fragment.getName().toString().equals("input")) {
                return fragment.getInitializer();
            }
        }
        return null;
    }
    private Expression getExpectedExpression(VariableDeclarationStatement variableDeclaration) {
        if (variableDeclaration.fragments().size() == 1) {
            VariableDeclarationFragment fragment =
                    (VariableDeclarationFragment) variableDeclaration.fragments().get(0);
            return fragment.getInitializer();
        }
        for (Object obj : variableDeclaration.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
            if (fragment.getName().toString().equals("expected")) {
                return fragment.getInitializer();
            }
        }
        return null;
    }
}
