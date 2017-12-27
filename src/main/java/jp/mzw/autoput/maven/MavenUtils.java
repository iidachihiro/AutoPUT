package jp.mzw.autoput.maven;

import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenUtils {
    static Logger LOGGER = LoggerFactory.getLogger(MavenUtils.class);

    public static final String FILENAME_POM = "pom.xml";

    public static int testCompile(File projectHome, File mavenHome, String testName) {
        List<String> goal = Arrays.asList("test-compile");
        int ret = -1;
        try {
            ret = maven(projectHome, mavenHome, goal, testName);
        } catch (MavenInvocationException e) {
            LOGGER.error("MavenInvocationException is thrown at {}", testName);
        }
        return ret;
    }

    public static int measureCoverage(File projectHome, File mavenHome, String originName, String mode) {
        // coverage measurement contains test execution.
        List<String> goal = Arrays.asList("jacoco:prepare-agent", "test", "-Dtest=" + mode, "-DfailIfNoTests=false", "jacoco:report");
        int ret = -1;
        try {
            ret = maven(projectHome, mavenHome, goal, originName);
        } catch (MavenInvocationException e) {
            LOGGER.error("MavenInvocationException is thrown at {}", originName);
        }
        return ret;
    }

    private static int maven(File projectHome, File mavenHome, List<String> goal, String testName) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(projectHome, FILENAME_POM));
        request.setGoals(goal);
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setOutputHandler(new InvocationOutputHandler() {
            @Override
            public void consumeLine(String s) {
                if (s.contains("[INFO] BUILD FAILURE")) {
                    LOGGER.warn("BUILD FAILURE: {}", testName);
                } else if (s.contains("[INFO] BUILD SUCCESS")) {
                    LOGGER.debug("BUILD SUCCESS: {}", testName);
                }
            }
        });
        invoker.setErrorHandler(new InvocationOutputHandler() {
            @Override
            public void consumeLine(String s) {
                if (s.contains("[INFO] BUILD FAILURE")) {
                    LOGGER.warn("BUILD FAILURE: {}", testName);
                } else if (s.contains("[INFO] BUILD SUCCESS")) {
                    LOGGER.debug("BUILD SUCCESS: {}", testName);
                }
            }
        });
        InvocationResult result = invoker.execute(request);
        return result.getExitCode();
    }
}
