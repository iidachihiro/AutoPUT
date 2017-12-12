package jp.mzw.autoput;

import jp.mzw.autoput.core.Project;

import jp.mzw.autoput.core.TestSuite;
import jp.mzw.autoput.experiment.ExperimentUtils;
import jp.mzw.autoput.modifier.ParameterizedModifier;
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
        String command   = args[1];
        Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
        ParameterizedModifier parameterizedModifier = new ParameterizedModifier(project);
        if (command.equals("detect")) {
            long start = System.currentTimeMillis();
            parameterizedModifier.detect();
            long detect_time = System.currentTimeMillis();
            System.out.println("Detect Time: " + (detect_time - start) + "ms");
        } else if (command.equals("convert")) {
            parameterizedModifier.modify();
        } else if (command.equals("experiment")) {
            parameterizedModifier.experiment();
        } else if (command.equals("evaluation")) {
            parameterizedModifier.evaluation();
        }  else if (command.equals("all")) {
            long start = System.currentTimeMillis();
            parameterizedModifier.detect();
            long detect_time = System.currentTimeMillis();
            parameterizedModifier.modify();
            long convert_time = System.currentTimeMillis();
            parameterizedModifier.experiment();
            parameterizedModifier.evaluation();
            long evaluation_time = System.currentTimeMillis();
            System.out.println("====================================");
            System.out.println(project.getProjectId());
            System.out.println("Detect Time: " + ((detect_time - start) / 1000) + "s");
            System.out.println("Convert Time: " + ((convert_time - start) / 1000) + "s");
            System.out.println("Finish Time: " + ((evaluation_time - start) / 1000) + "s");
            System.out.println("====================================");
        } else if (command.equals("compareNumOfStatements")) {
            parameterizedModifier.compareNumOfStatements();
        } else if (command.equals("testCompile")) {
            parameterizedModifier.testCompile();
        }  else {
            System.out.println("Wrong Command!");
        }
    }

}
