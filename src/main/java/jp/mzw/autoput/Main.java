package jp.mzw.autoput;

import jp.mzw.autoput.core.Project;

import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.modifier.ParamterizedModifier;
import jp.mzw.autoput.modifier.UseParametersAnnotation;
import org.eclipse.jface.text.BadLocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by TK on 7/19/17.
 */
public class Main {
    static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final String CONFIG_FILENAME = "config.properties";

    public static void main(String[] args) throws IOException, BadLocationException {
        String projectId = args[0];
        Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
        List<TestSuite> testSuites = project.getTestSuites();
        for (TestSuite testSuite : testSuites) {
            if (!testSuite.getTestFile().toString().endsWith("ComplexFormatAbstractTest.java")) {
                continue;
            }
//            UseConstVariable useConstVariable = new UseConstVariable(testSuite);
//            useConstVariable.checkConstVariavbles();
//            UseParametersAnnotation useParametersAnnotation = new UseParametersAnnotation(testSuite);
//            useParametersAnnotation.useParametersAnnotation();
            ParamterizedModifier paramterizedModifier = new ParamterizedModifier(testSuite);
            paramterizedModifier.paramterized();
        }
    }

}
