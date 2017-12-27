package jp.mzw.autoput.generate;

import jp.mzw.autoput.Main;
import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.TestCase;
import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.detect.Detector;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GenerateEngine {
    private static Logger LOGGER = LoggerFactory.getLogger(GenerateEngine.class);
    // ユーザが変更可能
    protected static final String METHOD_NAME = "autoPutTest";
    protected static final String INPUT_VAR = "_input";
    protected static final String EXPECTED_VAR = "_expected";
    protected static final String FIXTURE_CLASS = "Fixture";
    protected static final String FIXTURE_NAME = "fixture";

    protected TestSuite testSuite;

    public GenerateEngine(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    protected AST ast;
    protected CompilationUnit cu;
    protected ASTRewrite rewrite;

    public CompilationUnit getCompilationUnit() {
        return testSuite.getCu();
    }


    /* ----------------------------------------------------------------------- */

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
            // @Overrideを削除
            if (annotation.getTypeName().toString().equals("Override")) {
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
            if (Detector.similarAST(origin, method)) {
                if (mostDifferentNodes == null) {
                    mostDifferentNodes = GenerateHelper.getParameterNodes(method, origin);
                } else if (mostDifferentNodes.size() < ASTUtils.getDifferentNodes(origin, method).size()) {
                    mostDifferentNodes = GenerateHelper.getParameterNodes(method, origin);
                }
            }
        }
        // commonじゃないnodeをinputとexpectedに変更する
        // commonsじゃないnodeをinputとexpectedに分ける
        List<ASTNode> inputRelatedNodes = new ArrayList<>();
        List<ASTNode> expectedRelatedNodes = new ArrayList<>();
        for (ASTNode node : mostDifferentNodes) {
            if (GenerateHelper.isInExpectedDeclaringNode(origin, node)) {
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
            if (target instanceof SimpleName) {
                target = GenerateHelper.getRootName((Name) target);
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
                boolean isObjectArrayType = GenerateHelper.isObjectArrayType(_getExpectedType(origin));
                if (isObjectArrayType && GenerateHelper.isBooleanType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Boolean");
                } else if (isObjectArrayType && GenerateHelper.isCharacterType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Character");
                } else if (isObjectArrayType && GenerateHelper.isByteType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Byte");
                } else if (isObjectArrayType && GenerateHelper.isShortType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Short");
                } else if (isObjectArrayType && GenerateHelper.isIntType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Integer");
                } else if (isObjectArrayType && GenerateHelper.isLongType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Long");
                } else if (isObjectArrayType && GenerateHelper.isFloatType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Float");
                } else if (isObjectArrayType && GenerateHelper.isDoubleType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Double");
                } else if (isObjectArrayType && GenerateHelper.canBeCasted(target)) {
                    replace = _castObject(arrayAccess, target);
                } else {
                    replace = arrayAccess;
                }
                if (ASTUtils.isNumberLiteralWithPrefixedMinus(target)) {
                    target = target.getParent();
                }
                if (target instanceof SimpleName) {
                    target = GenerateHelper.getRootName((Name) target);
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
            if (target instanceof SimpleName) {
                target = GenerateHelper.getRootName((Name) target);
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
                boolean isObjectArrayType = GenerateHelper.isObjectArrayType(_getInputType(origin));
                if (isObjectArrayType && GenerateHelper.isBooleanType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Boolean");
                } else if (isObjectArrayType && GenerateHelper.isCharacterType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Character");
                } else if (isObjectArrayType && GenerateHelper.isByteType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Byte");
                } else if (isObjectArrayType && GenerateHelper.isShortType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Short");
                } else if (isObjectArrayType && GenerateHelper.isIntType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Integer");
                } else if (isObjectArrayType && GenerateHelper.isLongType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Long");
                } else if (isObjectArrayType && GenerateHelper.isFloatType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Float");
                } else if (isObjectArrayType && GenerateHelper.isDoubleType(target)) {
                    replace = _convertObjectToPrimitiveWrapper(arrayAccess, "Double");
                } else if (isObjectArrayType && GenerateHelper.canBeCasted(target)) {
                    replace = _castObject(arrayAccess, target);
                } else if (GenerateHelper.isDoubleArrayType(_getInputType(origin)) && GenerateHelper.shouldCastDoubleToInt(target)) {
                    replace = _castInt(arrayAccess);
                } else if (GenerateHelper.isDoubleArrayType(_getInputType(origin)) && GenerateHelper.shouldCastDoubleToLong(target)) {
                    replace = _castLong(arrayAccess);
                } else {
                    replace = arrayAccess;
                }
                if (ASTUtils.isNumberLiteralWithPrefixedMinus(target)) {
                    target = target.getParent();
                }
                if (target instanceof SimpleName) {
                    target = GenerateHelper.getRootName((Name) target);
                }
                rewrite.replace(target, replace, null);
            }
        }
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
            if (Detector.similarAST(origin, method)) {
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
            if (GenerateHelper.isInExpectedDeclaringNode(origin, node)) {
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
            inputStatement.setType(_getInputType(origin));
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
            expectedStatement.setType(_getExpectedType(origin));
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
            if (ASTUtils.hasPublicModifier(methodDeclaration.modifiers()) && methodDeclaration.getName().getIdentifier().startsWith("test")) {
                listRewrite.remove(methodDeclaration, null);
            }
        }
    }

    protected void modifyConstructor(MethodDeclaration origin, String clazzName) {
        TypeDeclaration modified = getTargetType();
        for (MethodDeclaration methodDeclaration : modified.getMethods()) {
            if (methodDeclaration.isConstructor()) {
                Name target = methodDeclaration.getName();
                Name replace = ast.newName(clazzName);
                rewrite.replace(target, replace, null);
            }
        }
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
        FieldDeclaration inputDeclaration = createFieldDeclaration(_getInputType(origin), INPUT_VAR);
        FieldDeclaration expectedDeclaration = createFieldDeclaration(_getExpectedType(origin), EXPECTED_VAR);
        // フィールド変数定義を追加
        ListRewrite bodyDeclarationsListRewrite = rewrite.getListRewrite(target, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        bodyDeclarationsListRewrite.insertLast(inputDeclaration, null);
        bodyDeclarationsListRewrite.insertLast(expectedDeclaration, null);
    }
    protected FieldDeclaration createFieldDeclaration(Type type, String var) {
        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setName(ast.newSimpleName(var));
        FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
        fieldDeclaration.setType(type);
        fieldDeclaration.modifiers().add(ASTUtils.getPrivateModifier(ast));
        return fieldDeclaration;
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
        input.setType(_getInputType(origin));
        input.setName(ast.newSimpleName(INPUT_VAR));
        // expectedの型と名前を設定
        SingleVariableDeclaration expected = ast.newSingleVariableDeclaration();
        expected.setType(_getExpectedType(origin));
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
    protected void modifyClassInfo(String clazzName) {
        TypeDeclaration modified = getTargetType();
        // 修飾子を変更
        ListRewrite modifiersListRewrite = rewrite.getListRewrite(modified, TypeDeclaration.MODIFIERS2_PROPERTY);
        // RunWithアノテーションを付与
        SingleMemberAnnotation annotation = ASTUtils.getRunWithAnnotation(ast);
        modifiersListRewrite.insertFirst(annotation, null);
    }
    protected void modifyClassName(String clazzName) {
        TypeDeclaration modified = getTargetType();
        String typeName = modified.getName().getIdentifier();
        List<Name> names = ASTUtils.getAllNames(modified);
        for (Name name : names) {
            if (name.toString().equals(typeName)) {
                Name replace = ast.newName(clazzName);
                rewrite.replace(name, replace, null);
            }
        }
    }

    /* --------------------- helper -------------------------------------- */
    private Type _getExpectedType(MethodDeclaration origin) {
        List<ASTNode> mostDifferentNodes = null;
        // 共通部分が一番少ないメソッドを探す
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (method.equals(origin)) {
                continue;
            }
            if (Detector.similarAST(origin, method)) {
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
            if (GenerateHelper.isInExpectedDeclaringNode(origin, node)) {
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

    private Type _getInputType(MethodDeclaration origin) {
        List<ASTNode> mostDifferentNodesInOrigin = null;
        List<List<ASTNode>> similarMethodNodes = new ArrayList<>();
        List<ASTNode> mostDifferentNodesFromAnother = null;
        // 共通部分が一番少ないメソッドを探す
        for (TestCase testCase : testSuite.getTestCases()) {
            MethodDeclaration method = testCase.getMethodDeclaration();
            if (method.equals(origin)) {
                continue;
            }
            if (Detector.similarAST(origin, method)) {
                if (mostDifferentNodesInOrigin == null) {
                    mostDifferentNodesInOrigin = ASTUtils.getDifferentNodes(method, origin);
                    mostDifferentNodesFromAnother = ASTUtils.getDifferentNodes(origin, method);
                } else if (mostDifferentNodesInOrigin.size() <= ASTUtils.getDifferentNodes(origin, method).size()) {
                    mostDifferentNodesInOrigin = ASTUtils.getDifferentNodes(method, origin);
                    mostDifferentNodesFromAnother = ASTUtils.getDifferentNodes(origin, method);
                }
            }
        }
        // commonじゃないnodeからinputを抽出する
        List<ASTNode> inputRelatedNodesInOrigin = new ArrayList<>();
        for (ASTNode node : mostDifferentNodesInOrigin) {
            if (!GenerateHelper.isInExpectedDeclaringNode(origin, node)) {
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
                        if (!GenerateHelper.isInExpectedDeclaringNode(origin, node)) {
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
        } else if (commonTypeInOrigin instanceof PrimitiveType) {
            PrimitiveType primitiveType = (PrimitiveType) commonTypeInOrigin;
            if (primitiveType.getPrimitiveTypeCode().equals(PrimitiveType.INT)) {
                List<ASTNode> inputRelatedNodesFromAnother = new ArrayList<>();
                for (ASTNode node : mostDifferentNodesFromAnother) {
                    if (!GenerateHelper.isInExpectedDeclaringNode(origin, node)) {
                        inputRelatedNodesFromAnother.add(node);
                    }
                }
                Type commonTypeFromAother = _getCommonType(inputRelatedNodesFromAnother);
                if (commonTypeFromAother instanceof PrimitiveType) {
                    PrimitiveType primitiveType1 = (PrimitiveType) commonTypeFromAother;
                    if (primitiveType1.getPrimitiveTypeCode() == PrimitiveType.DOUBLE) {
                        ret = commonTypeFromAother;
                    }
                }
            }
        }
        return ret;
    }

    private MethodInvocation _convertObjectToPrimitiveWrapper(Expression expression, String type) {
        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(expression);
        methodInvocation.setName(ast.newSimpleName("toString"));
        ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
        ListRewrite argsRewrite = rewrite.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
        argsRewrite.insertLast(methodInvocation, null);
        classInstanceCreation.setType(ast.newSimpleType(ast.newName(type)));
        MethodInvocation methodInvocation1 = ast.newMethodInvocation();
        methodInvocation1.setExpression(classInstanceCreation);
        String valueType = "";
        if (type.equals("Integer")) {
            valueType = "int";
        } else if (type.equals("Double")) {
            valueType = "double";
        } else if (type.equals("Long")) {
            valueType = "long";
        } else if (type.equals("Character")) {
            valueType = "char";
        } else if (type.equals("Boolean")) {
            valueType = "boolean";
        } else {
            valueType = type.toLowerCase();
        }
        methodInvocation1.setName(ast.newSimpleName(valueType + "Value"));
        return methodInvocation1;
    }

    private CastExpression _castObject(Expression expression, ASTNode target) {
        CastExpression castExpression = ast.newCastExpression();
        castExpression.setExpression(expression);
        ITypeBinding iTypeBinding = ((Expression) target).resolveTypeBinding();
        Type castType = ast.newSimpleType(ast.newName(iTypeBinding.getName()));
        castExpression.setType(castType);
        return castExpression;
    }

    private CastExpression _castInt(Expression expression) {
        CastExpression castExpression = ast.newCastExpression();
        castExpression.setExpression(expression);
        Type castType = ast.newPrimitiveType(PrimitiveType.INT);
        castExpression.setType(castType);
        return castExpression;
    }

    private CastExpression _castLong(Expression expression) {
        CastExpression castExpression = ast.newCastExpression();
        castExpression.setExpression(expression);
        Type castType = ast.newPrimitiveType(PrimitiveType.LONG);
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


    /* ------------------------- For Experiment ---------------------- */
    protected String generatePUT(MethodDeclaration method) {
        String clazzName = Main.PUT;
        cu = getCompilationUnit();
        ast = cu.getAST();
        rewrite = ASTRewrite.create(ast);
        // テストメソッドを作成(既存のテストメソッドを修正する)
        modifyTestMethod(method);
        // data-pointsを作成
        createDataPoints(method);
        // import文を追加
        addImportDeclarations();
        // Fixtureクラスを追加
        addFixtureClass(method);
        // クラスに修飾子も追加
        modifyClassInfo(clazzName);
        // 既存のテストメソッドを全て削除(コンストラクタと@Testのないものは残る)
        deleteOtherTestMethods(method);
        // コンストラクタ名を修正
        modifyConstructor(method, clazzName);
        // クラス内に出てくるクラス名をすべて変更
        modifyClassName(clazzName);
        // apply
        String content = applyAstEdit(testSuite, rewrite);
        if (content.equals("")) {
            LOGGER.error("Fail to parameterize: {} at {}", method.getName().getIdentifier(), testSuite.getTestClassName());
        }
        // initialize
        this.cu = null;
        this.ast = null;
        this.rewrite = null;
        return content;
    }

    public String extractOriginalCUTs(MethodDeclaration method, List<String> similarCUTs) {
        String clazzName = Main.CUT;
        cu = getCompilationUnit();
        ast = cu.getAST();
        rewrite = ASTRewrite.create(ast);
        excludeNotSimilarMethods(method, similarCUTs);
        // コンストラクタ名を修正
        modifyConstructor(method, clazzName);
        // クラス内に出てくるクラス名をすべて変更
        modifyClassName(clazzName);
        // apply
        String content = applyAstEdit(testSuite, rewrite);
        if (content.equals("")) {
            LOGGER.error("Fail to parameterize: {} at {}", method.getName().getIdentifier(), testSuite.getTestClassName());
        }
        // initialize
        this.cu = null;
        this.ast = null;
        this.rewrite = null;
        return content;
    }

    private String applyAstEdit(TestSuite testSuite, ASTRewrite rewrite) {
        try {
            Document document = new Document(testSuite.getTestSources());
            TextEdit edit = rewrite.rewriteAST(document, null);
            edit.apply(document);
            return document.get();
        } catch (IOException | BadLocationException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void excludeNotSimilarMethods(MethodDeclaration origin, List<String> similarMethods) {
        TypeDeclaration modified = getTargetType();
        ListRewrite listRewrite = rewrite.getListRewrite(modified, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        for (MethodDeclaration methodDeclaration : modified.getMethods()) {
            if (ASTUtils.hasPrivateModifier(methodDeclaration.modifiers())) {
                continue;
            }
            if (methodDeclaration.getName().getIdentifier().equals(origin.getName().getIdentifier())) {
                continue;
            }
            if (similarMethods.contains(methodDeclaration.getName().getIdentifier())) {
                continue;
            }
            if (ASTUtils.hasPublicModifier(methodDeclaration.modifiers())
                    && ASTUtils.isVoidReturn(methodDeclaration)
                    && methodDeclaration.getName().getIdentifier().startsWith("test")) {
                listRewrite.remove(methodDeclaration, null);
            }
        }
    }
}
