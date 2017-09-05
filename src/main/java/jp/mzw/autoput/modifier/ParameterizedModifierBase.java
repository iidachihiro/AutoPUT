package jp.mzw.autoput.modifier;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.Project;
import jp.mzw.autoput.core.TestCase;
import jp.mzw.autoput.core.TestSuite;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/28/17.
 */
public class ParameterizedModifierBase extends AbstractModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterizedModifier.class);
    // ユーザが変更可能
    protected static final String CLASS_NAME = "AutoPutTest";
    protected static final String METHOD_NAME = "autoPutTest";
    protected static final String INPUT_VAR = "_input";
    protected static final String EXPECTED_VAR = "_expected";
    protected static final String FIXTURE_CLASS = "Fixture";
    protected static final String FIXTURE_NAME = "fixture";

    // 変更不可
    protected static final String DATA_METHOD = "data";

    protected AST ast;
    protected CompilationUnit cu;
    protected ASTRewrite rewrite;


    public ParameterizedModifierBase(Project project) {
        super(project);
    }

    public ParameterizedModifierBase(TestSuite testSuite) {
        super(testSuite);
    }

    public ParameterizedModifierBase(Project project, TestSuite testSuite) {
        super(testSuite);
        this.project = project;
    }

    public CompilationUnit getCompilationUnit() {
        return testSuite.getCu();
    }
    
    protected void outputConvertResult(String methodName, String content) {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getConvertResultDir() + "/"
                + testSuite.getTestClassName() + "/" + methodName + ".txt"))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void modify(MethodDeclaration method) {
        cu = getCompilationUnit();
        ast = cu.getAST();
        String methodName = method.getName().getIdentifier();
        System.out.println(methodName);
        rewrite = ASTRewrite.create(ast);
        // テストメソッドを作成(既存のテストメソッドを修正する)
        modifyTestMethod(method);
        // data-pointsを作成
        createDataPoints(method);
        // 既存のテストメソッドを全て削除(コンストラクタと@Testのないものは残る)
        deleteOtherTestMethods(method);
        // コンストラクタ名を修正
        modifyConstructor(method);
        // import文を追加
        addImportDeclarations();
        // Fixtureクラスを追加
        addFixtureClass(method);
        // クラス名を変更，修飾子も追加
        modifyClassInfo();
        // クラス内に出てくるクラス名をすべて変更
        modifyClassName();
        // modify
        try {
            Document document = new Document(testSuite.getTestSources());
            TextEdit edit = rewrite.rewriteAST(document, null);
            edit.apply(document);
            outputConvertResult(methodName, document.get());
        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            LOGGER.error("{}: {} at {}", e.getClass(), e.getMessage(), methodName);
        }
        // initialize
        this.cu = null;
        this.ast = null;
        this.rewrite = null;
        return;
    }

    protected void addImportDeclarations() {
        ListRewrite listRewrite = rewrite.getListRewrite(getCompilationUnit(), CompilationUnit.IMPORTS_PROPERTY);
        // import文を生成
        ImportDeclaration runWith = ast.newImportDeclaration();
        runWith.setName(ast.newName(new String[]{"org", "junit", "runner", "RunWith"}));
        ImportDeclaration theories = ast.newImportDeclaration();
        theories.setName(ast.newName(new String[] {"org", "junit", "experimental", "theories", "Theories"}));
        ImportDeclaration theory = ast.newImportDeclaration();
        theory.setName(ast.newName(new String[] {"org", "junit", "experimental", "theories", "Theory"}));
        ImportDeclaration datapoints = ast.newImportDeclaration();
        datapoints.setName(ast.newName(new String[] {"org", "junit", "experimental", "theories", "DataPoints"}));
        // importを付与
        listRewrite.insertLast(runWith, null);
        listRewrite.insertLast(theories, null);
        listRewrite.insertLast(theory, null);
        listRewrite.insertLast(datapoints, null);
    }

    protected void modifyClassName() {
        TypeDeclaration modified = getTargetType();
        String typeName = modified.getName().getIdentifier();
        List<Name> names = ASTUtils.getAllNames(modified);
        for (Name name : names) {
            if (name.toString().equals(typeName)) {
                Name replace = ast.newName(CLASS_NAME);
                rewrite.replace(name, replace, null);
            }
        }
    }


    protected void modifyClassInfo() {
        TypeDeclaration modified = getTargetType();
        // 修飾子を変更
        ListRewrite modifiersListRewrite = rewrite.getListRewrite(modified, TypeDeclaration.MODIFIERS2_PROPERTY);
        // RunWithアノテーションを付与
        SingleMemberAnnotation annotation = ASTUtils.getRunWithAnnotation(ast);
        modifiersListRewrite.insertLast(annotation, null);
        // クラス名を変更
        rewrite.replace(modified.getName(), ast.newName(CLASS_NAME), null);
    }



    protected FieldDeclaration createFieldDeclaration(Type type, String var) {
        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setName(ast.newSimpleName(var));
        FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
        fieldDeclaration.setType(type);
        fieldDeclaration.modifiers().add(ASTUtils.getPrivateModifier(ast));
        return fieldDeclaration;
    }




    protected ExpressionStatement createConstructorBlockAssignment(String var) {
        // "this.var = var;" を生成
        Assignment assignment = ast.newAssignment();
        assignment.setOperator(Assignment.Operator.ASSIGN);
        FieldAccess leftOperand = ast.newFieldAccess();
        leftOperand.setExpression(ast.newThisExpression());
        leftOperand.setName(ast.newSimpleName(var));
        assignment.setLeftHandSide(leftOperand);
        assignment.setRightHandSide(ast.newSimpleName(var));
        ExpressionStatement ret = ast.newExpressionStatement(assignment);
        return ret;
    }

    protected TypeDeclaration getTargetType() {
        TypeDeclaration target = null;
        for (AbstractTypeDeclaration abstType : (List<AbstractTypeDeclaration>) getCompilationUnit().types()) {
            if (abstType instanceof TypeDeclaration) {
                TypeDeclaration type = (TypeDeclaration) abstType;
                if (type.getName().getIdentifier().equals(testSuite.getTestClassName())) {
                    target = type;
                }
            }
        }
        return target;
    }


    protected void addMethod(MethodDeclaration method) {
        TypeDeclaration modified = getTargetType();
        ListRewrite listRewrite = rewrite.getListRewrite(modified, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        listRewrite.insertLast(method, null);
    }


    protected void createDataMethod(MethodDeclaration origin) {
        MethodDeclaration method = ast.newMethodDeclaration();
        method.setConstructor(false);
        // メソッド名を設定
        method.setName(ast.newSimpleName(DATA_METHOD));
        // 修飾子を設定
        ListRewrite modifiersListRewrite = rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
        modifiersListRewrite.insertLast(ASTUtils.getParametersAnnotation(ast), null);
        modifiersListRewrite.insertLast(ASTUtils.getPublicModifier(ast), null);
        modifiersListRewrite.insertLast(ASTUtils.getStaticModifier(ast), null);
        // returnの型(Collection<Object[]>)を設定
        ParameterizedType type = ast.newParameterizedType(ast.newSimpleType(ast.newName("Collection")));
        ArrayType objectArray = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        type.typeArguments().add(objectArray);
        method.setReturnType2(type);
        // bodyを設定
        method.setBody(_createDataBody(origin));

        // dataメソッドを追加
        TypeDeclaration modified = getTargetType();
        ListRewrite listRewrite = rewrite.getListRewrite(modified, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        listRewrite.insertLast(method, null);
        return;
    }

    protected void createDataPoints(MethodDeclaration origin) {
        // private static String[] INPUT1 = { "foo1", "foo2" };
        // private static String[] EXPECTED1 = { "bar1", "bar2" };
        // private static String[] INPUT2 = { "foo3", "foo4" };
        // private static String[] EXPECTED2 = { "bar3", "bar4" };
        // @DataPoints
        // public static AutoPutTest[] DATA = {
        //         new AutoPutTest(INPUT1, EXPECTED1),
        //         new AutoPutTest(INPUT2, EXPECTED2)
        // };
        // を作る

        List<MethodDeclaration> similarMethods = new ArrayList<>();
        List<ASTNode> mostDifferentNodes = null;
        // 共通部分が一番少ないメソッドを探す && similarなメソッドを集める
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (similarAST(origin, method)) {
                similarMethods.add(method);
                if (mostDifferentNodes == null) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                } else if (mostDifferentNodes.size() < ASTUtils.getDifferentNodes(origin, method).size()) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                }
            }
        }

        // originのnodeをinputとexpectedに分ける
        List<ASTNode> inputRelatedNodes = new ArrayList<>();
        List<ASTNode> expectedRelatedNodes = new ArrayList<>();
        for (ASTNode node : mostDifferentNodes) {
            if (isInExpectedDeclaringNode(origin, node)) {
                expectedRelatedNodes.add(node);
            } else {
                inputRelatedNodes.add(node);
            }
        }
        // 各テストメソッドからinputとexpectedを抜き出して追加する
        List<ASTNode> originNodes = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(origin));
        for (int i = 0; i < similarMethods.size(); i++) {
            MethodDeclaration similarMethod = similarMethods.get(i);
            List<ASTNode> methodNodes = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(similarMethod));
            List<ASTNode> inputs = new ArrayList<>();
            List<ASTNode> expecteds = new ArrayList<>();
            for (int j = 0; j < originNodes.size(); j++) {
                ASTNode originNode = originNodes.get(j);
                if (expectedRelatedNodes.contains(originNode)) {
                    expecteds.add(methodNodes.get(j));
                } else if (inputRelatedNodes.contains(originNode)) {
                    inputs.add(methodNodes.get(j));
                }
            }
            VariableDeclarationFragment inputFragment = ast.newVariableDeclarationFragment();
            inputFragment.setName(ast.newSimpleName("INPUT" + (i + 1)));
            // 抜き出したinputが1つならそのまま，複数なら{}に入れて追加
            if (inputs.size() == 0) {
                inputFragment.setInitializer(ast.newNullLiteral());
            } else if (inputs.size() == 1) {
                Expression expression = (Expression) ASTNode.copySubtree(ast, inputs.get(0));
                inputFragment.setInitializer(expression);
            } else {
                ArrayInitializer inputArray = ast.newArrayInitializer();
                ListRewrite inputListRewrite = rewrite.getListRewrite(inputArray, ArrayInitializer.EXPRESSIONS_PROPERTY);
                for (ASTNode input : inputs) {
                    inputListRewrite.insertLast(input, null);
                }
                inputFragment.setInitializer(inputArray);
            }
            VariableDeclarationStatement inputStatement = ast.newVariableDeclarationStatement(inputFragment);
            inputStatement.setType(getInputType(origin));
            inputStatement.modifiers().add(ASTUtils.getPrivateModifier(ast));
            inputStatement.modifiers().add(ASTUtils.getStaticModifier(ast));

            VariableDeclarationFragment expectedFragment = ast.newVariableDeclarationFragment();
            expectedFragment.setName(ast.newSimpleName("EXPECTED" + (i + 1)));
            // 抜き出したexpectが1つならそのまま，複数なら{}に入れて追加
            if (expecteds.size() == 0) {
                expectedFragment.setInitializer(ast.newNullLiteral());
            } else if (expecteds.size() == 1) {
                Expression expression = (Expression) ASTNode.copySubtree(ast, expecteds.get(0));
                expectedFragment.setInitializer(expression);
            } else {
                ArrayInitializer expectedArray = ast.newArrayInitializer();
                ListRewrite expectedListRewrite = rewrite.getListRewrite(expectedArray, ArrayInitializer.EXPRESSIONS_PROPERTY);
                for (ASTNode expected : expecteds) {
                    expectedListRewrite.insertLast(expected, null);
                }
                expectedFragment.setInitializer(expectedArray);
            }
            VariableDeclarationStatement expectedStatement = ast.newVariableDeclarationStatement(expectedFragment);
            expectedStatement.setType(getExpectedType(origin));
            expectedStatement.modifiers().add(ASTUtils.getPrivateModifier(ast));
            expectedStatement.modifiers().add(ASTUtils.getStaticModifier(ast));

            TypeDeclaration modified = getTargetType();
            ListRewrite listRewrite = rewrite.getListRewrite(modified, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
            listRewrite.insertLast(inputStatement, null);
            listRewrite.insertLast(expectedStatement, null);
        }

        // @DataPoints public static Fixture[] DATA = { new Fixture(INPUT1,EXPECTED1),new Fixture(INPUT2,EXPECTED2)};
        // を生成
        VariableDeclarationFragment dataPointsFragment = ast.newVariableDeclarationFragment();
        dataPointsFragment.setName(ast.newSimpleName("DATA"));
        ArrayInitializer dataPointArray = ast.newArrayInitializer();
        dataPointsFragment.setInitializer(dataPointArray);
        ListRewrite dataPointListRewrite = rewrite.getListRewrite(dataPointArray, ArrayInitializer.EXPRESSIONS_PROPERTY);
        for (int i = 0; i < similarMethods.size(); i++) {
            ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
            classInstanceCreation.setType(ast.newSimpleType(ast.newName(FIXTURE_CLASS)));
            classInstanceCreation.arguments().add(ast.newSimpleName("INPUT" + (i + 1)));
            classInstanceCreation.arguments().add(ast.newSimpleName("EXPECTED" + (i + 1)));
            dataPointListRewrite.insertLast(classInstanceCreation, null);
        }
        VariableDeclarationStatement dataPointStatement = ast.newVariableDeclarationStatement(dataPointsFragment);
        dataPointStatement.setType(ast.newArrayType(ast.newSimpleType(ast.newName(FIXTURE_CLASS))));
        dataPointStatement.modifiers().add(ASTUtils.getDataPointsAnnotation(ast));
        dataPointStatement.modifiers().add(ASTUtils.getPublicModifier(ast));
        dataPointStatement.modifiers().add(ASTUtils.getStaticModifier(ast));
        TypeDeclaration modified = getTargetType();
        ListRewrite listRewrite = rewrite.getListRewrite(modified, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        listRewrite.insertLast(dataPointStatement, null);
        return;
    }

    protected Block _createDataBody(MethodDeclaration origin) {
        // return Arrays.asList(new Object[][] {{ input1, expected1 }, { input2, expected2 }}); を作成する
        // まず, new Object[][] {{ input1, expected1 }, { input2, expected2 }} を作成
        ArrayType arrayType = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        arrayType.dimensions().add(ast.newDimension());
        ArrayCreation arrayCreation = ast.newArrayCreation();
        arrayCreation.setType(arrayType);
        // {{ input1, expected1 }, { input2, expected2 }} を設定
        arrayCreation.setInitializer(createInputAndExpected(origin));
        // Arrays.asListを作成
        MethodInvocation content = ast.newMethodInvocation();
        content.setExpression(ast.newSimpleName("Arrays"));
        content.setName(ast.newSimpleName("asList"));
        content.arguments().add(arrayCreation);
        // return文を作成
        ReturnStatement returnStatement = ast.newReturnStatement();
        returnStatement.setExpression(content);
        Block block = ast.newBlock();
        block.statements().add(returnStatement);
        return block;
    }

    protected ArrayInitializer createInputAndExpected(MethodDeclaration origin) {
        // {{ input1, expected1 }, { input2, expected2 }} を作る

        List<MethodDeclaration> similarMethods = new ArrayList<>();
        List<ASTNode> mostDifferentNodes = null;
        // 共通部分が一番少ないメソッドを探す && similarなメソッドを集める
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (similarAST(origin, method)) {
                similarMethods.add(method);
                if (mostDifferentNodes == null) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                } else if (mostDifferentNodes.size() < ASTUtils.getDifferentNodes(origin, method).size()) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                }
            }
        }

        // originのnodeをinputとexpectedに分ける
        List<ASTNode> inputRelatedNodes = new ArrayList<>();
        List<ASTNode> expectedRelatedNodes = new ArrayList<>();
        for (ASTNode node : mostDifferentNodes) {
            if (isInExpectedDeclaringNode(origin, node)) {
                expectedRelatedNodes.add(node);
            } else {
                inputRelatedNodes.add(node);
            }
        }
        // 各テストメソッドからinputとexpectedを抜き出して追加する
        ArrayInitializer arrayInitializer = ast.newArrayInitializer();
        ListRewrite listRewrite = rewrite.getListRewrite(arrayInitializer, ArrayInitializer.EXPRESSIONS_PROPERTY);

        List<ASTNode> originNodes = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(origin));
        for (MethodDeclaration similarMethod : similarMethods) {
            List<ASTNode> methodNodes = ASTUtils.flattenMinusNumberLiteral(ASTUtils.getAllNodes(similarMethod));
            List<ASTNode> inputs = new ArrayList<>();
            List<ASTNode> expecteds = new ArrayList<>();
            for (int i = 0; i < originNodes.size(); i++) {
                ASTNode originNode = originNodes.get(i);
                if (expectedRelatedNodes.contains(originNode)) {
                    expecteds.add(methodNodes.get(i));
                } else if (inputRelatedNodes.contains(originNode)) {
                    inputs.add(methodNodes.get(i));
                }
            }
            ArrayInitializer inputAndExpected = ast.newArrayInitializer();
            ListRewrite dataListRewrite = rewrite.getListRewrite(inputAndExpected, ArrayInitializer.EXPRESSIONS_PROPERTY);
            // 抜き出したinputが1つならそのまま，複数なら{}に入れて追加
            if (inputs.size() == 1) {
                dataListRewrite.insertLast(inputs.get(0), null);
            } else {
                ArrayInitializer inputArray = ast.newArrayInitializer();
                ListRewrite inputListRewrite = rewrite.getListRewrite(inputArray, ArrayInitializer.EXPRESSIONS_PROPERTY);
                for (ASTNode input : inputs) {
                    inputListRewrite.insertLast(input, null);
                }
                dataListRewrite.insertLast(inputArray, null);
            }
            // 抜き出したexpectが1つならそのまま，複数なら{}に入れて追加
            if (expecteds.size() == 1) {
                dataListRewrite.insertLast(expecteds.get(0), null);
            } else {
                ArrayInitializer expectedArray = ast.newArrayInitializer();
                ListRewrite expectedListRewrite = rewrite.getListRewrite(expectedArray, ArrayInitializer.EXPRESSIONS_PROPERTY);
                for (ASTNode expected : expecteds) {
                    expectedListRewrite.insertLast(expected, null);
                }
                dataListRewrite.insertLast(expectedArray, null);
            }
            listRewrite.insertLast(inputAndExpected, null);
        }
        return arrayInitializer;
    }

    protected void modifyTestMethod(MethodDeclaration origin) {
        // 名前を変更
        rewrite.replace(origin.getName(), ast.newSimpleName(METHOD_NAME), null);
        // 引数を設定
        SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
        parameter.setType(ast.newSimpleType(ast.newName(FIXTURE_CLASS)));
        parameter.setName(ast.newSimpleName(FIXTURE_NAME));
        ListRewrite argsRewrite = rewrite.getListRewrite(origin, MethodDeclaration.PARAMETERS_PROPERTY);
        argsRewrite.insertLast(parameter, null);
        // @Testを削除し，@Theoryを追加
        ListRewrite annotationRewrite = rewrite.getListRewrite(origin, MethodDeclaration.MODIFIERS2_PROPERTY);
        for (IExtendedModifier modifier : (List<IExtendedModifier>) origin.modifiers()) {
            if (modifier.isModifier()) {
                continue;
            }
            Annotation annotation = (Annotation) modifier;
            // @Testを削除
            if (annotation.getTypeName().toString().equals("Test")) {
                annotationRewrite.remove(annotation, null);
            }
        }
        // @Theoryを追加
        annotationRewrite.insertFirst(ASTUtils.getThoryAnnotation(ast), null);
        // 中身を追加
        _modifyTestMethod(origin);
    }

    protected void _modifyTestMethod(MethodDeclaration origin) {
        List<ASTNode> mostDifferentNodes = null;
        // 共通部分が一番少ないメソッドを探す
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (method.equals(origin)) {
                continue;
            }
            if (similarAST(origin, method)) {
                if (mostDifferentNodes == null) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                } else if (mostDifferentNodes.size() < ASTUtils.getDifferentNodes(origin, method).size()) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                }
            }
        }
        // commonじゃないnodeをinputとexpectedに変更する
        // commonsじゃないnodeをinputとexpectedに分ける
        List<ASTNode> inputRelatedNodes = new ArrayList<>();
        List<ASTNode> expectedRelatedNodes = new ArrayList<>();
        for (ASTNode node : mostDifferentNodes) {
            if (isInExpectedDeclaringNode(origin, node)) {
                expectedRelatedNodes.add(node);
            } else {
                inputRelatedNodes.add(node);
            }
        }
        // expectedを置換する
        if (expectedRelatedNodes.size() == 1) {
            ASTNode target = expectedRelatedNodes.get(0);
            if (ASTUtils.isNumberLiteralWithPrefixedMinus(target)) {
                target = target.getParent();
            }
            FieldAccess replace = ast.newFieldAccess();
            SimpleName expected = ast.newSimpleName(EXPECTED_VAR);
            replace.setName(expected);
            SimpleName fixture = ast.newSimpleName(FIXTURE_NAME);
            replace.setExpression(fixture);
            rewrite.replace(target, replace, null);
        } else {
            for (int i = 0; i < expectedRelatedNodes.size(); i++) {
                ASTNode target = expectedRelatedNodes.get(i);

                ASTNode replace;
                QualifiedName fixtureExpected = ast.newQualifiedName(ast.newSimpleName(FIXTURE_NAME), ast.newSimpleName(EXPECTED_VAR));
                ArrayAccess arrayAccess = ast.newArrayAccess();
                arrayAccess.setArray(fixtureExpected);
                arrayAccess.setIndex(ast.newNumberLiteral(String.valueOf(i)));
                boolean isObjectArrayType = isObjectArrayType(getExpectedType(origin));
                if (isObjectArrayType && isBooleanType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Boolean");
                } else if (isObjectArrayType && isCharacterType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Character");
                } else if (isObjectArrayType && isByteType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Byte");
                } else if (isObjectArrayType && isShortType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Short");
                } else if (isObjectArrayType && isIntType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Integer");
                } else if (isObjectArrayType && isLongType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Long");
                } else if (isObjectArrayType && isFloatType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Float");
                } else if (isObjectArrayType && isDoubleType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Double");
                } else if (isObjectArrayType && canBeCasted(target)) {
                    replace = _castObject(arrayAccess, target);
                } else {
                    replace = arrayAccess;
                }
                if (ASTUtils.isNumberLiteralWithPrefixedMinus(target)) {
                    target = target.getParent();
                }
                rewrite.replace(target, replace, null);
            }
        }
        // inputを置換する
        if (inputRelatedNodes.size() == 1) {
            ASTNode target = inputRelatedNodes.get(0);
            if (ASTUtils.isNumberLiteralWithPrefixedMinus(target)) {
                target = target.getParent();
            }
            FieldAccess replace = ast.newFieldAccess();
            SimpleName input = ast.newSimpleName(INPUT_VAR);
            replace.setName(input);
            SimpleName fixture = ast.newSimpleName(FIXTURE_NAME);
            replace.setExpression(fixture);
            rewrite.replace(target, replace, null);
        } else {
            for (int i = 0; i < inputRelatedNodes.size(); i++) {
                ASTNode target = inputRelatedNodes.get(i);
                ASTNode replace;
                QualifiedName fixtureInput = ast.newQualifiedName(ast.newSimpleName(FIXTURE_NAME), ast.newSimpleName(INPUT_VAR));
                ArrayAccess arrayAccess = ast.newArrayAccess();
                arrayAccess.setArray(fixtureInput);
                arrayAccess.setIndex(ast.newNumberLiteral(String.valueOf(i)));
                boolean isObjectArrayType = isObjectArrayType(getInputType(origin));
                if (isObjectArrayType && isBooleanType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Boolean");
                } else if (isObjectArrayType && isCharacterType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Character");
                } else if (isObjectArrayType && isByteType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Byte");
                } else if (isObjectArrayType && isShortType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Short");
                } else if (isObjectArrayType && isIntType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Integer");
                } else if (isObjectArrayType && isLongType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Long");
                } else if (isObjectArrayType && isFloatType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Float");
                } else if (isObjectArrayType && isDoubleType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Double");
                } else if (isObjectArrayType && canBeCasted(target)) {
                    replace = _castObject(arrayAccess, target);
                } else {
                    replace = arrayAccess;
                }
                if (ASTUtils.isNumberLiteralWithPrefixedMinus(target)) {
                    target = target.getParent();
                }
                rewrite.replace(target, replace, null);
            }
        }

        return;
    }

    protected Type getInputType(MethodDeclaration origin) {
        List<ASTNode> mostDifferentNodesInOrigin = null;
        List<ASTNode> mostDifferentNodesFromAnother = null;
        // 共通部分が一番少ないメソッドを探す
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (method.equals(origin)) {
                continue;
            }
            if (similarAST(origin, method)) {
                if (mostDifferentNodesInOrigin == null) {
                    mostDifferentNodesInOrigin = ASTUtils.getDifferentNodes(method, origin);
                } else if (mostDifferentNodesInOrigin.size() < ASTUtils.getDifferentNodes(origin, method).size()) {
                    mostDifferentNodesInOrigin = ASTUtils.getDifferentNodes(method, origin);
                    mostDifferentNodesFromAnother = ASTUtils.getDifferentNodes(origin, method);
                }
            }
        }
        // commonじゃないnodeからinputを抽出する
        List<ASTNode> inputRelatedNodesInOrigin = new ArrayList<>();
        for (ASTNode node : mostDifferentNodesInOrigin) {
            if (!isInExpectedDeclaringNode(origin, node)) {
                inputRelatedNodesInOrigin.add(node);
            }
        }
        Type ret;
        Type commonTypeInOrigin = _getCommonType(inputRelatedNodesInOrigin);
        ret = commonTypeInOrigin;
        if (commonTypeInOrigin instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) commonTypeInOrigin;
            if (arrayType.getElementType() instanceof PrimitiveType) {
                PrimitiveType primitiveType = (PrimitiveType) arrayType.getElementType();
                if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.INT) {
                    List<ASTNode> inputRelatedNodesFromAnother = new ArrayList<>();
                    for (ASTNode node : mostDifferentNodesFromAnother) {
                        if (!isInExpectedDeclaringNode(origin, node)) {
                            inputRelatedNodesFromAnother.add(node);
                        }
                    }
                    Type commonTypeFromAother = _getCommonType(inputRelatedNodesFromAnother);
                    if (commonTypeFromAother instanceof ArrayType) {
                        ArrayType arrayType1 = (ArrayType) commonTypeFromAother;
                        if (arrayType1.getElementType() instanceof PrimitiveType) {
                            PrimitiveType primitiveType1 = (PrimitiveType) arrayType1.getElementType();
                            if (primitiveType1.getPrimitiveTypeCode() == PrimitiveType.DOUBLE) {
                                ret = commonTypeFromAother;
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    public Type getExpectedType(MethodDeclaration origin) {
        List<ASTNode> mostDifferentNodes = null;
        // 共通部分が一番少ないメソッドを探す
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (method.equals(origin)) {
                continue;
            }
            if (similarAST(origin, method)) {
                if (mostDifferentNodes == null) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                } else if (mostDifferentNodes.size() < ASTUtils.getDifferentNodes(origin, method).size()) {
                    mostDifferentNodes = ASTUtils.getDifferentNodes(method, origin);
                }
            }
        }
        // commonじゃないnodeからinputを抽出する
        List<ASTNode> expectedRelatedNodes = new ArrayList<>();
        for (ASTNode node : mostDifferentNodes) {
            if (isInExpectedDeclaringNode(origin, node)) {
                expectedRelatedNodes.add(node);
            }
        }
        Type ret;
        if (1 < expectedRelatedNodes.size()) {
            ret = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
            if (ASTUtils.allStringType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newSimpleType(ast.newName("String")));
            } else if (ASTUtils.allByteType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.BYTE));
            } else if (ASTUtils.allShortType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.SHORT));
            } else if (ASTUtils.allIntType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.INT));
            } else if (ASTUtils.allLongType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.LONG));
            } else if (ASTUtils.allFloatType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.FLOAT));
            } else if (ASTUtils.allDoubleType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.DOUBLE));
            } else if (ASTUtils.allCharacterType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.CHAR));
            } else if (ASTUtils.allBooleanType(expectedRelatedNodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
            }
        } else {
            ret = ast.newSimpleType(ast.newName("Object"));
            if (ASTUtils.allStringType(expectedRelatedNodes)) {
                ret = ast.newSimpleType(ast.newName("String"));
            } else if (ASTUtils.allByteType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.BYTE);
            } else if (ASTUtils.allShortType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.SHORT);
            } else if (ASTUtils.allIntType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.INT);
            } else if (ASTUtils.allLongType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.LONG);
            } else if (ASTUtils.allFloatType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.FLOAT);
            } else if (ASTUtils.allDoubleType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.DOUBLE);
            } else if (ASTUtils.allCharacterType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.CHAR);
            } else if (ASTUtils.allBooleanType(expectedRelatedNodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            }
        }
        return ret;
    }


    private boolean isInExpectedDeclaringNode(MethodDeclaration origin, ASTNode node) {
        List<MethodInvocation> assertions = ASTUtils.getAllAssertions(origin);
        for (MethodInvocation assertion : assertions) {
            Expression expected;
            ASTNode expectedDeclaringNode = null;
            if (assertion.arguments().size() == 2) {
                expected = (Expression) assertion.arguments().get(0);
                if (ASTUtils.isLiteralNode(expected) && expected.equals(node)) {
                    return true;
                } else if (expected instanceof Name) {
                    expectedDeclaringNode = getCompilationUnit().findDeclaringNode(((Name) expected).resolveBinding());
                }
            } else if (assertion.arguments().size() == 3) {
                expected = (Expression) assertion.arguments().get(1);
                if (ASTUtils.isLiteralNode(expected) && expected.equals(node)) {
                    return true;
                } else if (expected instanceof Name) {
                    expectedDeclaringNode = getCompilationUnit().findDeclaringNode(((Name) expected).resolveBinding());
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


    protected void deleteOtherTestMethods(MethodDeclaration origin) {
        TypeDeclaration modified = getTargetType();
        ListRewrite listRewrite = rewrite.getListRewrite(modified, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        for (MethodDeclaration methodDeclaration : modified.getMethods()) {
            if (methodDeclaration.equals(origin)) {
                continue;
            }
            if (ASTUtils.hasTestAnnotation(methodDeclaration.modifiers())) {
                listRewrite.remove(methodDeclaration, null);
            }
        }
    }

    protected void modifyConstructor(MethodDeclaration origin) {
        TypeDeclaration modified = getTargetType();
        for (MethodDeclaration methodDeclaration : modified.getMethods()) {
            if (methodDeclaration.isConstructor()) {
                Name target = methodDeclaration.getName();
                Name replace = ast.newName(CLASS_NAME);
                rewrite.replace(target, replace, null);
            }
        }
    }

    /* ------------------------- For Fixture Class ----------------------- */
    protected void addFixtureClass(MethodDeclaration origin) {
        // create Fixture Class
        TypeDeclaration fixtureClass = ast.newTypeDeclaration();
        fixtureClass.setName(ast.newSimpleName(FIXTURE_CLASS));
        ListRewrite modifiersRewrite = rewrite.getListRewrite(fixtureClass, TypeDeclaration.MODIFIERS2_PROPERTY);
        modifiersRewrite.insertLast(ASTUtils.getPublicModifier(ast), null);
        modifiersRewrite.insertLast(ASTUtils.getStaticModifier(ast), null);
        addFieldDeclarations(origin, fixtureClass);
        addConstructor(origin, fixtureClass);
        // add Fixture Class
        TypeDeclaration target = getTargetType();
        ListRewrite targetRewrite = rewrite.getListRewrite(target, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        targetRewrite.insertLast(fixtureClass, null);
    }

    protected void addFieldDeclarations(MethodDeclaration origin, TypeDeclaration fixtureClass) {
        TypeDeclaration target = fixtureClass;
        if (fixtureClass == null) {
            target = getTargetType();
        }
        // フィールド変数定義を生成
        FieldDeclaration inputDeclaration = createFieldDeclaration(getInputType(origin), INPUT_VAR);
        FieldDeclaration expectedDeclaration = createFieldDeclaration(getExpectedType(origin), EXPECTED_VAR);
        // フィールド変数定義を追加
        ListRewrite bodyDeclarationsListRewrite = rewrite.getListRewrite(target, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        bodyDeclarationsListRewrite.insertLast(inputDeclaration, null);
        bodyDeclarationsListRewrite.insertLast(expectedDeclaration, null);
    }

    protected void addConstructor(MethodDeclaration origin, TypeDeclaration fixtureClass) {
        TypeDeclaration target = fixtureClass;
        if (fixtureClass == null) {
            target = getTargetType();
        }
        // コンストラクタを生成
        MethodDeclaration constructor = ast.newMethodDeclaration();
        constructor.setConstructor(true);
        // 名前を設定
        constructor.setName(ast.newSimpleName(FIXTURE_CLASS));
        ListRewrite modifiersListRewrite = rewrite.getListRewrite(constructor, MethodDeclaration.MODIFIERS2_PROPERTY);
        // public修飾子を付与
        modifiersListRewrite.insertLast(ASTUtils.getPublicModifier(ast), null);
        // 引数を設定
        // inputの型と名前を設定
        SingleVariableDeclaration input = ast.newSingleVariableDeclaration();
        input.setType(getInputType(origin));
        input.setName(ast.newSimpleName(INPUT_VAR));
        // expectedの型と名前を設定
        SingleVariableDeclaration expected = ast.newSingleVariableDeclaration();
        expected.setType(getExpectedType(origin));
        expected.setName(ast.newSimpleName(EXPECTED_VAR));
        // 引数を追加
        ListRewrite parametersListRewrite = rewrite.getListRewrite(constructor, MethodDeclaration.PARAMETERS_PROPERTY);
        parametersListRewrite.insertLast(input, null);
        parametersListRewrite.insertLast(expected, null);
        // bodyを設定
        Block body = ast.newBlock();
        ExpressionStatement inputStatement = createConstructorBlockAssignment(INPUT_VAR);
        ExpressionStatement expectedStatement = createConstructorBlockAssignment(EXPECTED_VAR);
        ListRewrite bodyListRewrite = rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
        bodyListRewrite.insertLast(inputStatement, null);
        bodyListRewrite.insertLast(expectedStatement, null);
        constructor.setBody(body);
        // コンストラクタをクラスに追加
        ListRewrite bodyRewrite = rewrite.getListRewrite(target, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        bodyRewrite.insertLast(constructor, null);
    }
    /* -------------------------- private method --------------------------- */
    private boolean isObjectArrayType(Type type) {
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

    private boolean isDoubleArrayType(Type type) {
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

    private boolean isBooleanType(ASTNode node) {
        return _isThisType(node, "boolean");
    }
    private boolean isCharacterType(ASTNode node) {
        return _isThisType(node, "char");
    }
    private boolean isByteType(ASTNode node) {
        return _isThisType(node, "byte");
    }
    private boolean isShortType(ASTNode node) {
        return _isThisType(node, "short");
    }
    private boolean isIntType(ASTNode node) {
        return _isThisType(node, "int");
    }
    private boolean isLongType(ASTNode node) {
        return _isThisType(node, "long");
    }
    private boolean isFloatType(ASTNode node) {
        return _isThisType(node, "float");
    }
    private boolean isDoubleType(ASTNode node) {
        return _isThisType(node, "double");
    }
    private boolean _isThisType(ASTNode node, String type) {
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

    private ClassInstanceCreation _convertObjectToPrimitiveWrapper(Expression expression, String type) {
        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(expression);
        methodInvocation.setName(ast.newSimpleName("toString"));
        ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
        ListRewrite argsRewrite = rewrite.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
        argsRewrite.insertLast(methodInvocation, null);
        classInstanceCreation.setType(ast.newSimpleType(ast.newName(type)));
        return classInstanceCreation;
    }

    private boolean canBeCasted(ASTNode node) {
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
    private CastExpression _castObject(Expression expression, ASTNode target) {
        CastExpression castExpression = ast.newCastExpression();
        castExpression.setExpression(expression);
        ITypeBinding iTypeBinding = ((Expression) target).resolveTypeBinding();
        Type castType = ast.newSimpleType(ast.newName(iTypeBinding.getName()));
        castExpression.setType(castType);
        return castExpression;
    }

    private CastExpression _castInt(Expression expression, ASTNode target) {
        CastExpression castExpression = ast.newCastExpression();
        castExpression.setExpression(expression);
        Type castType = ast.newPrimitiveType(PrimitiveType.INT);
        castExpression.setType(castType);
        return castExpression;
    }

    private Type _getCommonType(List<ASTNode> nodes) {
        Type ret;
        if (1 < nodes.size()) {
            ret = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
            if (ASTUtils.allStringType(nodes)) {
                ret = ast.newArrayType(ast.newSimpleType(ast.newName("String")));
            } else if (ASTUtils.allByteType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.BYTE));
            } else if (ASTUtils.allShortType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.SHORT));
            } else if (ASTUtils.allIntType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.INT));
            } else if (ASTUtils.allLongType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.LONG));
            } else if (ASTUtils.allFloatType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.FLOAT));
            } else if (ASTUtils.allDoubleType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.DOUBLE));
            } else if (ASTUtils.allCharacterType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.CHAR));
            } else if (ASTUtils.allBooleanType(nodes)) {
                ret = ast.newArrayType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
            }
        } else {
            ret = ast.newSimpleType(ast.newName("Object"));
            if (ASTUtils.allStringType(nodes)) {
                ret = ast.newSimpleType(ast.newName("String"));
            } else if (ASTUtils.allByteType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.BYTE);
            } else if (ASTUtils.allShortType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.SHORT);
            } else if (ASTUtils.allIntType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.INT);
            } else if (ASTUtils.allLongType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.LONG);
            } else if (ASTUtils.allFloatType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.FLOAT);
            } else if (ASTUtils.allDoubleType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.DOUBLE);
            } else if (ASTUtils.allCharacterType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.CHAR);
            } else if (ASTUtils.allBooleanType(nodes)) {
                ret = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            }
        }
        return ret;
    }

}
