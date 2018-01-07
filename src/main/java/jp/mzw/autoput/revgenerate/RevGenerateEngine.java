package jp.mzw.autoput.revgenerate;

import jp.mzw.autoput.ast.ASTUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RevGenerateEngine {
    private static Logger LOGGER = LoggerFactory.getLogger(RevGenerateEngine.class);

    private static final String METHOD_NAME = "testMethod";

    protected String source;
    protected AST ast;
    protected CompilationUnit cu;
    protected ASTRewrite rewrite;

    public RevGenerateEngine(String source) {
        this.source = source;
    }

    protected String revgenerate() {
        cu = parse(source);
        ast = cu.getAST();
        rewrite = ASTRewrite.create(ast);

        // import information
        modifyImportDeclarations();
        TypeDeclaration typeDeclaration = (TypeDeclaration) cu.types().get(0);
        deleteRunWith(typeDeclaration);
        ListRewrite bodyRewrite = rewrite.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        // Theoryを特定
        MethodDeclaration theory = RevGenerateHelper.getTheory(cu);
        bodyRewrite.remove(theory, null);
        // 引数を削除
        removeArgs(theory);
        // Theoryに各INPUTとEXPECTEDを差し込む
        List<Pair<Expression, Expression>> parameters = RevGenerateHelper.getDataPoints(cu);
        for (Pair<Expression, Expression> parameter : parameters) {
            MethodDeclaration mold = (MethodDeclaration) ASTNode.copySubtree(ast, theory);
            modifyAnnotation(mold);
            revgenerate(mold, parameter);
            bodyRewrite.insertLast(mold, null);
            bodyRewrite.remove(parameter.getLeft().getParent().getParent(), null);
            bodyRewrite.remove(parameter.getRight().getParent().getParent(), null);
        }
        // DataPointsを削除
        deleteDataPoints(typeDeclaration, bodyRewrite);
        // Fixtureクラスを削除
        deleteFixtureClass(typeDeclaration, bodyRewrite);
        // apply & return
        return applyAstEdit(source, rewrite);
    }

    private CompilationUnit parse(String origin) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(origin.toCharArray());
        return (CompilationUnit) parser.createAST(new NullProgressMonitor());
    }

    private String applyAstEdit(String source, ASTRewrite rewrite) {
        try {
            Document document = new Document(source);
            TextEdit edit = rewrite.rewriteAST(document, null);
            edit.apply(document);
            return document.get();
        } catch (BadLocationException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void removeArgs(MethodDeclaration theory) {
        // 引数を削除
        ListRewrite argsRewrite = rewrite.getListRewrite(theory, MethodDeclaration.PARAMETERS_PROPERTY);
        for (SingleVariableDeclaration singleVariableDeclaration : (List<SingleVariableDeclaration>) theory.parameters()) {
            argsRewrite.remove(singleVariableDeclaration, null);
        }
    }

    private void modifyAnnotation(MethodDeclaration method) {
        // @Theoryを削除して@Testを付与する
        ListRewrite annotationRewrite = rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
        for (IExtendedModifier modifier : (List<IExtendedModifier>) method.modifiers()) {
            if (modifier.isModifier()) {
                continue;
            }
            Annotation annotation = (Annotation) modifier;
            // @Theoryを削除
            if (annotation.getTypeName().toString().equals("Theory")) {
                annotationRewrite.remove(annotation, null);
            }
        }
        // @Testを追加
        annotationRewrite.insertFirst(ASTUtils.getTestAnnotation(ast), null);
    }

    private void revgenerate(MethodDeclaration origin, Pair<Expression, Expression> parameter) {
        Expression inputExpression = parameter.getLeft();
        Expression expectedExpression = parameter.getRight();
        List<QualifiedName> qualifiedNames = ASTUtils.getAllQualifiedNames(origin);
        List<QualifiedName> inputs = new ArrayList<>();
        List<QualifiedName> expecteds = new ArrayList<>();
        for (QualifiedName qualifiedName : qualifiedNames) {
            if (qualifiedName.getQualifier().toString().equals("fixture")) {
                if (qualifiedName.getName().getIdentifier().startsWith("_input")) {
                    // inputを入れ替え
                    inputs.add(qualifiedName);
                } else if (qualifiedName.getName().getIdentifier().startsWith("_expected")) {
                    expecteds.add(qualifiedName);
                }
            }
        }
        // fixtureとparameterを交換
        replaceFixture(inputs, inputExpression);
        replaceFixture(expecteds, expectedExpression);
    }

    private void replaceFixture(List<QualifiedName> fixture, Expression parameter) {
        if (fixture.isEmpty()) {
            return;
        }
        if (parameter instanceof ArrayInitializer) {
            ArrayInitializer arrayInitializer = (ArrayInitializer) parameter;
            for (int i = 0; i < fixture.size(); i++) {
                QualifiedName qualifiedName = fixture.get(i);
                ASTNode target = qualifiedName.getParent();
                Expression expression = (Expression) arrayInitializer.expressions().get(i);
                Expression replace = (Expression) ASTNode.copySubtree(ast, expression);
                rewrite.replace(target, replace, null);
            }
        } else {
            QualifiedName qualifiedName = fixture.get(0);
            Expression replace = (Expression) ASTNode.copySubtree(ast, parameter);
            rewrite.replace(qualifiedName, replace, null);
        }
    }

    protected void modifyImportDeclarations() {
        ListRewrite listRewrite = rewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
        // import文を生成
        for (ImportDeclaration importDeclaration : (List<ImportDeclaration>) cu.imports()) {
            if (importDeclaration.getName().toString().equals("org.junit.runner.RunWith")) {
                listRewrite.remove(importDeclaration, null);
            }
            if (importDeclaration.getName().toString().equals("org.junit.experimental.theories.Theories")) {
                listRewrite.remove(importDeclaration, null);
            }
            if (importDeclaration.getName().toString().equals("org.junit.experimental.theories.Theory")) {
                listRewrite.remove(importDeclaration, null);
            }
            if (importDeclaration.getName().toString().equals("org.junit.experimental.theories.DataPoints")) {
                listRewrite.remove(importDeclaration, null);
            }
        }
        ImportDeclaration test = ast.newImportDeclaration();
        test.setName(ast.newName(new String[]{"org", "junit", "Test"}));
        // importを付与
        listRewrite.insertLast(test, null);
    }

    private void deleteRunWith(TypeDeclaration typeDeclaration) {
        ListRewrite listRewrite = rewrite.getListRewrite(typeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
        for (IExtendedModifier iExtendedModifier : (List<IExtendedModifier>) typeDeclaration.modifiers()) {
            if (iExtendedModifier.isModifier()) {
                continue;
            }
            Annotation annotation = (Annotation) iExtendedModifier;
            if (annotation.getTypeName().toString().equals("RunWith")) {
                listRewrite.remove(annotation, null);
            }
        }
    }

    private void deleteDataPoints(TypeDeclaration typeDeclaration, ListRewrite listRewrite) {
        List<FieldDeclaration> fieldDeclarations = ASTUtils.getAllFieldDeclaratoins(typeDeclaration);
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
            for (IExtendedModifier modifier : (List<IExtendedModifier>) fieldDeclaration.modifiers()) {
                if (!modifier.isAnnotation()) {
                    continue;
                }
                Annotation annotation = (Annotation) modifier;
                if (annotation.getTypeName().toString().equals("DataPoints")) {
                    listRewrite.remove(fieldDeclaration, null);
                }
            }
        }
    }

    private void deleteFixtureClass(TypeDeclaration origin, ListRewrite listRewrite) {
        List<TypeDeclaration> typeDeclarations = ASTUtils.getAllTypeDeclarations(origin);
        for (TypeDeclaration typeDeclaration : typeDeclarations) {
            if (typeDeclaration.getName().getIdentifier().equals("Fixture")) {
                listRewrite.remove(typeDeclaration, null);
            }
        }
    }

}
