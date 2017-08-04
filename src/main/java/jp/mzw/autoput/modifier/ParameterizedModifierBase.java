package jp.mzw.autoput.modifier;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.core.TestSuite;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 7/28/17.
 */
public class ParameterizedModifierBase extends AbstractModifier {
    // ユーザが変更可能
    protected static final String CLASS_NAME = "AutoPUT";
    protected static final String METHOD_NAME = "autoPutTest";
    protected static final String INPUT_VAR = "input";
    protected static final String EXPECTED_VAR = "expected";

    // 変更不可
    protected static final String DATA_METHOD = "data";

    protected AST ast;

    protected String inputType = "String";
    protected String expectedType = "String";

    public ParameterizedModifierBase(TestSuite testSuite) {
        super(testSuite);
        ast = testSuite.getCu().getAST();
    }

    @Override
    public void modify(List<MethodDeclaration> targets) {
        CompilationUnit parameterizedCU = createParameterizedCompilationUnit();
        System.out.println(parameterizedCU);
    }

    protected CompilationUnit createParameterizedCompilationUnit() {
        // packageとimportをコピー
        CompilationUnit modified = (CompilationUnit) ASTNode.copySubtree(ast, testSuite.getCu());
        modified.imports().addAll(createImportDeclarations());
        // 新しいclassをset
        modified.types().clear();
        modified.types().add(createParameterizedClass());
        return modified;
    }

    protected List<ImportDeclaration> createImportDeclarations() {
        // import文を生成
        ImportDeclaration runWith = ast.newImportDeclaration();
        runWith.setName(ast.newName(new String[]{"org", "junit", "runner", "RunWith"}));
        ImportDeclaration parameterized = ast.newImportDeclaration();
        parameterized.setName(ast.newName(new String[] {"org", "junit", "runners", "Parameterized"}));
        ImportDeclaration parameters = ast.newImportDeclaration();
        parameters.setName(ast.newName(new String[] {"org", "junit", "runners", "Parameters"}));
        //return
        List<ImportDeclaration> ret = new ArrayList<>();
        ret.add(runWith);
        ret.add(parameterized);
        ret.add(parameters);
        return ret;
    }

    protected TypeDeclaration createParameterizedClass() {
        TypeDeclaration modified = ast.newTypeDeclaration();
        // RunWithアノテーションを付与
        SingleMemberAnnotation annotation = ASTUtils.getRunWithAnnotation(ast);
        modified.modifiers().add(annotation);
        // public修飾子を付与 & クラス名を付与
        modified.modifiers().add(ASTUtils.getPublicModifier(ast));
        modified.setName(ast.newSimpleName(CLASS_NAME));
        // フィールド変数を定義
        modified.bodyDeclarations().addAll(createFieldDeclarations());
        // コンストラクタとdataメソッドとテストメソッドを定義
        modified.bodyDeclarations().addAll(createMethodDeclaration());
        return modified;
    }

    protected List<FieldDeclaration> createFieldDeclarations() {
        List<FieldDeclaration> fields = new ArrayList<>();
        fields.add(createFieldDeclaration(inputType, INPUT_VAR));
        fields.add(createFieldDeclaration(expectedType, EXPECTED_VAR));
        return fields;
    }

    protected FieldDeclaration createFieldDeclaration(String type, String var) {
        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setName(ast.newSimpleName(var));
        FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
        fieldDeclaration.setType(ast.newSimpleType(ast.newName(type)));
        fieldDeclaration.modifiers().add(ASTUtils.getPrivateModifier(ast));
        return fieldDeclaration;
    }

    protected List<MethodDeclaration> createMethodDeclaration() {
        List<MethodDeclaration> methods = new ArrayList<>();
        MethodDeclaration constructor = createConstructor();
        MethodDeclaration data = createDataMethod();
        MethodDeclaration test = createTestMethod();
        methods.add(constructor);
        methods.add(data);
        methods.add(test);
        return methods;
    }

    protected MethodDeclaration createConstructor() {
        MethodDeclaration constructor = ast.newMethodDeclaration();
        constructor.setConstructor(true);
        // 名前を設定 & public修飾子を付与
        constructor.setName(ast.newSimpleName(CLASS_NAME));
        constructor.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        // 引数を設定
        SingleVariableDeclaration arg = ast.newSingleVariableDeclaration();
        arg.setType(ast.newSimpleType(ast.newName(this.inputType)));
        arg.setName(ast.newSimpleName(INPUT_VAR));
        constructor.parameters().add(arg);
        arg = ast.newSingleVariableDeclaration();
        arg.setType(ast.newSimpleType(ast.newName(this.expectedType)));
        arg.setName(ast.newSimpleName(EXPECTED_VAR));
        constructor.parameters().add(arg);

        // bodyを設定
        Block body = ast.newBlock();
        ExpressionStatement inputStatement = createConstructorBlockAssignment(INPUT_VAR);
        ExpressionStatement expectedStatement = createConstructorBlockAssignment(EXPECTED_VAR);
        body.statements().add(inputStatement);
        body.statements().add(expectedStatement);
        constructor.setBody(body);
        return constructor;
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

    protected MethodDeclaration createDataMethod() {
        MethodDeclaration method = ast.newMethodDeclaration();
        method.setConstructor(false);
        // 修飾子とメソッド名を設定
        method.setName(ast.newSimpleName(DATA_METHOD));
        method.modifiers().add(ASTUtils.getParametersAnnotation(ast));
        method.modifiers().add(ASTUtils.getPublicModifier(ast));
        method.modifiers().add(ASTUtils.getStaticModifier(ast));
        // returnの型(Collection<Object[]>)を設定
        ParameterizedType type = ast.newParameterizedType(ast.newSimpleType(ast.newName("Collection")));
        ArrayType objectArray = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        type.typeArguments().add(objectArray);
        method.setReturnType2(type);
        // bodyを設定
        method.setBody(createDataBody());
        return method;
    }

    protected Block createDataBody() {
        // return Arrays.asList(new Object[][] {{ input1, expected1 }, { input2, expected2 }}); を作成する
        // まず, new Object[][] {{ input1, expected1 }, { input2, expected2 }} を作成
        ArrayType arrayType = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        arrayType.dimensions().add(ast.newDimension());
        ArrayCreation arrayCreation = ast.newArrayCreation();
        arrayCreation.setType(arrayType);
        // {{ input1, expected1 }, { input2, expected2 }} を設定
        arrayCreation.setInitializer(createInputAndExpected());
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

    protected ArrayInitializer createInputAndExpected() {
        // {{ input1, expected1 }, { input2, expected2 }} を作る
        ArrayInitializer arrayInitializer = ast.newArrayInitializer();
        // TODO ArrayInitializerを作成する
        return arrayInitializer;
    }

    protected MethodDeclaration createTestMethod() {
        MethodDeclaration testMethod = ast.newMethodDeclaration();
        testMethod.modifiers().add(ASTUtils.getTestAnnotation(ast));
        testMethod.modifiers().add(ASTUtils.getPublicModifier(ast));
        testMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
        testMethod.setName(ast.newSimpleName(METHOD_NAME));
        Block body = createTestMethodBody();
        // TODO Bodyを作成する
        testMethod.setBody(body);
        return testMethod;
    }

    protected Block createTestMethodBody() {
        return ast.newBlock();
    }
}
