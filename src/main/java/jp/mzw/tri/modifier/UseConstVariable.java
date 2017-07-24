package jp.mzw.tri.modifier;

import jp.mzw.tri.ast.AllLiteralFindVisitor;
import jp.mzw.tri.core.TestCase;
import jp.mzw.tri.core.TestSuite;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by TK on 7/20/17.
 */
public class UseConstVariable {
    protected static Logger LOGGER = LoggerFactory.getLogger(UseConstVariable.class);

    protected TestSuite testSuite;

    public UseConstVariable(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public void checkConstVariavbles() throws IOException, BadLocationException {
        HashMap<String, List<StringLiteral>> commonStringLiterals = new HashMap<>();

        List<TestCase> testCases = testSuite.getTestCases();
        for (TestCase testCase : testCases) {
            AllLiteralFindVisitor visitor = new AllLiteralFindVisitor();
            testCase.getMethodDeclaration().accept(visitor);
            List<StringLiteral> stringLiterals = visitor.getFoundStringLiterals();
            for (StringLiteral stringLiteral : stringLiterals) {
                String key = stringLiteral.getLiteralValue();
                if (commonStringLiterals.containsKey(key)) {
                    List<StringLiteral> strings = commonStringLiterals.get(key);
                    strings.add(stringLiteral);
                } else {
                    List<StringLiteral> strings = new ArrayList<>();
                    strings.add(stringLiteral);
                    commonStringLiterals.put(key, strings);
                }
            }
        }

        final CompilationUnit cu = testSuite.getCu();
        final AST ast = cu.getAST();
        final ASTRewrite rewrite = ASTRewrite.create(ast);

        // 共通して使われているStringをprivate static finalとして定義
        HashMap<String, Name> varMapping = new HashMap<>();
        TypeDeclaration clazz = testSuite.getClassDeclaration();
        for (String key : commonStringLiterals.keySet()) {
            VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
            fragment.setName(ast.newSimpleName(key.toUpperCase()));
            StringLiteral value = ast.newStringLiteral();
            value.setLiteralValue(key);
            fragment.setInitializer(value);
            FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
            fieldDeclaration.setType(ast.newSimpleType(ast.newName("String")));
            fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
            fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
            fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
            varMapping.put(key, fragment.getName());
        }

        // 点在しているStringを上で定義した変数で置き換える．
        for (String key : commonStringLiterals.keySet()) {
            List<StringLiteral> stringLiterals = commonStringLiterals.get(key);
            for (StringLiteral stringLiteral : stringLiterals) {
                MethodInvocation target = (MethodInvocation) stringLiteral.getParent();
                MethodInvocation replace = (MethodInvocation) ASTNode.copySubtree(ast, target);
                replace.arguments().clear();
                List<Object> arguments = target.arguments();
                for (Object obj : arguments) {
                    Expression argument = (Expression) obj;
                    if (!argument.equals(stringLiteral)) {
                        replace.arguments().add((Expression) ASTNode.copySubtree(ast, argument));
                        continue;
                    }
                    Name name = varMapping.get(key);
                    replace.arguments().add((Expression) ASTNode.copySubtree(ast, name));
                    rewrite.replace(target, replace, null);
                }
            }
        }

        String origin = testSuite.getTestSources();
        Document document = new Document(origin);
        TextEdit edit = rewrite.rewriteAST(document, null);
        edit.apply(document);
        System.out.println(document.get());
    }
}
