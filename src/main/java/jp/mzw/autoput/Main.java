package jp.mzw.autoput;

import jp.mzw.autoput.core.Project;

import jp.mzw.autoput.detect.Detector;
import jp.mzw.autoput.detect.NaiveDetector;
import jp.mzw.autoput.experiment.DetectorComprisonExperiment;
import jp.mzw.autoput.generate.Generator;
import jp.mzw.autoput.requirement.RequirementChecker;
import jp.mzw.autoput.revgenerate.RevGenerator;
import org.eclipse.jface.text.BadLocationException;

import java.io.IOException;

/**
 * Created by TK on 7/19/17.
 */
public class Main {
    public static final String CONFIG_FILENAME = "config.properties";

    public final static String PUT = "AutoPUT";
    public final static String CUT = "Original";

    public static void main(String[] args) throws IOException, BadLocationException {
        String projectId = args[0];
        String command   = args[1];
        Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
        if (command.equals("detect")) {
            long start = System.currentTimeMillis();
            Detector detector = new Detector(project);
            detector.detect();
            long end = System.currentTimeMillis();
            System.out.println("detection time on " + projectId + " : " + (end - start) + "ms");
        } else if (command.equals("generate")) {
            long start = System.currentTimeMillis();
            Generator generator = new Generator(project);
            generator.generate();
            long end = System.currentTimeMillis();
            System.out.println("generation time on " + projectId + " : " + (end - start) + "ms");
        } else if (command.equals("requirement")) {
            long start = System.currentTimeMillis();
            RequirementChecker requirementChecker = new RequirementChecker(project);
            requirementChecker.check();
            long end = System.currentTimeMillis();
            System.out.println("requirements checking time on " + projectId + " : " + (end - start) + "ms");
        }  else if (command.equals("all")) {
            long start = System.currentTimeMillis();
            Detector detector = new Detector(project);
            detector.detect();
            long detection_time = System.currentTimeMillis();
            Generator generator = new Generator(project);
            generator.generate();
            long generation_time = System.currentTimeMillis();
            RequirementChecker requirementChecker = new RequirementChecker(project);
            requirementChecker.check();
            long checking_time = System.currentTimeMillis();
            System.out.println("====================================");
            System.out.println(project.getProjectId());
            System.out.println("detection time: " + ((detection_time - start) / 1000) + "s");
            System.out.println("generation time: " + ((generation_time + checking_time - start) / 1000) + "s");
            System.out.println("====================================");
        } else if (command.equals("naive-detect")) {
            long start = System.currentTimeMillis();
            NaiveDetector naiveDetector = new NaiveDetector(project);
            naiveDetector.detect();
            long end = System.currentTimeMillis();
            System.out.println("detection time on " + projectId + " : " + (end - start) + "ms");
        } else if (command.equals("detect-comparison")) {
            DetectorComprisonExperiment.experiment();
        } else if (command.equals("revgenerate")) {
            RevGenerator revGenerator = new RevGenerator(project);
            revGenerator.revgenerate();
        } else {
            System.out.println("Wrong Command!");
        }
    }

}
