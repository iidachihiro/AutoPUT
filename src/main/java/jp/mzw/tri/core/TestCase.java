package jp.mzw.tri.core;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 7/19/17.
 */
public class TestCase {
    protected Logger LOGGER = LoggerFactory.getLogger(TestCase.class);
    MethodDeclaration method;
    CompilationUnit cu;
    String name;
    String className;

    TestSuite testSuite;
    public TestCase(String name, String classname, MethodDeclaration method, CompilationUnit cu, TestSuite testSuite) {
        this.name = name;
        this.className = classname;
        this.method = method;
        this.cu = cu;
        this.testSuite = testSuite;
    }

    public String getName() {
        return this.name;
    }
    public String getClassName() {
        return this.className;
    }
    public MethodDeclaration getMethodDeclaration() {
        return this.method;
    }
    public CompilationUnit getCompilationUnit() {
        return this.cu;
    }
    public TestSuite getTestSuite() {
        return this.testSuite;
    }
}
