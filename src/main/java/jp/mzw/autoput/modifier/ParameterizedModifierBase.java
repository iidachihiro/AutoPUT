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

    protected String inputType;
    protected String expectedType;

    public ParameterizedModifierBase(TestSuite testSuite) {
        super(testSuite);
        ast = AST.newAST(AST.JLS8);
    }

    @Override
    public Map<MethodDeclaration, List<MethodDeclaration>> detect() {
        // TODO implementation
        return new HashMap<>();
    }

    @Override
    public void modify(List<MethodDeclaration> targets) {
        CompilationUnit parameterizedCU = createParameterizedCompilationUnit();
    }

    protected CompilationUnit createParameterizedCompilationUnit() {
        // packageとimportをコピー
        CompilationUnit modified = (CompilationUnit) ASTNode.copySubtree(ast, testSuite.getCu());
        // TODO importを追加
        // 新しいclassをset
        modified.types().clear();
        modified.types().add(createParameterizedClass());
        return modified;
    }

    protected TypeDeclaration createParameterizedClass() {
        TypeDeclaration modified = ast.newTypeDeclaration();
        // RunWithアノテーションを付与
        SingleMemberAnnotation annotation = ASTUtils.getRunWithAnnotation();
        modified.modifiers().add(annotation);
        // public修飾子を付与 & クラス名を付与
        modified.modifiers().add(ASTUtils.getPublicModifier());
        modified.setName(ast.newSimpleName(CLASS_NAME));
        // フィールド変数を定義
        modified.bodyDeclarations().addAll(createFieldDeclarations());
        // コンストラクタとdataメソッドとテストメソッドを定義
        modified.bodyDeclarations().addAll(createMethodDeclaration());
        return modified;
    }

    private List<FieldDeclaration> createFieldDeclarations() {
        List<FieldDeclaration> fields = new ArrayList<>();
        fields.add(createFieldDeclaration(inputType, INPUT_VAR));
        fields.add(createFieldDeclaration(expectedType, EXPECTED_VAR));
        return fields;
    }

    private FieldDeclaration createFieldDeclaration(String type, String var) {
        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setName(ast.newSimpleName(var));
        FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
        fieldDeclaration.setType(ast.newSimpleType(ast.newName(type)));
        fieldDeclaration.modifiers().add(ASTUtils.getPrivateModifier());
        return fieldDeclaration;
    }

    private List<MethodDeclaration> createMethodDeclaration() {
        List<MethodDeclaration> methods = new ArrayList<>();
        MethodDeclaration constructor = createConstructor();
        MethodDeclaration data = createDataMethod();
        MethodDeclaration test = createTestMethod(ast);
        methods.add(constructor);
        methods.add(data);
        methods.add(test);
        return methods;
    }

    private MethodDeclaration createConstructor() {
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

    private ExpressionStatement createConstructorBlockAssignment(String var) {
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

    private MethodDeclaration createDataMethod() {
        MethodDeclaration method = ast.newMethodDeclaration();
        method.setConstructor(false);
        // 修飾子とメソッド名を設定
        method.setName(ast.newSimpleName(DATA_METHOD));
        method.modifiers().add(ASTUtils.getParametersAnnotation());
        method.modifiers().add(ASTUtils.getPublicModifier());
        method.modifiers().add(ASTUtils.getStaticModifier());
        // returnの型(Collection<Object[]>)を設定
        ParameterizedType type = ast.newParameterizedType(ast.newSimpleType(ast.newName("Collection")));
        ArrayType objectArray = ast.newArrayType(ast.newSimpleType(ast.newName("Object")));
        type.typeArguments().add(objectArray);
        method.setReturnType2(type);
        // bodyを設定
        method.setBody(createDataBody());
        return method;
    }

    private Block createDataBody() {
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

    private ArrayInitializer createInputAndExpected() {
        // {{ input1, expected1 }, { input2, expected2 }} を作る
        ArrayInitializer arrayInitializer = ast.newArrayInitializer();
        // TODO ArrayInitializerを作成する
        return arrayInitializer;
    }

    private MethodDeclaration createTestMethod(AST ast) {
        MethodDeclaration testMethod = ast.newMethodDeclaration();
        testMethod.modifiers().add(ASTUtils.getTestAnnotation());
        testMethod.modifiers().add(ASTUtils.getPublicModifier());
        testMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
        testMethod.setName(ast.newSimpleName(METHOD_NAME));
        Block body = ast.newBlock();
        // TODO Bodyを作成する
        testMethod.setBody(body);
        return testMethod;
    }
}
