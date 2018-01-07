package jp.mzw.autoput.core;

import jp.mzw.autoput.ast.ASTUtils;
import jp.mzw.autoput.ast.AllMethodFindVisitor;
import jp.mzw.autoput.ast.ClassDeclarationFindVisitor;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by TK on 7/19/17.
 */
public class TestSuite {
    protected static Logger LOGGER = LoggerFactory.getLogger(TestSuite.class);

    protected File testFile;
    protected String testClassName;
    protected List<TestCase> testCases;

    protected CompilationUnit cu;
    protected TypeDeclaration clazz;

    private MethodDeclaration autoPutTest;

    public TestSuite(File testFile, CompilationUnit cu) {
        this.testFile = testFile;
        String pathToTestFile = testFile.getAbsolutePath().substring(testFile.getParent().length() + 1);
        this.testClassName = pathToTestFile.replace(".java", "").replaceAll("/", ".");

        this.testCases = new ArrayList<>();
        this.cu = cu;
        this.autoPutTest = null;
        prepare();
        setClass();
    }

    public File getTestFile() {
        return this.testFile;
    }

    public String getTestClassName() {
        return this.testClassName;
    }
    public List<TestCase> getTestCases() {
        return this.testCases;
    }
    public CompilationUnit getCu() {
        return this.cu;
    }
    public TypeDeclaration getClassDeclaration() {
        return this.clazz;
    }
    public MethodDeclaration getAutoPutTest() {
        return this.autoPutTest;
    }

    private void prepare() {
        AllMethodFindVisitor visitor = new AllMethodFindVisitor();
        cu.accept(visitor);
        List<MethodDeclaration> methods = visitor.getFoundMethods();
        for (MethodDeclaration method : methods) {
            if (method.getName().getIdentifier().equals("autoPutTest")) {
                this.autoPutTest = method;
                continue;
            }
            if (!ASTUtils.isTestMethod(method)) {
                continue;
            }
            TestCase testcase = new TestCase(method.getName().getIdentifier(), testClassName, method, cu, this);
            testCases.add(testcase);
        }
    }

    private void setClass() {
        ClassDeclarationFindVisitor visitor = new ClassDeclarationFindVisitor();
        cu.accept(visitor);
        this.clazz = visitor.getClassDeclaration();
    }

    public String getTestSources() throws IOException {
        return Files.lines(Paths.get(testFile.getPath()))
                .collect(Collectors.joining(System.getProperty("line.separator")));
    }

    public boolean alreadyParameterized() {
        List<Annotation> annotations = ASTUtils.getAnnotations(clazz);
        for (Annotation annotation : annotations) {
            if (!(annotation instanceof SingleMemberAnnotation)) {
                continue;
            }
            if (annotation.getTypeName().toString().equals("Parameters")) {
                return true;
            }
            SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) annotation;
            Expression expression = singleMemberAnnotation.getValue();
            if (annotation.getTypeName().toString().equals("RunWith")
                    && (expression instanceof TypeLiteral)
                    && ((TypeLiteral) expression).getType().toString().equals("Parameterized")) {
                return true;
            }
        }
        return false;
    }
}
